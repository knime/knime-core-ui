import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import InputOutputPane from "../InputOutputPane.vue";
import { scriptingServiceMock } from "@/__mocks__/scripting-service";
import { afterEach } from "node:test";
import InputOutputItem from "../InputOutputItem.vue";

vi.mock("monaco-editor");
vi.mock("@/scripting-service");

describe("InputOutputPane", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches input/output objects", async () => {
    mount(InputOutputPane);
    await flushPromises();
    expect(scriptingServiceMock.sendToService).toHaveBeenNthCalledWith(
      1,
      "getInputObjects",
    );
    expect(scriptingServiceMock.sendToService).toHaveBeenNthCalledWith(
      2,
      "getOutputObjects",
    );
    expect(scriptingServiceMock.sendToService).toHaveBeenNthCalledWith(
      3,
      "getFlowVariableInputs",
    );
  });

  it("renders input / output items", async () => {
    scriptingServiceMock.sendToService = vi.fn(() => [
      { name: "myObject", type: "myType" },
    ]) as any;
    const wrapper = mount(InputOutputPane);
    await flushPromises();
    expect(wrapper.findAllComponents(InputOutputItem).length).toBe(3);
  });

  it("does not render input / output items if no data is fetched", async () => {
    const wrapper = mount(InputOutputPane);
    await flushPromises();
    expect(wrapper.findComponent(InputOutputItem).exists());
  });
});
