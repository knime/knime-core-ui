import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi, afterEach, beforeEach } from "vitest";
import InputOutputPane from "../InputOutputPane.vue";
import { scriptingServiceMock } from "@/__mocks__/scripting-service";
import {} from "node:test";
import InputOutputItem, {
  INPUT_OUTPUT_DRAG_EVENT_ID,
  type InputOutputModel,
} from "../InputOutputItem.vue";
import {
  useInputOutputSelectionStore,
  type InputOutputSelectionStore,
} from "../../store/io-selection";
import { useMainCodeEditorStore } from "@/editor";
import { nextTick } from "vue";

vi.mock("monaco-editor");
vi.mock("@/scripting-service");
vi.mock("@/editor");

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
    vi.mocked(scriptingServiceMock.getInputObjects).mockImplementation(() =>
      Promise.resolve([
        { name: "myInputObject1", type: "myType" },
        { name: "myInputObject2", type: "myType" },
      ]),
    );
    vi.mocked(scriptingServiceMock.getOutputObjects).mockImplementation(() =>
      Promise.resolve([{ name: "myOutputObject", type: "myType" }]),
    );
    vi.mocked(scriptingServiceMock.getFlowVariableInputs).mockImplementation(
      () =>
        Promise.resolve({
          name: "myFlowVarInp",
          type: "myType",
        }),
    );
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
        name: "myFlowVarInp",
        type: "myType",
      },
    });
    expect(inpOupPanes.at(3)!.props()).toEqual({
      inputOutputItem: {
        name: "myOutputObject",
        type: "myType",
      },
    });
  });

  it("does not render input / output items if no data is fetched", async () => {
    const wrapper = mount(InputOutputPane);
    await flushPromises();
    expect(wrapper.findComponent(InputOutputItem).exists());
  });

  describe("drop event handler", () => {
    const wrongSourceDropEventMock = {
      dataTransfer: {
        getData: vi.fn((key) => {
          if (key === "eventId") {
            return "wrong source";
          } else {
            throw new Error("key is not mocked");
          }
        }),
      },
    };
    const correctSourceDropEventMock = {
      dataTransfer: {
        getData: vi.fn((key) => {
          if (key === "eventId") {
            return INPUT_OUTPUT_DRAG_EVENT_ID;
          } else {
            throw new Error("key is not mocked");
          }
        }),
      },
    };
    let inputOutputSelectionStore: InputOutputSelectionStore;
    const item1: InputOutputModel = {
      name: "input item 1",
      requiredImport: "import me",
      subItems: [
        { name: "subitem1", type: "type1" },
        { name: "subitem2", type: "type2" },
        { name: "subitem3", type: "type3" },
      ],
    };

    beforeEach(() => {
      inputOutputSelectionStore = useInputOutputSelectionStore();
      inputOutputSelectionStore.selectedItem = item1;
      useMainCodeEditorStore().value!.text.value = "this is my script";
    });

    afterEach(() => {
      delete inputOutputSelectionStore.selectedItem;
      vi.clearAllMocks();
    });

    it("emits drop event on mount", async () => {
      const wrapper = mount(InputOutputPane);
      await flushPromises();
      expect(wrapper.emitted("drop-event-handler-created")).toBeTruthy();
    });

    it("drop event handler does nothing if drag source is not from input output pane", async () => {
      const wrapper = mount(InputOutputPane);
      await flushPromises();
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as Function;
      dropEventHandler(wrongSourceDropEventMock);
      expect(
        wrongSourceDropEventMock.dataTransfer.getData,
      ).toHaveBeenCalledWith("eventId");
    });

    it("drop event handler adds required import", async () => {
      const wrapper = mount(InputOutputPane);
      await flushPromises();
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as Function;

      // run drop event handler
      dropEventHandler(correctSourceDropEventMock);
      expect(
        correctSourceDropEventMock.dataTransfer.getData,
      ).toHaveBeenCalledWith("eventId");

      const script = useMainCodeEditorStore().value!.text;

      // fake the monaco drop event
      script.value = "this is my script\nand a dropped line";
      await nextTick();

      // check whether script contains import
      expect(script.value).toBe(
        "import me\nthis is my script\nand a dropped line",
      );

      // check that following changes do not add import again
      script.value = "my replaced script";
      await nextTick();
      expect(script.value).toBe("my replaced script");
    });

    it("drop event handler does not add required import if it is already in script", async () => {
      const wrapper = mount(InputOutputPane);
      await flushPromises();
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as Function;
      // run drop event handler
      dropEventHandler(correctSourceDropEventMock);
    });

    it("selection is not cleared after unsuccessful drop", async () => {
      const wrapper = mount(InputOutputPane);
      await flushPromises();
      expect(inputOutputSelectionStore.selectedItem).toStrictEqual(item1);
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as Function;
      dropEventHandler(wrongSourceDropEventMock);
      expect(inputOutputSelectionStore.selectedItem).toStrictEqual(item1);
    });

    it("selection is cleared after successful drop", async () => {
      const wrapper = mount(InputOutputPane);
      await flushPromises();
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as Function;
      dropEventHandler(correctSourceDropEventMock);
      expect(inputOutputSelectionStore.selectedItem).toBeUndefined();
    });
  });
});
