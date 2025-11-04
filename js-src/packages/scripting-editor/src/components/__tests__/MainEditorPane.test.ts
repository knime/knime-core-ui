import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";
import { onKeyStroke } from "@vueuse/core";

import { useMainCodeEditor } from "@/editor";
import { getSettingsService } from "@/init";
import { type GenericNodeSettings } from "@/settings-service";
import { DEFAULT_INITIAL_SETTINGS } from "@/settings-service-browser-mock";
import MainEditorPane from "../MainEditorPane.vue";

vi.mock("@vueuse/core", async (importOriginal) => {
  const original = await importOriginal<typeof import("@vueuse/core")>();
  return {
    ...original,
    onKeyStroke: vi.fn(),
  };
});
vi.mock("@/editor");

const registerSettingsGetterForApplyMock = vi.mocked(
  getSettingsService().registerSettingsGetterForApply,
);

const doMount = (
  props?: Partial<InstanceType<typeof MainEditorPane>["$props"]>,
) => {
  const wrapper = mount(MainEditorPane, {
    props: {
      showControlBar: true,
      language: "something",
      fileName: "something.js",
      modelOrView: "model",
      ...props,
    },
  });

  return wrapper;
};

describe("MainEditorPane", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("registers default settings getter", async () => {
    doMount();

    await flushPromises();

    const calls = registerSettingsGetterForApplyMock.mock.calls;
    const settingsGetter = calls[0][0];
    expect(settingsGetter()).toStrictEqual({
      script: DEFAULT_INITIAL_SETTINGS.script,
    });
  });

  it("registers settings getter using toSettings prop", async () => {
    const settings: GenericNodeSettings = { script: "blah" };
    const toSettings = vi.fn().mockReturnValue(settings);
    doMount({ toSettings });

    await flushPromises();

    expect(registerSettingsGetterForApplyMock).toHaveBeenCalledOnce();

    const calls = registerSettingsGetterForApplyMock.mock.calls;
    const settingsGetter = calls[0][0];

    expect(settingsGetter()).toStrictEqual(settings);
  });

  it("should register onKeyStroke handler", () => {
    doMount();
    expect(onKeyStroke).toHaveBeenCalledWith("z", expect.anything());
    expect(onKeyStroke).toHaveBeenCalledWith("Escape", expect.anything());
  });

  it("displays code editor and passes props", () => {
    doMount();
    expect(useMainCodeEditor).toHaveBeenCalledWith({
      container: expect.anything(),
      language: "something",
      fileName: "something.js",
    });
    expect(
      vi.mocked(useMainCodeEditor).mock.calls[0][0].container.value,
    ).toBeDefined();
  });
});
