import { afterEach, describe, expect, it, vi } from "vitest";
import { getInitialDataService } from "@/initial-data-service";

const { mockedJsonDataService, mockedDialogueService } = vi.hoisted(() => {
  const mockedJsonDataService = {
    registerDataGetter: vi.fn(() => {}),
    initialData: vi.fn(() => ({ script: "foo" })),
    data: vi.fn(() => Promise.resolve()),
    applyData: vi.fn(() => {}),
  };

  return {
    mockedJsonDataService: {
      ...mockedJsonDataService,
      getInstance: vi.fn(() => Promise.resolve(mockedJsonDataService)),
    },
    mockedDialogueService: {
      setApplyListener: vi.fn(),
      getInstance: vi.fn(),
    },
  };
});

vi.mock("@knime/ui-extension-service", () => ({
  JsonDataService: mockedJsonDataService,
  DialogService: mockedDialogueService,
}));

describe("initial settings", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("requests initial data", async () => {
    await getInitialDataService().getInitialData();
    expect(
      (await mockedJsonDataService.getInstance()).initialData,
    ).toHaveBeenCalled();
  });
});
