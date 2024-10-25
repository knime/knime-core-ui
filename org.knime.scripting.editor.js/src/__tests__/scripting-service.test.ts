import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { DialogService, JsonDataService } from "@knime/ui-extension-service";
import { sleep } from "@knime/utils";

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
});
