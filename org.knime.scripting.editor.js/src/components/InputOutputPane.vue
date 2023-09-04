<script setup lang="ts">
import { onMounted, ref, type Ref } from "vue";
import InputOutputItem, { type InputOutputModel } from "./InputOutputItem.vue";
import { getScriptingService } from "@/scripting-service";

const inputOutputItems: Ref<InputOutputModel[]> = ref([]);
const fetchInputOutputObjects = async (method: string) => {
  const items: InputOutputModel[] = await getScriptingService().sendToService(
    method,
  );
  if (items) {
    inputOutputItems.value.push(...items);
  }
};
const fetchFlowVariables = async () => {
  const item: InputOutputModel = await getScriptingService().sendToService(
    "getFlowVariableInputs",
  );
  if (item) {
    inputOutputItems.value.push(item);
  }
};

onMounted(async () => {
  await fetchInputOutputObjects("getInputObjects");
  await fetchInputOutputObjects("getOutputObjects");
  await fetchFlowVariables();
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
}
</style>
