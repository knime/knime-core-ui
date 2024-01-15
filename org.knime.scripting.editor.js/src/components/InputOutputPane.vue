<script setup lang="ts">
import { onMounted, ref, watch, type Ref } from "vue";
import InputOutputItem, {
  INPUT_OUTPUT_DRAG_EVENT_ID,
  type InputOutputModel,
} from "./InputOutputItem.vue";
import { getScriptingService } from "@/scripting-service";
import { useInputOutputSelectionStore } from "@/store/io-selection";
import { useMainCodeEditorStore } from "@/editor";

const emit =
  defineEmits<
    (
      e: "drop-event-handler-created",
      dropEventHandler: (payload: DragEvent) => void,
    ) => void
  >();

const inputOutputItems: Ref<InputOutputModel[]> = ref([]);
const inputOutputSelectionStore = useInputOutputSelectionStore();

const fetchInputOutputObjects = async (
  method: "getInputObjects" | "getOutputObjects",
) => {
  const items = await getScriptingService()[method]();
  if (items) {
    inputOutputItems.value.push(...items);
  }
};

const fetchFlowVariables = async () => {
  const item = await getScriptingService().getFlowVariableInputs();
  if (item) {
    inputOutputItems.value.push(item);
  }
};

const mainEditorState = useMainCodeEditorStore();

const dropEventHandler = (event: DragEvent) => {
  // If source is not input/output element, do nothing
  if (event.dataTransfer?.getData("eventId") !== INPUT_OUTPUT_DRAG_EVENT_ID) {
    return;
  }

  // check if an import is required for the selected item
  const requiredImport =
    inputOutputSelectionStore.selectedItem?.requiredImport ?? null;

  if (
    requiredImport &&
    !mainEditorState.value?.text.value.includes(requiredImport)
  ) {
    // wait until monaco has processed drop event
    const unwatch = watch(
      () => mainEditorState.value?.text.value,
      (newScript) => {
        unwatch();
        mainEditorState.value!.text.value = `${requiredImport}\n${newScript}`;
      },
    );
  }

  // clear selection
  delete inputOutputSelectionStore.selectedItem;
};

onMounted(async () => {
  await fetchInputOutputObjects("getInputObjects");
  await fetchFlowVariables();
  await fetchInputOutputObjects("getOutputObjects");
  emit("drop-event-handler-created", dropEventHandler);
});
</script>

<template>
  <div class="in-out-container">
    <InputOutputItem
      v-for="inputOutputItem in inputOutputItems"
      :key="inputOutputItem.name"
      :input-output-item="inputOutputItem"
    />
  </div>
</template>

<style scoped lang="postcss">
.in-out-container {
  display: flex;
  flex-direction: column;
  min-width: 150px;
}
</style>
