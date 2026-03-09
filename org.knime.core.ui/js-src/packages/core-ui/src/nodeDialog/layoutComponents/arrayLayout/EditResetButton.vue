<script lang="ts">
// (see the respective @InternalButtonReferenceId annotation in the backend)
export const ELEMENT_RESET_BUTTON_ID = "ElementResetButton";
</script>

<script setup lang="ts">
import { ref, watchEffect } from "vue";
import { rendererProps, useJsonFormsControl } from "@jsonforms/vue";

import { FunctionButton, LoadingIcon } from "@knime/components";
import EditIcon from "@knime/styles/img/icons/pencil.svg";
import ResetIcon from "@knime/styles/img/icons/reset-all.svg";

import inject from "../../utils/inject";

const props = defineProps({
  ...rendererProps(),
  initialIsEdited: {
    type: Boolean,
    default: false,
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
});

const trigger = inject("trigger");

const { control, handleChange } = useJsonFormsControl(props as any);

const isEditing = ref(false);

const onStartEditingClick = () => {
  isEditing.value = true;
};

const onResetClick = () => {
  isEditing.value = false;
  trigger({ id: ELEMENT_RESET_BUTTON_ID });
};

/**
 * sync isEditing with initial value per prop.
 */
watchEffect(() => {
  isEditing.value = props.initialIsEdited;
});

/**
 * sync data with isEditing
 */
watchEffect(() => {
  if (isEditing.value && !control.value.data) {
    handleChange(control.value.path, true);
  } else if (!isEditing.value && control.value.data) {
    handleChange(control.value.path, false);
  }
});
</script>

<template>
  <FunctionButton v-if="isLoading" disabled>
    <LoadingIcon class="loading-icon" />
  </FunctionButton>
  <template v-else>
    <FunctionButton v-if="!isEditing" title="Edit" @click="onStartEditingClick">
      <EditIcon />
    </FunctionButton>
    <FunctionButton v-else title="Reset to default" @click="onResetClick">
      <ResetIcon />
    </FunctionButton>
  </template>
</template>

<style lang="postcss" scoped>
.loading-icon {
  width: 14px;
  height: 14px;
}
</style>
