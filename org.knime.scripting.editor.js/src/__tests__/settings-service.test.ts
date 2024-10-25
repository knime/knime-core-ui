import { afterEach, describe, expect, it, vi } from "vitest";

import { getSettingsService } from "@/settings-service";

const { mockedJsonDataService, mockedDialogueService } = vi.hoisted(() => {
  const mockedJsonDataService = {
    registerDataGetter: vi.fn(() => {}),
    initialData: vi.fn(() => ({ script: "foo" })),
    data: vi.fn(() => Promise.resolve()),
    applyData: vi.fn(() => {}),
  };
  const mockedDialogueService = {
    setApplyListener: vi.fn(),
  };

  return {
    mockedJsonDataService: {
      ...mockedJsonDataService,
      getInstance: vi.fn(() => Promise.resolve(mockedJsonDataService)),
    },
    mockedDialogueService: {
      ...mockedDialogueService,
      getInstance: vi.fn(() => Promise.resolve(mockedDialogueService)),
    },
  };
});
vi.mock("@knime/ui-extension-service", () => ({
  JsonDataService: mockedJsonDataService,
  DialogService: mockedDialogueService,
}));

describe("settings", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  describe("registerSettingsGetterForApply", () => {
    it("adds listener in DialogService to apply data in JsonDataService", async () => {
      const settings = { script: "myScript" };
      const settingsService = getSettingsService();
      await settingsService.registerSettingsGetterForApply(() => settings);
      expect(mockedDialogueService.setApplyListener).toHaveBeenCalled();
      const applyListener =
        mockedDialogueService.setApplyListener.mock.calls[0][0];
      expect(await applyListener()).toStrictEqual({ isApplied: true });
      expect(mockedJsonDataService.applyData).toHaveBeenCalledWith(settings);
    });

    it("does not apply settings if they are invalid", async () => {
      const settings = { script: "myScript" };
      const settingsService = getSettingsService();
      await settingsService.registerSettingsGetterForApply(() => settings);
      const applyListener =
        mockedDialogueService.setApplyListener.mock.calls[0][0];
      mockedJsonDataService.applyData.mockReturnValue(
        Promise.reject(new Error("invalid settings")) as any,
      );
      expect(await applyListener()).toStrictEqual({ isApplied: false });
    });
  });
});
