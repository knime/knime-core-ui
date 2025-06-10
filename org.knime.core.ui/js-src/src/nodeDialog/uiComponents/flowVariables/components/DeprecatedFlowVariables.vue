<script setup lang="ts">
import { getFlowVariablesMap } from "../../../composables/components/useProvidedFlowVariablesMap";
import useDeprecatedConfigPaths from "../composables/useDeprecatedConfigPaths";

import UnsetDeprecatedFlowVariableButton from "./UnsetDeprecatedFlowVariableButton.vue";

const { deprecatedSetConfigPaths } = useDeprecatedConfigPaths();

const flowVariablesMap = getFlowVariablesMap();
const getFlowVariableName = (path: string) =>
  flowVariablesMap[path].controllingFlowVariableName ??
  flowVariablesMap[path].exposedFlowVariableName;
</script>

<template>
  <p>The following set flow variables are deprecated:</p>
  <div v-for="path in deprecatedSetConfigPaths" :key="path">
    <ul>
      <li>
        <p>
          "{{ path }}": <input disabled :value="getFlowVariableName(path)" />
        </p>
        <UnsetDeprecatedFlowVariableButton :path />
      </li>
    </ul>
  </div>
</template>

<style scoped lang="postcss">
p {
  margin: 10px 0;
  font-size: 13px;
}

li {
  font-size: 13px;
  overflow-wrap: break-word;
}
</style>
