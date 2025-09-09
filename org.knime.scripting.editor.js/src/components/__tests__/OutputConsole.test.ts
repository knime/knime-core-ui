import { afterEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";
import { Terminal } from "@xterm/xterm";

import OutputConsole, {
  type ConsoleHandler,
} from "@/components/OutputConsole.vue";

describe("OutputConsole", () => {
  const doMount = async () => {
    const wrapper = mount(OutputConsole);
    await flushPromises();

    // Get the terminal instance
    const term = vi.mocked(Terminal).mock.instances[0];

    // Get the handler to write to the console
    expect(wrapper.emitted()).toHaveProperty("console-created");
    // @ts-expect-error - the type of emitted()["console-created"] is unknown
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

  it("adds error icon prefix", async () => {
    const { term, handler } = await doMount();

    handler.write({ error: "my error" });
    expect(term.write).toBeCalledWith(
      "❌ my error",
    );
  });

  it("adds warning icon prefix", async () => {
    const { term, handler } = await doMount();

    handler.write({ warning: "my warning" });
    expect(term.write).toBeCalledWith(
      "⚠️  my warning",
    );
  });

  it("clear via handler", async () => {
    const { term, handler } = await doMount();

    handler.clear();
    expect(term.reset).toHaveBeenCalledOnce();
  });

  it("watches for output", async () => {
    const { term } = await doMount();
    expect(term.onWriteParsed).toHaveBeenCalled();
  });
});
