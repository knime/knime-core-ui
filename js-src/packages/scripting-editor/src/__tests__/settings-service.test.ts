import { describe, expect, it, vi } from "vitest";

import type {
  DialogService,
  JsonDataService,
} from "@knime/ui-extension-service";

import { type GenericNodeSettings, SettingsService } from "../settings-service";

describe("settings-service", () => {
  it("should create SettingsService instance", () => {
    const initialSettings: GenericNodeSettings = { script: "test" };
    const mockDialogService = {} as DialogService;
    const mockJsonDataService = {} as JsonDataService;

    const service = new SettingsService(
      initialSettings,
      mockDialogService,
      mockJsonDataService,
    );

    expect(service).toBeDefined();
  });

  it("should return initial settings from getSettings", () => {
    const initialSettings: GenericNodeSettings = { script: "test script" };
    const mockDialogService = {} as DialogService;
    const mockJsonDataService = {} as JsonDataService;

    const service = new SettingsService(
      initialSettings,
      mockDialogService,
      mockJsonDataService,
    );

    expect(service.getSettings()).toBe(initialSettings);
  });

  it("should register apply listener successfully", async () => {
    const initialSettings: GenericNodeSettings = { script: "initial" };
    const settingsToApply: GenericNodeSettings = { script: "updated" };

    const mockSetApplyListener = vi.fn();
    const mockDialogService = {
      setApplyListener: mockSetApplyListener,
    } as any as DialogService;

    const mockApplyData = vi.fn().mockResolvedValue(undefined);
    const mockJsonDataService = {
      applyData: mockApplyData,
    } as any as JsonDataService;

    const service = new SettingsService(
      initialSettings,
      mockDialogService,
      mockJsonDataService,
    );

    const settingsGetter = vi.fn(() => settingsToApply);
    service.registerSettingsGetterForApply(settingsGetter);

    expect(mockSetApplyListener).toHaveBeenCalledTimes(1);

    // Call the registered listener
    const registeredListener = mockSetApplyListener.mock.calls[0][0];
    const result = await registeredListener();

    expect(settingsGetter).toHaveBeenCalledTimes(1);
    expect(mockApplyData).toHaveBeenCalledWith(settingsToApply);
    expect(result).toEqual({ isApplied: true });
  });

  it("should handle apply errors gracefully", async () => {
    const initialSettings: GenericNodeSettings = { script: "initial" };
    const settingsToApply: GenericNodeSettings = { script: "updated" };

    const mockSetApplyListener = vi.fn();
    const mockDialogService = {
      setApplyListener: mockSetApplyListener,
    } as any as DialogService;

    const mockApplyData = vi.fn().mockRejectedValue(new Error("Apply failed"));
    const mockJsonDataService = {
      applyData: mockApplyData,
    } as any as JsonDataService;

    const service = new SettingsService(
      initialSettings,
      mockDialogService,
      mockJsonDataService,
    );

    const settingsGetter = vi.fn(() => settingsToApply);
    service.registerSettingsGetterForApply(settingsGetter);

    // Call the registered listener
    const registeredListener = mockSetApplyListener.mock.calls[0][0];
    const result = await registeredListener();

    expect(settingsGetter).toHaveBeenCalledTimes(1);
    expect(mockApplyData).toHaveBeenCalledWith(settingsToApply);
    expect(result).toEqual({ isApplied: false });
  });

  it("should register settings and return function", () => {
    const initialSettings: GenericNodeSettings = { script: "test" };
    const testInitialValue = { testProp: "testValue" };

    const mockSettingsRegistrar = vi.fn();
    const mockRegisterSettings = vi.fn(() => mockSettingsRegistrar);
    const mockDialogService = {
      registerSettings: mockRegisterSettings,
    } as any as DialogService;

    const mockJsonDataService = {} as JsonDataService;

    const service = new SettingsService(
      initialSettings,
      mockDialogService,
      mockJsonDataService,
    );

    const registerFunction = service.registerSettings("model");

    expect(typeof registerFunction).toBe("function");

    const result = registerFunction({ initialValue: testInitialValue });

    expect(mockRegisterSettings).toHaveBeenCalledWith("model");
    expect(mockSettingsRegistrar).toHaveBeenCalledWith({
      initialValue: testInitialValue,
    });
    expect(result).toBe(mockSettingsRegistrar.mock.results[0].value);
  });
});
