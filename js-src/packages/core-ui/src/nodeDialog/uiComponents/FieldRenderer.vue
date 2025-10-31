<script setup lang="ts" generic="T">
import { computed } from "vue";
import type { UISchemaElement } from "@jsonforms/core";
import { DispatchRenderer } from "@jsonforms/vue";

import { type VueControlProps } from "@knime/jsonforms";

import { composePaths } from "@/nodeDialog/utils/paths";

const props = defineProps<{
  control: VueControlProps<T>["control"];
  fieldName: keyof T & string;
  uischema: UISchemaElement & { scope?: `#${string}` };
}>();

const path = computed(() => composePaths(props.control.path, props.fieldName));
const schema = computed(
  () => props.control.schema.properties?.[props.fieldName]!,
);
</script>

<template>
  <DispatchRenderer
    :key="path"
    :schema
    :uischema
    :path
    :enabled="control.enabled"
    :renderers="control.renderers"
    :cells="control.cells"
  />
</template>
