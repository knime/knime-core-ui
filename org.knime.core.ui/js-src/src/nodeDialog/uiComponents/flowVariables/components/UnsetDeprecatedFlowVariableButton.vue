<script setup lang="ts">
import { Button } from "@knime/components";

import { getFlowVariablesMap } from "@/nodeDialog/composables/components/useProvidedFlowVariablesMap";
import useControllingFlowVariable from "../composables/useControllingFlowVariable";
import useExposedFlowVariable from "../composables/useExposedFlowVariable";

const props = defineProps<{ path: string }>();

const { controllingFlowVariableName, unsetControllingFlowVariable } =
  useControllingFlowVariable(props.path);

const { exposedFlowVariableName, setExposedFlowVariable } =
  useExposedFlowVariable(props.path);

const flowVariablesMap = getFlowVariablesMap();

const unsetDeprecatedPath = () => {
  if (controllingFlowVariableName.value) {
    unsetControllingFlowVariable(props);
  }
  if (exposedFlowVariableName.value) {
    setExposedFlowVariable({ path: props.path, flowVariableName: "" });
  }
  delete flowVariablesMap[props.path];
};
</script>

<template>
  <Button with-border compact @click="unsetDeprecatedPath"> Unset </Button>
</template>
