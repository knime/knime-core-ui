import OutputConsole, {
  type ConsoleHandler,
} from "@/components/OutputConsole.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import FunctionButton from "webapps-common/ui/components/FunctionButton.vue";
import { Terminal } from "xterm";

vi.mock("xterm");

describe("OutputConsole", () => {
  const doMount = async () => {
    const wrapper = mount(OutputConsole);
    await flushPromises();

    // Get the terminal instance
    const term = vi.mocked(Terminal).mock.instances[0];

    // Get the handler to write to the console
    expect(wrapper.emitted()).toHaveProperty("console-created");
    // @ts-ignore
    const handler = wrapper.emitted()[
      "console-created"
    ][0][0] as ConsoleHandler;

    return { wrapper, term, handler };
  };

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("create console", async () => {
    const { term } = await doMount();
    expect(term.open).toHaveBeenCalledOnce();
  });

  it("write to console", async () => {
    const { term, handler } = await doMount();

    handler.write({ text: "hallo" });
    expect(term.write).toBeCalledWith("hallo");
  });

  it("write to console with new line", async () => {
    const { term, handler } = await doMount();

    handler.writeln({ text: "hallo" });
    expect(term.writeln).toBeCalledWith("hallo");
  });

  it("does not highlight text", async () => {
    const { term, handler } = await doMount();

    handler.write({ text: "hallo" });
    expect(term.write).toBeCalledWith("hallo");
  });

  it("highlights error red and bold", async () => {
    const { term, handler } = await doMount();

    handler.write({ error: "my error" });
    expect(term.write).toBeCalledWith(
      "❌ \u001b[48;5;224m\u001b[30mmy error\u001b[0m",
    );
  });

  it("highlights warning yellow", async () => {
    const { term, handler } = await doMount();

    handler.write({ warning: "my warning" });
    expect(term.write).toBeCalledWith(
      "⚠️  \u001b[47m\u001b[30mmy warning\u001b[0m",
    );
  });

  it("clear via click button", async () => {
    const { wrapper, term } = await doMount();

    // get Clear Button and Click it
    const button = wrapper.findComponent(FunctionButton);
    button.vm.$emit("click");
    await flushPromises();

    expect(term.reset).toHaveBeenCalledOnce();
  });

  it("clear via handler", async () => {
    const { term, handler } = await doMount();

    handler.clear();
    expect(term.reset).toHaveBeenCalledOnce();
  });

  it("displays slotted content", () => {
    const wrapper = mount(OutputConsole, {
      slots: {
        "console-status": "<div class='test-class'>Test</div>",
      },
    });
    const slotContent = wrapper.find(".test-class");
    expect(slotContent.exists()).toBeTruthy();
  });

  it("watches for output", async () => {
    const { term } = await doMount();
    expect(term.onWriteParsed).toHaveBeenCalled();
  });
});
