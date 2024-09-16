import { DEFAULT_INITIAL_DATA } from "@/initial-data-service-browser-mock";
import { it, vi, expect, describe, afterEach } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";
import AiButton from "@/components/ai-assistant/AiButton.vue";
import {
  getInitialDataService,
  type InputConnectionInfo,
} from "@/initial-data-service";
import type { PaneSizes } from "@/components/utils/paneSizes";

vi.mock("@/scripting-service");
vi.mock("@/initial-data-service", () => ({
  getInitialDataService: vi.fn(() => ({
    getInitialData: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_DATA)),
  })),
}));
const doMount = async (
  args: {
    props?: Partial<InstanceType<typeof AiButton>["$props"]>;
  } = {
    props: {
      currentPaneSizes: { left: 0, right: 0, bottom: 0 } satisfies PaneSizes,
      showButtonText: true,
    },
  },
) => {
  const wrapper = mount(AiButton, {
    // @ts-ignore I'm not sure why this line gives a type error, because it's fine
    props: args.props,
  });

  await flushPromises();
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("AiButton", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("hides ai button if ai assistant disabled", async () => {
    vi.mocked(getInitialDataService).mockReturnValue({
      getInitialData: vi.fn(() =>
        Promise.resolve({
          ...DEFAULT_INITIAL_DATA,
          kAiConfig: {
            ...DEFAULT_INITIAL_DATA.kAiConfig,
            codeAssistantEnabled: false,
          },
        }),
      ),
    });

    const wrapper = await doMount();

    const button = wrapper.findComponent(".ai-button");
    expect(button.exists()).toBeFalsy();
  });

  it("ai button opens ai bar", async () => {
    const wrapper = await doMount();

    const button = wrapper.find(".ai-button");

    // aiBar should be turned off at mount
    expect(wrapper.find("[data-testid='ai-popup']").exists()).toBeFalsy();
    await button.trigger("click");

    // then it should be visible
    expect(wrapper.find("[data-testid='ai-popup']").exists()).toBeTruthy();
  });

  it("ai button closes ai bar if it is opened", async () => {
    const wrapper = await doMount();

    const button = wrapper.find(".ai-button");

    expect(wrapper.find("[data-testid='ai-popup']").exists()).toBeFalsy();
    await button.trigger("click");
    expect(wrapper.find("[data-testid='ai-popup']").exists()).toBeTruthy();
    await button.trigger("click");
    expect(wrapper.find("[data-testid='ai-popup']").exists()).toBeFalsy();
  });

  it("ai bar is not closed on click inside of ai bar", async () => {
    const wrapper = await doMount();

    const button = wrapper.find(".ai-button");
    await wrapper.vm.$nextTick();
    await button.trigger("click");
    expect(wrapper.find("[data-testid='ai-popup']").exists()).toBeTruthy();
    await wrapper.find("[data-testid='ai-popup']").trigger("click");
    expect(wrapper.find("[data-testid='ai-popup']").exists()).toBeTruthy();
  });

  it("test aiButton is available if inputs and code assistance are available", async () => {
    const wrapper = await doMount();

    const button = wrapper.findComponent({ ref: "aiButtonRef" });

    expect(button.props().disabled).toBeFalsy();
  });

  it("test aiButton is disabled if inputs are not available", async () => {
    vi.mocked(getInitialDataService).mockReturnValue({
      getInitialData: vi.fn(() =>
        Promise.resolve({
          ...DEFAULT_INITIAL_DATA,
          inputConnectionInfo: [
            {
              status: "OK",
              isOptional: true,
            },
            {
              status: "MISSING_CONNECTION",
              isOptional: false,
            },
          ] satisfies InputConnectionInfo[],
        }),
      ),
    });

    const wrapper = await doMount();

    const button = wrapper.findComponent({ ref: "aiButtonRef" });

    expect(button.props().disabled).toBeTruthy();
  });

  it("test aiButton is enabled even if code assistant is not installed", async () => {
    vi.mocked(getInitialDataService().getInitialData).mockResolvedValue({
      ...DEFAULT_INITIAL_DATA,
      kAiConfig: {
        ...DEFAULT_INITIAL_DATA.kAiConfig,
        codeAssistantInstalled: false,
      },
    });

    const wrapper = await doMount();

    const button = wrapper.findComponent({ ref: "aiButtonRef" });

    expect(button.props().disabled).toBeFalsy();
  });
});
