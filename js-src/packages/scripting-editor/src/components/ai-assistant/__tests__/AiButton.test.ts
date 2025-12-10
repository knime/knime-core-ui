import { afterEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";

import { initMocked } from "../../../init";
import {
  type GenericInitialData,
  type InputConnectionInfo,
} from "../../../initial-data-service";
import { DEFAULT_INITIAL_DATA } from "../../../initial-data-service-browser-mock";
import type { PaneSizes } from "../../utils/paneSizes";
import AiButton from "../AiButton.vue";

const doMount = async (
  args: {
    props?: Partial<InstanceType<typeof AiButton>["$props"]>;
  } = {},
) => {
  const wrapper = mount(AiButton, {
    props: {
      currentPaneSizes: { left: 0, right: 0, bottom: 0 } satisfies PaneSizes,
      showButtonText: true,
      ...args.props,
    },
  });

  await flushPromises();
  await wrapper.vm.$nextTick();

  return wrapper;
};

/** Override some initial data for tests */
const mockInitialData = (initialData: Partial<GenericInitialData>) =>
  initMocked({
    initialData: { ...DEFAULT_INITIAL_DATA, ...initialData },
  });

describe("AiButton", () => {
  afterEach(() => {
    // Restore the default initial data mock
    initMocked({ initialData: DEFAULT_INITIAL_DATA });
    vi.restoreAllMocks();
  });

  it("hides ai button if K-AI is disabled", async () => {
    mockInitialData({
      kAiConfig: {
        ...DEFAULT_INITIAL_DATA.kAiConfig,
        isKaiEnabled: false,
      },
    });

    const wrapper = await doMount();

    const button = wrapper.findComponent(".ai-button");
    expect(button.exists()).toBeFalsy();
  });

  it("renders ai button if K-AI is enabled", async () => {
    const wrapper = await doMount();

    const button = wrapper.findComponent(".ai-button");
    expect(button.exists()).toBeTruthy();
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

  it("test aiButton is enabled if the node has no input ports", async () => {
    mockInitialData({ inputConnectionInfo: [] });

    const wrapper = await doMount();
    const button = wrapper.findComponent({ ref: "aiButtonRef" });
    expect(button.props().disabled).toBeFalsy();
  });

  it("test aiButton is enabled if optional ports are not connected", async () => {
    mockInitialData({
      inputConnectionInfo: [
        { status: "MISSING_CONNECTION", isOptional: true },
        { status: "UNCONFIGURED_CONNECTION", isOptional: true },
        { status: "OK", isOptional: false },
      ] satisfies InputConnectionInfo[],
    });

    const wrapper = await doMount();
    const button = wrapper.findComponent({ ref: "aiButtonRef" });
    expect(button.props().disabled).toBeFalsy();
  });

  it("test aiButton is disabled if inputs are not available", async () => {
    mockInitialData({
      inputConnectionInfo: [
        { status: "OK", isOptional: true },
        { status: "MISSING_CONNECTION", isOptional: false },
      ] satisfies InputConnectionInfo[],
    });

    const wrapper = await doMount();
    const button = wrapper.findComponent({ ref: "aiButtonRef" });
    expect(button.props().disabled).toBeTruthy();
  });
});
