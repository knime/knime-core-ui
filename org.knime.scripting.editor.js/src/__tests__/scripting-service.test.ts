import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import sleep from "webapps-common/util/sleep";

import {
  IFrameKnimeService,
  JsonDataService,
} from "@knime/ui-extension-service";
import type { ScriptingServiceType } from "../scripting-service";

vi.mock("monaco-editor");

vi.mock("@knime/ui-extension-service", () => ({
  IFrameKnimeService: vi.fn(),
  JsonDataService: vi.fn(),
}));

const lock = () => {
  let resolve: () => void = () => {};
  const promise = new Promise<void>((r) => {
    resolve = r;
  });
  return { promise, resolve };
};

describe("scripting-service", () => {
  let _knimeService: any,
    _jsonDataService: any,
    getScriptingService: (mock?: ScriptingServiceType) => ScriptingServiceType,
    getScriptingServiceWithoutEventPoller: () => ScriptingServiceType;

  beforeEach(async () => {
    // Make sure the module is reloaded to reset the singleton instance
    vi.resetModules();
    getScriptingService = (await import("../scripting-service"))
      .getScriptingService;

    getScriptingServiceWithoutEventPoller = () => {
      const scriptingService = getScriptingService();
      scriptingService.stopEventPoller();
      return scriptingService;
    };

    // Mock the services
    _knimeService = {
      waitForInitialization: vi.fn(() => Promise.resolve()),
    };
    vi.mocked(IFrameKnimeService).mockReturnValue(_knimeService);

    _jsonDataService = {
      registerDataGetter: vi.fn(() => {}),
      initialData: vi.fn(() => ({ script: "foo" })),
      data: vi.fn(() => {}),
      applyData: vi.fn(() => {}),
    };
    vi.mocked(JsonDataService).mockReturnValue(_jsonDataService);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("waits for knime service initialization", async () => {
    const sendToServiceResolved = vi.fn();
    const sendToServiceRejected = vi.fn();

    // Replace the mock such that we can block the initialization
    const { promise, resolve } = lock();
    _knimeService.waitForInitialization.mockReturnValue(promise);
    const sendToServicePromise = getScriptingServiceWithoutEventPoller()
      .sendToService("dummy")
      .then(() => {
        sendToServiceResolved();
      })
      .catch(() => {
        sendToServiceRejected();
      });
    await sleep(20);
    expect(sendToServiceResolved).not.toHaveBeenCalled();
    expect(sendToServiceRejected).not.toHaveBeenCalled();

    // Now the initialization should be resolved
    resolve();

    await sendToServicePromise;
    expect(sendToServiceResolved).toHaveBeenCalled();
    expect(sendToServiceRejected).not.toHaveBeenCalled();
  });

  it("initializes the JsonDataService with the KnimeService", async () => {
    await getScriptingServiceWithoutEventPoller().sendToService("dummy");
    expect(JsonDataService).toHaveBeenCalled();
    expect(JsonDataService).toHaveBeenCalledWith(_knimeService);
  });

  it("sends requests to the JsonDataService", async () => {
    await getScriptingServiceWithoutEventPoller().sendToService("dummy", [
      "foo",
      "bar",
    ]);
    expect(_jsonDataService.data).toHaveBeenCalledWith({
      method: "dummy",
      options: ["foo", "bar"],
    });
  });

  it("uses the mock if provided", () => {
    const mockScriptingService: Partial<ScriptingServiceType> = {
      sendToService: vi.fn(),
    };

    // @ts-ignore
    const scriptingService = getScriptingService(mockScriptingService);
    expect(scriptingService).toBe(mockScriptingService);

    // All future calls should return the mock
    expect(scriptingService).toBe(mockScriptingService);
  });

  describe("settings", () => {
    it("gets initial data from the JsonDataService", async () => {
      const initalSettings =
        await getScriptingServiceWithoutEventPoller().getInitialSettings();
      expect(initalSettings).toEqual({ script: "foo" });
      expect(_jsonDataService.initialData).toHaveBeenCalledOnce();
      expect(_jsonDataService.initialData).toHaveBeenCalledWith();
    });

    it("saves settings by calling JsonDataService.applyData", async () => {
      await getScriptingServiceWithoutEventPoller().saveSettings({
        script: "bar",
      });
      expect(_jsonDataService.applyData).toHaveBeenCalledOnce();
    });

    it("registers the data getter to return the last saved settings", async () => {
      let dataGetter: (() => any) | null = null;
      _jsonDataService.registerDataGetter.mockImplementation(
        (providedDataGetter: () => any) => {
          dataGetter = providedDataGetter;
        },
      );

      const scriptingService = getScriptingServiceWithoutEventPoller();
      await scriptingService.saveSettings({ script: "bar" });
      expect(dataGetter).not.toBeNull();
      expect(dataGetter!()).toEqual({ script: "bar" });

      await scriptingService.saveSettings({ script: "print('Hello world')" });
      expect(dataGetter!()).toEqual({ script: "print('Hello world')" });
    });
  });

  describe("events handler", () => {
    beforeEach(() => {
      // Do not spam the event poller by returning events all the time
      const { promise: blockForever } = lock();
      _jsonDataService.data.mockImplementation(
        ({ method }: { method: string }) => {
          expect(method).toBe("getEvent");
          return blockForever;
        },
      );
    });

    afterEach(() => {
      getScriptingService().stopEventPoller();
    });

    it("polls events from the service", async () => {
      // Return one event
      _jsonDataService.data.mockImplementationOnce(
        ({ method }: { method: string }) => {
          expect(method).toBe("getEvent");
        },
      );

      // Scripting service initialization starts the event poller
      getScriptingService();

      // Wait for the event poller to fetch the event
      await sleep(10);
      expect(_jsonDataService.data).toHaveBeenCalledWith({
        method: "getEvent",
      });
    });

    it("calls the correct event handler", async () => {
      const scriptingService = getScriptingService();

      // Event handler for foo (the first event resolves the promise)
      const { promise: eventFooPromise, resolve: resolveFooEvent } = lock();
      const eventHandlerFoo = vi.fn(resolveFooEvent);
      scriptingService.registerEventHandler("foo", eventHandlerFoo);

      // Event handler for bar (the first event resolves the promise)
      const { promise: eventBarPromise, resolve: resolveBarEvent } = lock();
      const eventHandlerBar = vi.fn(resolveBarEvent);
      scriptingService.registerEventHandler("bar", eventHandlerBar);

      // Let the next data call return events for foo and bar
      _jsonDataService.data.mockImplementationOnce(() => {
        return Promise.resolve({ type: "bar", data: "data for bar" });
      });
      _jsonDataService.data.mockImplementationOnce(() => {
        return Promise.resolve({ type: "foo", data: "data for foo" });
      });

      await eventFooPromise;
      expect(eventHandlerFoo).toHaveBeenCalledWith("data for foo");

      await eventBarPromise;
      expect(eventHandlerBar).toHaveBeenCalledWith("data for bar");
    });

    it("calls the language-server event handler", async () => {
      const scriptingService = getScriptingService();

      const { promise, resolve } = lock();
      const eventHandler = vi.fn(resolve);
      scriptingService.registerLanguageServerEventHandler(eventHandler);

      // Let the next data call return an event for the language server
      _jsonDataService.data.mockImplementationOnce(() => {
        return Promise.resolve({
          type: "language-server",
          data: "data for language server",
        });
      });

      await promise;
      expect(eventHandler).toHaveBeenCalledWith("data for language server");
    });

    it("calls the console event handler", async () => {
      const scriptingService = getScriptingService();

      const { promise, resolve } = lock();
      const eventHandler = vi.fn(resolve);
      scriptingService.registerConsoleEventHandler(eventHandler);

      // Let the next data call return an event for the console
      _jsonDataService.data.mockImplementationOnce(() => {
        return Promise.resolve({
          type: "console",
          data: { text: "my console text", stderr: false },
        });
      });

      await promise;
      expect(eventHandler).toHaveBeenCalledWith({
        text: "my console text",
        stderr: false,
      });
    });
  });

  describe("input / output objects", () => {
    it("requests getFlowVariableInputs", async () => {
      await getScriptingServiceWithoutEventPoller().getFlowVariableInputs();
      expect(_jsonDataService.data).toHaveBeenCalledWith({
        method: "getFlowVariableInputs",
      });
    });

    it("requests getInputObjects", async () => {
      await getScriptingServiceWithoutEventPoller().getInputObjects();
      expect(_jsonDataService.data).toHaveBeenCalledWith({
        method: "getInputObjects",
      });
    });

    it("requests getOutputObjects", async () => {
      await getScriptingServiceWithoutEventPoller().getOutputObjects();
      expect(_jsonDataService.data).toHaveBeenCalledWith({
        method: "getOutputObjects",
      });
    });
  });
});
