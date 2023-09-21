import OutputConsole, {
  type ConsoleText,
} from "@/components/OutputConsole.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import Button from "webapps-common/ui/components/Button.vue";
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

    handler({ text: "hallo", highlightMode: "TEXT" });
    expect(term.write).toBeCalledWith("hallo");
  });

  it("highlights error red and bold", async () => {
    const { term, handler } = await doMount();

    handler({ text: "my error", highlightMode: "ERROR" });
    expect(term.write).toBeCalledWith("\x1b[31m\x1b[1mmy error\x1b[0m");
  });

  it("highlights warning yellow", async () => {
    const { term, handler } = await doMount();

    handler({ text: "my warning", highlightMode: "WARNING" });
    expect(term.write).toBeCalledWith("\x1b[33mmy warning\x1b[0m");
  });

  it("clear via click button", async () => {
    const { wrapper, term } = await doMount();

    // get Clear Button and Click it
    const button = wrapper.findComponent(Button);
    button.vm.$emit("click");
    await flushPromises();

    expect(term.reset).toHaveBeenCalledOnce();
  });
});
