import OutputConsole, {
  type ConsoleText,
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
    const handler = wrapper.emitted()["console-created"][0][0];

    return { wrapper, term, handler: handler as (text: ConsoleText) => void };
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

    handler({ text: "hallo" });
    expect(term.write).toBeCalledWith("hallo");
  });

  it("does not highlight text", async () => {
    const { term, handler } = await doMount();

    handler({ text: "hallo" });
    expect(term.write).toBeCalledWith("hallo");
  });

  it("highlights error red and bold", async () => {
    const { term, handler } = await doMount();

    handler({ error: "my error" });
    expect(term.write).toBeCalledWith(
      "❌ \u001b[48;5;224m\u001b[30mmy error\u001b[0m",
    );
  });

  it("highlights warning yellow", async () => {
    const { term, handler } = await doMount();

    handler({ warning: "my warning" });
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
});
