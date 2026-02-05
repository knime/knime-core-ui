<script setup lang="ts">
import { ref, nextTick } from "vue";
import { defaultRenderers, JsonFormsDialog } from "@knime/jsonforms";
import type { JsonSchema } from "@jsonforms/core";

const props = defineProps<{
  columnData: Record<string, any>;
  schema: JsonSchema;
  uischema: { elements: any[] };
  stateProviderListenerValue: any;
}>();

const emit = defineEmits<{
  "update:columnData": [data: Record<string, any>];
}>();

const root = ref<{ $el: HTMLElement } | null>(null);
let pendingFocus: { pending: true; initialValue?: string } | null = null;

const focusFirstInput = (initialValue?: string) => {
  const input = root.value?.$el.querySelector<HTMLInputElement>(
    "input, select, textarea",
  );
  if (input) {
    input.focus();
    if (initialValue !== undefined) {
      input.value = initialValue;
      input.dispatchEvent(new Event("input", { bubbles: true }));
    }
  } else {
    pendingFocus = { pending: true, initialValue };
  }
};

const onInitialized = async () => {
  if (pendingFocus) {
    const { initialValue } = pendingFocus;
    pendingFocus = null;
    await nextTick();
    focusFirstInput(initialValue);
  }
};

defineExpose({ focusFirstInput });

</script>

<template>
    <JsonFormsDialog
      ref="root"
      class="root"
      :data="columnData"
      :schema="schema"
      :uischema="uischema"
      :renderers="defaultRenderers"
      @change="({ data }) => emit('update:columnData', data)"
      @initialized="onInitialized"
      @state-provider-listener="
        (_id, callback) => callback(stateProviderListenerValue)
      "
    />
</template>

<style lang="postcss" scoped>
.root {
  height: 100%;
}
</style>
