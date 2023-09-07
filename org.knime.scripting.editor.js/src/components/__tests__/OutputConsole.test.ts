import { describe, it, expect, vi, afterEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import OutputConsole from "@/components/OutputConsole.vue";
import Button from "webapps-common/ui/components/Button.vue";
import { Terminal } from "xterm";

vi.mock("xterm");

describe("OutputConsole", () => {
  let term;

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("create console", async () => {
    mount(OutputConsole);
    await flushPromises();
    term = vi.mocked(Terminal).mock.instances[0];
    expect(term.open).toHaveBeenCalledOnce();
  });

  it("write to console", async () => {
    const outputConsole = mount(OutputConsole);
    await flushPromises();
    term = vi.mocked(Terminal).mock.instances[0];
    expect(term.open).toHaveBeenCalledOnce();
    expect(outputConsole.emitted()).toHaveProperty("console-created");

    // @ts-ignore
    const handler = outputConsole.emitted()["console-created"][0][0];

    // @ts-ignore
    handler({ text: "hallo", stderr: false });
    expect(term.write).toBeCalledWith("hallo");
  });

  it("clear via click button", async () => {
    const outputConsole = mount(OutputConsole);
    await flushPromises();
    term = vi.mocked(Terminal).mock.instances[0];
    expect(term.open).toHaveBeenCalledOnce();

    // get Clear Button and Click it
    const button = outputConsole.findComponent(Button);
    button.vm.$emit("click");
    await flushPromises();

    expect(term.reset).toHaveBeenCalledOnce();
  });
});
