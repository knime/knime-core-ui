import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { sleep } from "@knime/utils";

import { DialogService, JsonDataService } from "@knime/ui-extension-service";

vi.mock("monaco-editor");

vi.mock("@knime/ui-extension-service", () => ({
  JsonDataService: vi.fn(),
  DialogService: vi.fn(),
}));

const lock = <T = void>() => {
  let resolve: (resolvedValue: T) => void = () => {};
  const promise = new Promise<T>((r) => {
    resolve = r;
  });
  return { promise, resolve };
};

describe("scripting-service", () => {
  let _jsonDataService: any, _dialogService: any;

  const getScriptingService = async () =>
    (await import("../scripting-service")).getScriptingService();

  beforeEach(() => {
    // Make sure the module is reloaded to reset the singleton instance
    vi.resetModules();

    // Mock the services
    _jsonDataService = {
      registerDataGetter: vi.fn(() => {}),
      initialData: vi.fn(() => ({ script: "foo" })),
      data: vi.fn(() => Promise.resolve()),
      applyData: vi.fn(() => {}),
    };
    _dialogService = {
      setApplyListener: vi.fn(),
    };
    JsonDataService.getInstance = vi.fn().mockResolvedValue(_jsonDataService);
    DialogService.getInstance = vi.fn().mockResolvedValue(_dialogService);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("waits for knime service initialization", async () => {
    const sendToServiceResolved = vi.fn();
    const sendToServiceRejected = vi.fn();

    // Replace the mock such that we can block the initialization
    const { promise, resolve } = lock<JsonDataService>();
    JsonDataService.getInstance = vi.fn().mockReturnValue(promise);
    const sendToServicePromise = (await getScriptingService())
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
    resolve(_jsonDataService);

    await sendToServicePromise;
    expect(sendToServiceResolved).toHaveBeenCalled();
    expect(sendToServiceRejected).not.toHaveBeenCalled();
  });

  it("sends requests to the JsonDataService", async () => {
    await (await getScriptingService()).sendToService("dummy", ["foo", "bar"]);
    expect(_jsonDataService.data).toHaveBeenCalledWith({
      method: "ScriptingService.dummy",
      options: ["foo", "bar"],
    });
  });

  describe("settings", () => {
    it("gets initial data from the JsonDataService", async () => {
      const initalSettings = await (
        await getScriptingService()
      ).getInitialSettings();
      expect(initalSettings).toEqual({ script: "foo" });
      expect(_jsonDataService.initialData).toHaveBeenCalledOnce();
      expect(_jsonDataService.initialData).toHaveBeenCalledWith();
    });

    describe("registerSettingsGetterForApply", () => {
      it("adds listener in DialogService to apply data in JsonDataService", async () => {
        const settings = { script: "myScript" };
        const scriptingService = await getScriptingService();
        await scriptingService.registerSettingsGetterForApply(() => settings);
        expect(_dialogService.setApplyListener).toHaveBeenCalled();
        const applyListener = _dialogService.setApplyListener.mock.calls[0][0];
        expect(await applyListener()).toStrictEqual({ isApplied: true });
        expect(_jsonDataService.applyData).toHaveBeenCalledWith(settings);
      });

      it("does not apply settings if they are invalid", async () => {
        const settings = { script: "myScript" };
        const scriptingService = await getScriptingService();
        await scriptingService.registerSettingsGetterForApply(() => settings);
        const applyListener = _dialogService.setApplyListener.mock.calls[0][0];
        _jsonDataService.applyData.mockReturnValue(
          Promise.reject(new Error("invalid settings")),
        );
        expect(await applyListener()).toStrictEqual({ isApplied: false });
      });
    });
  });

  describe("input / output objects", () => {
    it("requests getFlowVariableInputs", async () => {
      await (await getScriptingService()).getFlowVariableInputs();
      expect(_jsonDataService.data).toHaveBeenCalledWith({
        method: "ScriptingService.getFlowVariableInputs",
      });
    });

    it("requests getInputObjects", async () => {
      await (await getScriptingService()).getInputObjects();
      expect(_jsonDataService.data).toHaveBeenCalledWith({
        method: "ScriptingService.getInputObjects",
      });
    });

    it("requests getOutputObjects", async () => {
      await (await getScriptingService()).getOutputObjects();
      expect(_jsonDataService.data).toHaveBeenCalledWith({
        method: "ScriptingService.getOutputObjects",
      });
    });
  });
});
