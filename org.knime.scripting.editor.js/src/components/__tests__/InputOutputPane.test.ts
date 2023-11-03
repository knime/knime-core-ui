import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi, afterEach, beforeEach } from "vitest";
import InputOutputPane from "../InputOutputPane.vue";
import {
  getScriptingService,
  scriptingServiceMock,
} from "@/__mocks__/scripting-service";
import {} from "node:test";
import InputOutputItem, {
  INPUT_OUTPUT_DRAG_EVENT_ID,
  type InputOutputModel,
} from "../InputOutputItem.vue";
import {
  useInputOutputSelectionStore,
  type InputOutputSelectionStore,
} from "../../store/io-selection";

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
      getScriptingService().getScript.mockReturnValue("this is my script");
    });

    afterEach(() => {
      inputOutputSelectionStore.clearSelection();
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
      expect(getScriptingService().getScript).not.toHaveBeenCalled();
      expect(getScriptingService().setScript).not.toHaveBeenCalled();
    });

    it("drop event handler adds required import", async () => {
      const wrapper = mount(InputOutputPane);
      await flushPromises();
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as Function;
      const mockEventListener = { dispose: vi.fn() };
      getScriptingService().setOnDidChangeContentListener.mockReturnValue(
        mockEventListener,
      );

      // run drop event handler
      dropEventHandler(correctSourceDropEventMock);
      expect(
        correctSourceDropEventMock.dataTransfer.getData,
      ).toHaveBeenCalledWith("eventId");
      // check whether script contains import
      expect(getScriptingService().getScript).toHaveBeenCalled();
      // if it doesn't, check that a content change listener was registered
      expect(
        getScriptingService().setOnDidChangeContentListener,
      ).toHaveBeenCalled();
      const listener =
        getScriptingService().setOnDidChangeContentListener.mock.calls[0][0];
      // trigger the listener manually and check whether the import was prepended
      // and the content listener disposed
      listener();
      expect(mockEventListener.dispose).toHaveBeenCalled();
      expect(getScriptingService().setScript).toHaveBeenCalledWith(
        "import me\nthis is my script",
      );
    });

    it("drop event handler does not add required import if it is already in script", async () => {
      getScriptingService().getScript.mockReturnValue(
        "import me\nthis is my script",
      );
      // getScriptingService().getScript = vi.fn(() => "this is my script") as any;
      const wrapper = mount(InputOutputPane);
      await flushPromises();
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as Function;
      // run drop event handler
      dropEventHandler(correctSourceDropEventMock);
      expect(
        getScriptingService().setOnDidChangeContentListener,
      ).not.toHaveBeenCalled();
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
