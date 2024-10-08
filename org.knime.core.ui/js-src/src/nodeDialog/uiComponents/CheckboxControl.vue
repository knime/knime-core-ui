<script setup lang="ts">
import { ref } from "vue";
import { Checkbox } from "@knime/components";
import ReexecutionIcon from "@knime/styles/img/icons/reexecution.svg";
import FlowVariableButton from "./flowVariables/components/FlowVariableButton.vue";
import ErrorMessage from "./ErrorMessage.vue";
import DescriptionPopover from "./description/DescriptionPopover.vue";
import DialogComponentWrapper from "./DialogComponentWrapper.vue";
import useDialogControl, {
  useTriggersReexecution,
} from "../composables/components/useDialogControl";
import { rendererProps } from "@jsonforms/vue";
const props = defineProps(rendererProps());
const { control, onChange, disabled } = useDialogControl({ props });
const hover = ref(false);
const triggersReexecution = useTriggersReexecution(control);
</script>

<template>
  <DialogComponentWrapper :control="control">
    <div
      class="checkbox-input"
      @mouseover="hover = true"
      @mouseleave="hover = false"
    >
      <Checkbox
        class="checkbox"
        :disabled="disabled"
        :model-value="control.data"
        @update:model-value="onChange"
      >
        {{ control.label }}
        <ReexecutionIcon v-if="triggersReexecution" class="reexecution-icon" />
      </Checkbox>
      <FlowVariableButton
        :hover="hover"
        @controlling-flow-variable-set="onChange"
      />
      <DescriptionPopover
        v-if="control.description"
        :html="control.description"
        :hover="hover"
      />
      <ErrorMessage :error="control.errors" />
    </div>
  </DialogComponentWrapper>
</template>

<style lang="postcss" scoped>
.checkbox-input {
  margin-bottom: -10px;

  /**
   * This is necessary to fixate the dialog popovers
  */
  position: relative;
  display: flex;

  & .checkbox {
    min-width: 0;
    flex: 1;
  }
}

.reexecution-icon {
  display: inline-block;
  vertical-align: top;
  height: 10px;
  margin: 3px 0 1px 5px;
}
</style>
