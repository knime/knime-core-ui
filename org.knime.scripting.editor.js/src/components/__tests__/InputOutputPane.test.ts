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
    expect(scriptingServiceMock.getInputObjects).toHaveBeenCalledOnce();
    expect(scriptingServiceMock.getOutputObjects).toHaveBeenCalledOnce();
    expect(scriptingServiceMock.getFlowVariableInputs).toHaveBeenCalledOnce();
  });

  it("renders input / output items", async () => {
    scriptingServiceMock.getInputObjects = vi.fn(() => [
      { name: "myInputObject1", type: "myType" },
      { name: "myInputObject2", type: "myType" },
    ]);
    scriptingServiceMock.getOutputObjects = vi.fn(() => [
      { name: "myOutputObject", type: "myType" },
    ]);
    scriptingServiceMock.getFlowVariableInputs = vi.fn(() => ({
      name: "myFlowVarInp",
      type: "myType",
    }));
    const wrapper = mount(InputOutputPane);
    await flushPromises();
    const inpOupPanes = wrapper.findAllComponents(InputOutputItem);
    expect(inpOupPanes.length).toBe(4);
    expect(inpOupPanes.at(0)!.props()).toEqual({
      inputOutputItem: {
        name: "myInputObject1",
        type: "myType",
      },
    });
    expect(inpOupPanes.at(1)!.props()).toEqual({
      inputOutputItem: {
        name: "myInputObject2",
        type: "myType",
      },
    });
    expect(inpOupPanes.at(2)!.props()).toEqual({
      inputOutputItem: {
        name: "myOutputObject",
        type: "myType",
      },
    });
    expect(inpOupPanes.at(3)!.props()).toEqual({
      inputOutputItem: {
        name: "myFlowVarInp",
        type: "myType",
      },
    });
  });

  it("does not render input / output items if no data is fetched", async () => {
    const wrapper = mount(InputOutputPane);
    await flushPromises();
    expect(wrapper.findComponent(InputOutputItem).exists());
  });
});
