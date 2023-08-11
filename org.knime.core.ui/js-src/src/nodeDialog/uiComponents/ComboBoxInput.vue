<script lang="ts">
import { defineComponent, type ComponentOptions } from "vue";
import { rendererProps } from "@jsonforms/vue";
import { getFlowVariablesMap, isModelSettingAndHasNodeView } from "../utils";
import ComboBox from "webapps-common/ui/components/forms/ComboBox.vue";
import LabeledInput from "./LabeledInput.vue";
import DialogComponentWrapper from "./DialogComponentWrapper.vue";
import { useJsonFormsControlWithUpdate } from "./composables/jsonFormsControlWithUpdate";

const ComboBoxInput = defineComponent({
  name: "ComboBoxInput",
  components: {
    ComboBox,
    LabeledInput,
    DialogComponentWrapper,
  } as ComponentOptions,
  props: {
    ...rendererProps(),
  },
  setup(props) {
    // @ts-ignore
    return useJsonFormsControlWithUpdate(props);
  },
  data(): {
    options: string[] | undefined;
    initialSelectedIds: string[];
    loaded: boolean;
  } {
    return {
      options: [],
      initialSelectedIds: [],
      loaded: false,
    };
  },
  computed: {
    isModelSettingAndHasNodeView() {
      return isModelSettingAndHasNodeView(this.control);
    },
    flowSettings() {
      return getFlowVariablesMap(this.control);
    },
    disabled() {
      return (
        !this.control.enabled ||
        this.flowSettings?.controllingFlowVariableAvailable ||
        typeof this.options === "undefined" ||
        this.options.length === 0
      );
    },
    noPossibleValuesPresent() {
      return typeof this.options === "undefined";
    },
  },
  mounted() {
    this.initialSelectedIds = this.control.data;
    this.options = this.control.uischema?.options?.possibleValues;
    this.loaded = true;
  },
  methods: {
    onChange(value: string[]) {
      this.handleChange(this.control.path, value);
      if (this.isModelSettingAndHasNodeView) {
        // @ts-ignore
        this.$store.dispatch("pagebuilder/dialog/dirtySettings", true);
      }
    },
  },
});
export default ComboBoxInput;
</script>

<template>
  <DialogComponentWrapper :control="control" style="min-width: 0">
    <LabeledInput
      :text="control.label"
      :show-reexecution-icon="isModelSettingAndHasNodeView"
      :scope="control.uischema.scope"
      :flow-settings="flowSettings"
      :description="control.description"
    >
      <!--
        TODO Enable unsing :allow-new-values="noPossibleValuesPresent"
        (see https://github.com/vuejs/vue/issues/2169)
      -->
      <ComboBox
        v-if="loaded"
        :allow-new-values="noPossibleValuesPresent ? '' : false"
        :aria-label="control.label"
        :disabled="disabled"
        :possible-values="noPossibleValuesPresent ? [] : options"
        :initial-selected-ids="initialSelectedIds"
        @update:selected-ids="onChange"
      />
    </LabeledInput>
  </DialogComponentWrapper>
</template>

<style scoped>
:deep(.multiselect) {
  background-color: white;
}
</style>