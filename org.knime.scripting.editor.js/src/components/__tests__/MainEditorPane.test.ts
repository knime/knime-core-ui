import { type GenericNodeSettings } from "@/settings-service";
import {
  DEFAULT_INITIAL_SETTINGS,
  registerSettingsMock,
} from "@/settings-service-browser-mock";
import { flushPromises, mount } from "@vue/test-utils";
import { describe, it, vi, expect, afterEach, beforeEach } from "vitest";
import MainEditorPane from "../MainEditorPane.vue";
import { onKeyStroke } from "@vueuse/core";
import { useMainCodeEditor } from "@/editor";

vi.mock("@vueuse/core", async (importOriginal) => {
  const original = await importOriginal<typeof import("@vueuse/core")>();
  return {
    ...original,
    onKeyStroke: vi.fn(),
  };
});
vi.mock("@/scripting-service");
vi.mock("@/editor");

const registerSettingsGetterForApplyMock = vi.hoisted(() =>
  vi.fn(() => {
    return Promise.resolve();
  }),
);

vi.mock("@/settings-service", () => ({
  getSettingsService: vi.fn(() => ({
    getSettings: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_SETTINGS)),
    registerSettingsGetterForApply: registerSettingsGetterForApplyMock,
    registerSettings: vi.fn(registerSettingsMock),
  })),
}));

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
    // @ts-ignore - for some reason typescript thinks mock.calls is guaranteed to be empty...
    const settingsGetter = calls[0][0] as unknown as Function;
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
    // @ts-ignore - for some reason typescript thinks mock.calls is guaranteed to be empty...
    const settingsGetter = calls[0][0] as unknown as Function;

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
