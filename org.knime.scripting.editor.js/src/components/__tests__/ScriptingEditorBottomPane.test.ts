import { afterEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";
import { type ConsoleHandler } from "../OutputConsole.vue";
import ScriptingEditorBottomPane from "../ScriptingEditorBottomPane.vue";
import { FunctionButton } from "@knime/components";

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
      props: { slottedTabs: [] },
      slots: {},
    },
  ) => {
    const wrapper = mount(ScriptingEditorBottomPane, {
      props: args.props,
      slots: args.slots,
    });
    await flushPromises();

    return { wrapper, consoleHandler };
  };

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("clears console when clear button is clicked", async () => {
    const { wrapper, consoleHandler } = await doMount();

    // get clear Button and Click it
    const button = wrapper.findComponent(FunctionButton);
    button.vm.$emit("click");
    await flushPromises();

    expect(consoleHandler.clear).toHaveBeenCalled();
  });

  it("displays content passed by slot", async () => {
    const { wrapper } = await doMount({
      props: {
        slottedTabs: [{ value: "someSlottedThing", label: "SomeSlottedThing" }],
      },
      slots: {
        someSlottedThing: "<div id='test-slotted-id'>Test</div>",
      },
    });

    expect(wrapper.find("#test-slotted-id").exists()).toBeTruthy();
  });

  it("displays slotted console status", async () => {
    const { wrapper } = await doMount({
      slots: {
        "console-status": "<div id='test-id'>Test</div>",
      },
    });

    const slotContent = wrapper.find("#test-id");
    expect(slotContent.exists()).toBeTruthy();
  });
});
