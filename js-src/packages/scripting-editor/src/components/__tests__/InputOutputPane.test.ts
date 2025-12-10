import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { flushPromises, mount } from "@vue/test-utils";

import { useMainCodeEditorStore } from "../../editor";
import { DEFAULT_INITIAL_DATA } from "../../initial-data-service-browser-mock";
import {
  type InputOutputSelectionStore,
  useInputOutputSelectionStore,
} from "../../store/io-selection";
import InputOutputItem, {
  INPUT_OUTPUT_DRAG_EVENT_ID,
  type InputOutputModel,
} from "../InputOutputItem.vue";
import InputOutputPane from "../InputOutputPane.vue";

vi.mock("monaco-editor");
vi.mock("../../editor");

describe("InputOutputPane", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  const doMount = () =>
    mount(InputOutputPane, {
      props: {
        inputOutputItems: [
          ...DEFAULT_INITIAL_DATA.inputObjects,
          DEFAULT_INITIAL_DATA.flowVariables,
          ...DEFAULT_INITIAL_DATA.outputObjects!,
        ],
      },
    });

  it("renders input / output items", async () => {
    const wrapper = doMount();

    const numInputObjects = DEFAULT_INITIAL_DATA.inputObjects.length;
    const numOutputObjects = DEFAULT_INITIAL_DATA.outputObjects!.length;
    const numFlowVariables = 1;

    await flushPromises();
    const inputOutputItemComponents =
      wrapper.findAllComponents(InputOutputItem);
    expect(inputOutputItemComponents.length).toBe(
      numInputObjects + numOutputObjects + numFlowVariables,
    );
    expect(inputOutputItemComponents.at(0)!.props().inputOutputItem).toEqual(
      DEFAULT_INITIAL_DATA.inputObjects[0],
    );
    expect(
      inputOutputItemComponents.at(numInputObjects)!.props().inputOutputItem,
    ).toEqual(DEFAULT_INITIAL_DATA.flowVariables);
    expect(
      inputOutputItemComponents.at(numFlowVariables + numInputObjects)!.props()
        .inputOutputItem,
    ).toEqual(DEFAULT_INITIAL_DATA.outputObjects![0]);
  });

  it("does not render input / output items if no data is fetched", async () => {
    const wrapper = doMount();
    await flushPromises();
    expect(wrapper.findComponent(InputOutputItem).exists()).toBe(true);
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
        { name: "subitem1", type: { displayName: "type1" }, supported: true },
        { name: "subitem2", type: { displayName: "type2" }, supported: true },
        { name: "subitem3", type: { displayName: "type3" }, supported: true },
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
      const wrapper = doMount();
      await flushPromises();
      expect(wrapper.emitted("drop-event-handler-created")).toBeTruthy();
    });

    it("drop event handler does nothing if drag source is not from input output pane", async () => {
      const wrapper = doMount();
      await flushPromises();
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as (payload: any) => void;
      dropEventHandler(wrongSourceDropEventMock);
      expect(
        wrongSourceDropEventMock.dataTransfer.getData,
      ).toHaveBeenCalledWith("eventId");
    });

    it("drop event handler adds required import", async () => {
      const wrapper = doMount();
      await flushPromises();
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as (payload: any) => void;

      // run drop event handler
      dropEventHandler(correctSourceDropEventMock);

      expect(
        correctSourceDropEventMock.dataTransfer.getData,
      ).toHaveBeenCalledWith("eventId");

      const script = useMainCodeEditorStore().value!.text;

      // fake the monaco drop event. Since the next text change
      // after the drop event is assumed to be the addition of
      // the column, we can add literally any text and it'll
      // be interpreted as an inserted column.
      script.value = "this is my script\nand a dropped line";
      await nextTick();

      // check whether script contains import
      expect(script.value).toContain("import me");

      // check that following changes do not add import again
      script.value = "my replaced script";
      await nextTick();
      expect(script.value).toBe("my replaced script");
    });

    it("drop event handler does not add required import if it is already in script", async () => {
      const wrapper = doMount();
      await flushPromises();

      // Add the import to the script before we trigger any drop events
      const script = useMainCodeEditorStore().value!.text;
      script.value = `import me\n${script.value}`;

      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as (payload: any) => void;

      // run drop event handler
      dropEventHandler(correctSourceDropEventMock);

      // fake dropping some text
      script.value = `${script.value}\nand a dropped line`;

      // Check that the import only appears once
      const countImportOccurences = script.value.split("import me").length - 1;
      expect(countImportOccurences).toBe(1);
    });

    it("selection is not cleared after unsuccessful drop", async () => {
      const wrapper = doMount();
      await flushPromises();
      expect(inputOutputSelectionStore.selectedItem).toStrictEqual(item1);
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as (payload: any) => void;
      dropEventHandler(wrongSourceDropEventMock);
      expect(inputOutputSelectionStore.selectedItem).toStrictEqual(item1);
    });

    it("selection is cleared after successful drop", async () => {
      const wrapper = doMount();
      await flushPromises();
      const dropEventHandler = wrapper.emitted(
        "drop-event-handler-created",
      )![0][0] as (payload: any) => void;
      dropEventHandler(correctSourceDropEventMock);
      expect(inputOutputSelectionStore.selectedItem).toBeUndefined();
    });
  });
});
