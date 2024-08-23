import { describe, afterEach, it, vi, expect, beforeEach } from "vitest";
import CodeEditorControlBar from "../CodeEditorControlBar.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { DEFAULT_INITIAL_DATA } from "@/initial-data-service-browser-mock";

import type { PaneSizes } from "../utils/paneSizes";
import AiButton from "../ai-assistant/AiButton.vue";

vi.mock("@/scripting-service");
vi.mock("@/initial-data-service", () => ({
  getInitialDataService: vi.fn(() => ({
    getInitialData: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_DATA)),
  })),
}));

const doMount = async (
  args: {
    props?: Partial<InstanceType<typeof CodeEditorControlBar>["$props"]>;
    slots?: any;
  } = {
    props: {
      currentPaneSizes: { left: 0, right: 0, bottom: 0 } satisfies PaneSizes,
    },
    slots: {},
  },
) => {
  const wrapper = mount(CodeEditorControlBar, {
    // @ts-ignore
    props: args.props,
    slots: args.slots,
    attachTo: "body",
  });

  await flushPromises();

  return wrapper;
};

describe("CodeEditorControlBar", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  beforeEach(() => {
    vi.resetModules();
  });

  it("passes slotted content (via the 'controls' slot)", async () => {
    const wrapper = await doMount({
      slots: {
        controls: "<button id='my-slotted-test'>Submit</button>",
      },
    });
    expect(wrapper.find("#my-slotted-test").exists()).toBeTruthy();
  });

  it("mounts the AiButton component", async () => {
    const wrapper = await doMount();

    expect(wrapper.findComponent(AiButton).exists()).toBe(true);
  });
});
