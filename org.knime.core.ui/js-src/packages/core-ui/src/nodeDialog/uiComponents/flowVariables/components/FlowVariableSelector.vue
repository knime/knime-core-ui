<script setup lang="ts">
import { type Ref, computed, onMounted, ref } from "vue";

import { Dropdown } from "@knime/components";
import { KdsDataType } from "@knime/kds-components";

import type { PossibleFlowVariable } from "../../../api/types";
import { injectForFlowVariables } from "../../../utils/inject";
import useControllingFlowVariable from "../composables/useControllingFlowVariable";
import type { FlowVariableSelectorProps } from "../types/FlowVariableSelectorProps";

const props = defineProps<FlowVariableSelectorProps>();
const {
  setControllingFlowVariable,
  unsetControllingFlowVariable,
  controllingFlowVariableName,
  invalidateSetFlowVariable,
} = useControllingFlowVariable(props.persistPath);
const {
  getAvailableFlowVariables,
  getFlowVariableOverrideValue,
  clearControllingFlowVariable,
} = injectForFlowVariables("flowVariablesApi")!;

const dropdownPossibleValues: Ref<
  {
    id: string | number;
    text: string;
    title: string;
    slotData: { typeId?: string; typeText?: string; text?: string };
  }[]
> = ref([]);
const nameToFlowVariable: Ref<Record<string, PossibleFlowVariable>> = ref({});

const noFlowVariableOption = {
  id: "",
  text: "None",
  title: "No flow variable selected",
  slotData: { text: "None" },
};

const toDropdownValues = (allPossibleValues: PossibleFlowVariable[]) => [
  noFlowVariableOption,
  ...allPossibleValues.map((flowVar) => ({
    id: flowVar.name,
    text: flowVar.name,
    title: `${flowVar.name} (currently "${flowVar.value}")`,
    slotData: {
      typeId: flowVar.type.id,
      typeText: flowVar.type.text,
      text: flowVar.name,
    },
  })),
];

const fetchAllPossibleValues = async (path: string) => {
  const possibleValuesByType = await getAvailableFlowVariables(path);
  return Object.values(possibleValuesByType).flat();
};

let availableVariablesLoaded = ref(false);

onMounted(async () => {
  const allPossibleValues = await fetchAllPossibleValues(props.persistPath);
  nameToFlowVariable.value = allPossibleValues.reduce(
    (lookupMap, flowVar) => {
      lookupMap[flowVar.name] = flowVar;
      return lookupMap;
    },
    {} as Record<string, PossibleFlowVariable>,
  );
  dropdownPossibleValues.value = toDropdownValues(allPossibleValues);
  availableVariablesLoaded.value = true;
});

const emit = defineEmits<{
  controllingFlowVariableSet: [
    path: string,
    value: unknown,
    flowVarName: string,
  ];
}>();

const selectValue = async (selectedId: string | number) => {
  if (selectedId === noFlowVariableOption.id) {
    unsetControllingFlowVariable({ path: props.persistPath });
    clearControllingFlowVariable(props.persistPath);
    return;
  }
  const flowVar = nameToFlowVariable.value[selectedId];
  const setVariableProps = {
    path: props.persistPath,
    flowVariableName: flowVar.name,
  };
  setControllingFlowVariable(setVariableProps);
  const value = await getFlowVariableOverrideValue(
    props.persistPath,
    props.dataPath,
  );
  const isFlawed = typeof value === "undefined";
  if (isFlawed) {
    invalidateSetFlowVariable(setVariableProps);
  } else {
    emit("controllingFlowVariableSet", props.dataPath, value, flowVar.name);
  }
};

const ariaLabel = computed(
  () => `controlling-flow-variables-${props.persistPath}`,
);
const noOptionsPresent = computed(
  () =>
    Boolean(dropdownPossibleValues.value.length === 1) &&
    !controllingFlowVariableName.value,
);
const placeholder = computed(() => {
  if (!availableVariablesLoaded.value) {
    return "Fetching available flow variables...";
  }
  return noOptionsPresent.value
    ? "No suitable flow variable present"
    : "No flow variable selected";
});
</script>

<!-- eslint-disable vue/attribute-hyphenation typescript complains with ':aria-label' instead of ':ariaLabel'-->
<template>
  <Dropdown
    :ariaLabel="ariaLabel"
    :possible-values="dropdownPossibleValues"
    :model-value="availableVariablesLoaded ? controllingFlowVariableName : ''"
    :placeholder="placeholder"
    :disabled="!availableVariablesLoaded || noOptionsPresent"
    compact
    @update:model-value="selectValue"
    ><template
      #option="{ slotData, selectedValue, isMissing, expanded } = {
        slotData: {},
      }"
    >
      <template v-if="expanded || selectedValue !== '' || isMissing">
        <div
          :class="[
            'data-type-entry',
            { 'with-type': isMissing || slotData.typeId },
          ]"
        >
          <template v-if="isMissing">
            <KdsDataType size="small" />
            <span>(MISSING) {{ selectedValue }}</span>
          </template>
          <template v-else>
            <template v-if="slotData.typeId">
              <KdsDataType
                :icon-name="slotData.typeId"
                :icon-title="slotData.typeText"
                size="small"
              />
            </template>
            <span>{{ slotData.text }}</span>
          </template>
        </div>
      </template>
      <template v-else>{{ placeholder }}</template>
    </template></Dropdown
  >
</template>

<style lang="css" scoped>
.data-type-entry.with-type {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}
</style>
