<script setup lang="ts">
import { computed, watch } from "vue";
import SideDrawerContent from "./SideDrawerContent.vue";
import useDialogControl from "../../../composables/components/useDialogControl";
import LabeledInput from "../../label/LabeledInput.vue";
import SettingsSubPanel from "@/nodeDialog/layoutComponents/settingsSubPanel/SettingsSubPanel.vue";
import { rendererProps } from "@jsonforms/vue";
import { FileChooserUiSchemaOptions } from "@/nodeDialog/types/FileChooserUiSchema";
const props = defineProps(rendererProps());
const {
  control,
  onChange: onChangeControl,
  disabled,
  flowSettings,
} = useDialogControl({
  subConfigKeys: ["path"],
  props,
});

const getDefaultData = () => {
  return {
    path: "",
    timeout: 10000,
    fsCategory: "LOCAL",
  };
};

const data = computed(() => {
  return control.value.data?.path ?? getDefaultData();
});

const onChange = (value: any) => {
  onChangeControl({ path: value });
};

const browseOptions = computed(() => {
  return control.value.uischema.options as FileChooserUiSchemaOptions;
});

watch(
  () => Boolean(flowSettings.value?.controllingFlowVariableName),
  (value) => {
    if (!value) {
      onChange(getDefaultData());
    }
  },
);
</script>

<template>
  <LabeledInput
    #default="{ labelForId }"
    :control="control"
    @controlling-flow-variable-set="onChange"
  >
    <SettingsSubPanel>
      <template #expand-button="{ expand }">
        <button @click="expand">click me</button>
      </template>
      <template #default>
        <SideDrawerContent
          :id="labelForId"
          :disabled="disabled"
          :model-value="data"
          :browse-options="browseOptions"
          @update:model-value="onChange"
        />
      </template>
    </SettingsSubPanel>
  </LabeledInput>
</template>
