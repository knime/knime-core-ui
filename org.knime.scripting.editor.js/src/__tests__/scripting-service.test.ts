import { describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";

import type { JsonDataService } from "@knime/ui-extension-service";

import { ScriptingService } from "@/scripting-service";

type MockBackend = {
  [key: string]: (options: any[]) => Promise<any> | undefined;
};
const dataFnMock = (backend: MockBackend) => {
  // By default we add a method that stops the event loop
  backend = {
    getEvent: () => Promise.resolve({ type: "STOP" }),
    ...backend,
  };

  return vi.fn(({ method, options }: { method: string; options: any[] }) => {
    // Remove ScriptingService. prefix
    if (method.startsWith("ScriptingService.")) {
      method = method.replace("ScriptingService.", "");
    } else {
      return Promise.reject(new Error(`Unexpected method call ${method}`));
    }

    // Get the method
    const backendMethod = backend[method];

    // Call the method if it exists
    if (backendMethod) {
      return backendMethod(options);
    } else {
      return Promise.reject(
        new Error(`Method ${method} not found in backend mock`),
      );
    }
  });
};

const lock = <T = void>() => {
  let resolve: (resolvedValue: T) => void = () => {};
  const promise = new Promise<T>((r) => {
    resolve = r;
  });
  return { promise, resolve };
};

describe("scripting-service", () => {
  describe("class EventHandler", () => {
    it("should call event handler on received events", async () => {
      type Event = { type: string; data: any };
      const { promise: promiseA, resolve: resolveA } = lock<Event>();
      const { promise: promiseB, resolve: resolveB } = lock<Event>();

      const events = [promiseA, promiseB, Promise.resolve({ type: "STOP" })];

      const service = new ScriptingService({
        data: dataFnMock({ getEvent: () => events.shift() }),
      } as any as JsonDataService);
      await nextTick();

      const eventHandlerA = vi.fn();
      service.registerEventHandler("a", eventHandlerA);
      const eventHandlerB = vi.fn();
      service.registerEventHandler("b", eventHandlerB);

      // Resolve the first event - expect eventHandlerA to be called
      resolveA({ type: "a", data: "foo" });

      await nextTick();
      expect(eventHandlerA).toHaveBeenCalledWith("foo");
      expect(eventHandlerB).not.toHaveBeenCalled();

      // Resolve the second event - expect eventHandlerB to be called
      resolveB({ type: "b", data: "bar" });

      await nextTick();
      expect(eventHandlerB).toHaveBeenCalledWith("bar");
    });

    it("should handle null events gracefully (no timeout)", async () => {
      type Event = { type: string; data: any };
      const { promise: realEventPromise, resolve: resolveRealEvent } =
        lock<Event>();

      // Mix null events with a real event, then stop
      const events = [
        Promise.resolve(null),
        Promise.resolve(null),
        realEventPromise,
        Promise.resolve({ type: "STOP" }),
      ];

      const service = new ScriptingService({
        data: dataFnMock({ getEvent: () => events.shift() }),
      } as any as JsonDataService);
      await nextTick();

      const eventHandler = vi.fn();
      service.registerEventHandler("test", eventHandler);

      // Resolve the real event after null events
      resolveRealEvent({ type: "test", data: "after-nulls" });

      // Wait multiple ticks for all async operations to complete
      await nextTick();
      await nextTick();
      await nextTick();

      // The handler should still be called despite null events
      expect(eventHandler).toHaveBeenCalledWith("after-nulls");
      expect(eventHandler).toHaveBeenCalledTimes(1);
    });
  });

  describe("sendToService", () => {
    it("should call backend methods correctly", async () => {
      const expectedResult = { success: true };
      const testMethodSpy = vi.fn(() => Promise.resolve(expectedResult));
      const anotherMethodSpy = vi.fn(() => Promise.resolve(expectedResult));

      const service = new ScriptingService({
        data: dataFnMock({
          testMethod: testMethodSpy,
          anotherMethod: anotherMethodSpy,
        }),
      } as any as JsonDataService);

      // Test without options
      const result1 = await service.sendToService("testMethod");
      expect(testMethodSpy).toHaveBeenCalledWith(undefined);
      expect(result1).toBe(expectedResult);

      // Test with options
      const testOptions = ["param1", "param2"];
      const result2 = await service.sendToService("anotherMethod", testOptions);
      expect(anotherMethodSpy).toHaveBeenCalledWith(testOptions);
      expect(result2).toBe(expectedResult);
    });
  });

  describe("service methods", () => {
    it("isKaiEnabled should call backend correctly", async () => {
      const isKaiEnabledSpy = vi.fn(() => Promise.resolve(true));

      const service = new ScriptingService({
        data: dataFnMock({
          isKaiEnabled: isKaiEnabledSpy,
        }),
      } as any as JsonDataService);

      const result = await service.isKaiEnabled();

      expect(isKaiEnabledSpy).toHaveBeenCalledWith(undefined);
      expect(result).toBe(true);
    });

    it("isLoggedIntoHub should call backend correctly", async () => {
      const isLoggedIntoHubSpy = vi.fn(() => Promise.resolve(false));

      const service = new ScriptingService({
        data: dataFnMock({
          isLoggedIntoHub: isLoggedIntoHubSpy,
        }),
      } as any as JsonDataService);

      const result = await service.isLoggedIntoHub();

      expect(isLoggedIntoHubSpy).toHaveBeenCalledWith(undefined);
      expect(result).toBe(false);
    });

    it("getAiDisclaimer should call backend correctly", async () => {
      const disclaimer = "AI disclaimer text";
      const getAiDisclaimerSpy = vi.fn(() => Promise.resolve(disclaimer));

      const service = new ScriptingService({
        data: dataFnMock({
          getAiDisclaimer: getAiDisclaimerSpy,
        }),
      } as any as JsonDataService);

      const result = await service.getAiDisclaimer();

      expect(getAiDisclaimerSpy).toHaveBeenCalledWith(undefined);
      expect(result).toBe(disclaimer);
    });
  });
});
