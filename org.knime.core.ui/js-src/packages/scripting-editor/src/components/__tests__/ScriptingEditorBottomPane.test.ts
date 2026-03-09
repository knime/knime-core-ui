import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { flushPromises, mount } from "@vue/test-utils";

import { getScriptingService } from "../../init";
import { DEFAULT_PORT_CONFIGS } from "../../initial-data-service-browser-mock";
import CompactTabBar from "../CompactTabBar.vue";
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
  vi.mock("../../consoleHandler", () => ({
    setConsoleHandler: vi.fn(),
    consoleHandler,
  }));
  vi.mock("xterm");

  vi.mock("../../editor");

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

  beforeEach(() => {
    vi.mocked(getScriptingService().isCallKnimeUiApiAvailable).mockReturnValue(
      Promise.resolve(false),
    );
  });

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

  it("exposes hasTabs=false if no tabs are available", async () => {
    const { wrapper } = await doMount();
    expect(wrapper.vm.hasTabs).toBe(false);
  });

  it("exposes hasTabs=true if there are slotted tabs", async () => {
    const { wrapper } = await doMount({
      props: {
        slottedTabs: [
          {
            slotName: "bottomPaneTabSlot:someSlottedThing",
            label: "SomeSlottedThing",
          },
        ],
      },
    });
    expect(wrapper.vm.hasTabs).toBe(true);
  });

  it("exposes hasTabs=true if there are port config tabs", async () => {
    vi.mocked(
      getScriptingService().isCallKnimeUiApiAvailable,
    ).mockReturnValueOnce(Promise.resolve(true));
    const { wrapper } = await doMount();
    expect(wrapper.vm.hasTabs).toBe(true);
  });

  it("selects the first tab when tabs become available", async () => {
    let resolvePromise: ((value: boolean) => void) | undefined;
    vi.mocked(
      getScriptingService().isCallKnimeUiApiAvailable,
    ).mockReturnValueOnce(
      new Promise((resolve) => {
        resolvePromise = resolve;
      }),
    );

    const { wrapper } = await doMount();
    const tabBar = wrapper.findComponent(CompactTabBar);

    expect(wrapper.vm.hasTabs).toBe(false);
    expect(tabBar.vm.modelValue).toBeNull();

    resolvePromise!(true);
    await flushPromises();

    expect(wrapper.vm.hasTabs).toBe(true);
    expect(tabBar.vm.modelValue).not.toBeNull();
  });

  it("shows input port tabs for connected ports", async () => {
    vi.mocked(
      getScriptingService().isCallKnimeUiApiAvailable,
    ).mockReturnValueOnce(Promise.resolve(true));
    const { wrapper } = await doMount();

    await flushPromises();

    const inOutPane = wrapper.findComponent(ScriptingEditorBottomPane);
    expect(inOutPane.exists()).toBeTruthy();

    const tabBar = inOutPane.findComponent(CompactTabBar);
    expect(tabBar.exists()).toBeTruthy();

    const tabElements = tabBar.findAll("input[type='radio']");

    for (const inputPort of DEFAULT_PORT_CONFIGS.inputPorts) {
      if (typeof inputPort.nodeId === "undefined") {
        // skip unconnected ports
        continue;
      }

      const expectedElement = tabElements.find(
        (tab) =>
          tab.attributes("value") ===
          `bottomPaneTabSlot:${inputPort.nodeId}-${inputPort.portIdx}`,
      );

      expect(expectedElement).toBeDefined();
      expect(expectedElement!.isVisible()).toBeTruthy();
    }
  });

  it("shows input ports in correct order", async () => {
    vi.mocked(
      getScriptingService().isCallKnimeUiApiAvailable,
    ).mockReturnValueOnce(Promise.resolve(true));
    const { wrapper } = await doMount();

    await flushPromises();
    const inOutPane = wrapper.findComponent(ScriptingEditorBottomPane);
    expect(inOutPane.exists()).toBeTruthy();

    const tabBar = inOutPane.findComponent(CompactTabBar);
    expect(tabBar.exists()).toBeTruthy();
    const tabElements = tabBar.findAll("input[type='radio']");
    const actualOrder = tabElements.map((tab) => tab.attributes("value"));

    // All ports except variable port
    const expectedOrderWithoutVariables = DEFAULT_PORT_CONFIGS.inputPorts
      .filter((_, idx) => idx !== 0) // Skip variable port (because it will be displayed last)
      .filter((port) => typeof port.nodeId !== "undefined")
      .map((port) => `bottomPaneTabSlot:${port.nodeId}-${port.portIdx}`);
    expect(actualOrder.slice(0, -1)).toEqual(expectedOrderWithoutVariables);

    // Variable port last
    const variablesPort = DEFAULT_PORT_CONFIGS.inputPorts[0];
    expect(actualOrder[actualOrder.length - 1]).toBe(
      `bottomPaneTabSlot:${variablesPort.nodeId}-${DEFAULT_PORT_CONFIGS.inputPorts[0].portIdx}`,
    );
  });
});
