import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";

import CodeEditorControlBar from "../CodeEditorControlBar.vue";
import AiButton from "../ai-assistant/AiButton.vue";

const doMount = async (
  args: {
    props?: Partial<InstanceType<typeof CodeEditorControlBar>["$props"]>;
    slots?: any;
  } = {
    slots: {},
  },
) => {
  const wrapper = mount(CodeEditorControlBar, {
    props: {
      currentPaneSizes: { left: 0, right: 0, bottom: 0 },
      ...args.props,
    },
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
