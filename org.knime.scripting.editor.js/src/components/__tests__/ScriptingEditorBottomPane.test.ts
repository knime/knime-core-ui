import { afterEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { flushPromises, mount } from "@vue/test-utils";

import { type ConsoleHandler } from "../OutputConsole.vue";
import ScriptingEditorBottomPane from "../ScriptingEditorBottomPane.vue";

describe("ScriptingEditorBottomPane", () => {
  const { consoleHandler } = vi.hoisted(() => ({
    consoleHandler: {
      writeln: vi.fn(),
      write: vi.fn(),
      clear: vi.fn(),
    } satisfies ConsoleHandler,
  }));

  // Get the console handler
  vi.mock("@/consoleHandler", () => ({
    setConsoleHandler: vi.fn(),
    consoleHandler,
  }));
  vi.mock("xterm");

  vi.mock("@/scripting-service");
  vi.mock("@/editor");

  const doMount = async (
    args: {
      props?: Partial<InstanceType<typeof ScriptingEditorBottomPane>["$props"]>;
      slots?: any;
    } = {
      props: {},
      slots: {},
    },
  ) => {
    const wrapper = mount(ScriptingEditorBottomPane, {
      props: {
        slottedTabs: [],
        ...args.props,
      },
      slots: args.slots,
      attachTo: "body",
    });
    await flushPromises();

    return { wrapper, consoleHandler };
  };

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("displays content passed by slot (but only selected tab)", async () => {
    const { wrapper } = await doMount({
      props: {
        slottedTabs: [
          {
            slotName: "bottomPaneTabSlot:someSlottedThing",
            label: "SomeSlottedThing",
          },
          {
            slotName: "bottomPaneTabSlot:someSlottedThing2",
            label: "SomeSlottedThing2",
          },
        ],
      },
      slots: {
        "bottomPaneTabSlot:someSlottedThing":
          "<div id='test-slotted-id'>Test</div>",
        "bottomPaneTabSlot:someSlottedThing2":
          "<div id='test-slotted-id-2'>Test</div>",
      },
    });

    expect(wrapper.find("#test-slotted-id").isVisible()).toBeTruthy();
    expect(wrapper.find("#test-slotted-id-2").isVisible()).toBeFalsy();
  });

  it("displays extra controls for each slot (but only for selected tab)", async () => {
    const { wrapper } = await doMount({
      props: {
        slottedTabs: [
          {
            slotName: "bottomPaneTabSlot:someSlottedThing",
            label: "SomeSlottedThing",
            associatedControlsSlotName:
              "bottomPaneTabControlsSlot:someSlottedThingControls",
          },
          {
            slotName: "bottomPaneTabSlot:someSlottedThingControls",
            label: "Controls",
          },
          {
            slotName: "bottomPaneTabSlot:someSlottedThing2",
            label: "SomeSlottedThing2",
            associatedControlsSlotName:
              "bottomPaneTabControlsSlot:someSlottedThing2Controls",
          },
          {
            slotName: "bottomPaneTabSlot:someSlottedThing2Controls",
            label: "Controls2",
          },
        ],
      },
      slots: {
        "bottomPaneTabSlot:someSlottedThing":
          "<div id='test-slotted-id'>Test</div>",
        "bottomPaneTabControlsSlot:someSlottedThingControls":
          "<div id='test-slotted-id-2'>Test</div>",
        "bottomPaneTabSlot:someSlottedThing2":
          "<div id='test-slotted-id-3'>Test</div>",
        "bottomPaneTabControlsSlot:someSlottedThing2Controls":
          "<div id='test-slotted-id-4'>Test</div>",
      },
    });

    expect(wrapper.find("#test-slotted-id").isVisible()).toBeTruthy();
    expect(wrapper.find("#test-slotted-id-2").isVisible()).toBeTruthy();
    expect(wrapper.find("#test-slotted-id-3").isVisible()).toBeFalsy();
    expect(wrapper.find("#test-slotted-id-4").isVisible()).toBeFalsy();
  });

  it("lets you switch between tabs and their associated controls", async () => {
    const { wrapper } = await doMount({
      props: {
        slottedTabs: [
          {
            slotName: "bottomPaneTabSlot:someSlottedThing",
            associatedControlsSlotName:
              "bottomPaneTabControlsSlot:someSlottedThingControls",
            label: "SomeSlottedThing",
          },
          {
            slotName: "bottomPaneTabSlot:someSlottedThing2",
            label: "SomeSlottedThing2",
          },
        ],
      },
      slots: {
        "bottomPaneTabSlot:someSlottedThing":
          "<div id='test-slotted-id-1a'>Test</div>",
        "bottomPaneTabControlsSlot:someSlottedThingControls":
          "<div id='test-slotted-id-1b'>Test</div>",
        "bottomPaneTabSlot:someSlottedThing2":
          "<div id='test-slotted-id-2'>Test</div>",
      },
    });

    expect(wrapper.find("#test-slotted-id-1a").isVisible()).toBeTruthy();
    expect(wrapper.find("#test-slotted-id-1b").isVisible()).toBeTruthy();
    expect(wrapper.find("#test-slotted-id-2").isVisible()).toBeFalsy();

    await wrapper.findAll("label")[1].trigger("click");
    await nextTick();

    expect(wrapper.find("#test-slotted-id-1a").isVisible()).toBeFalsy();
    expect(wrapper.find("#test-slotted-id-1b").isVisible()).toBeFalsy();
    expect(wrapper.find("#test-slotted-id-2").isVisible()).toBeTruthy();
  });

  it("displays slotted console status", async () => {
    const { wrapper } = await doMount({
      slots: {
        "status-label": "<div id='test-id'>Test</div>",
      },
    });

    const slotContent = wrapper.find("#test-id");
    expect(slotContent.isVisible()).toBeTruthy();
  });
});
