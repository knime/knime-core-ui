<script setup lang="ts">
import { toRefs } from "vue";

import { useTriggersReexecution } from "../../composables/components/useDialogControl";
import type { Control } from "../../types/Control";
import DialogComponentWrapper from "../DialogComponentWrapper.vue";

import DialogLabel from "./DialogLabel.vue";

const props = withDefaults(
  defineProps<{
    control: Control;
    marginBottom?: number;
    show?: boolean;
    fill?: boolean;
  }>(),
  {
    marginBottom: 0,
    show: true,
    fill: false,
  },
);
const { control } = toRefs(props);

const showControlHeader =
  (control.value.uischema.options?.hideControlHeader ?? false) === false;

// The show prop is used by calling UI component to always hide
// the control header (hardcoded behavior). The hideControlHeader option
// from the backend allows dynamic control over whether the control header
// is hidden. These two inputs are combined to determine
// the final visibility of the control header.
const showTitle = showControlHeader && props.show;

// An empty string will change the layout. In order to keep the layout we add
// a space here that will lead to the correct height of the control header.
// Hiding the whole header can be done by setting the hideControlHeader option
const title = control.value.label === "" ? " " : control.value.label;

defineEmits<{
  controllingFlowVariableSet: [value: any];
}>();

const triggersReexecution = useTriggersReexecution(control);
</script>

<template>
  <DialogComponentWrapper
    :control="control"
    :class="{ fill }"
    :style="{ marginBottom: `${marginBottom}px` }"
  >
    <DialogLabel
      :class="{ fill }"
      :title="title"
      :show-reexecution-icon="triggersReexecution"
      :description="control.description"
      :errors="[control.errors]"
      :show="showTitle"
      @controlling-flow-variable-set="
        (event) => $emit('controllingFlowVariableSet', event)
      "
    >
      <template #default="{ labelForId }">
        <slot :label-for-id="labelForId" />
      </template>
      <template #before-label>
        <slot name="before-label" />
      </template>
    </DialogLabel>
  </DialogComponentWrapper>
</template>

<style scoped>
.fill {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}
</style>
