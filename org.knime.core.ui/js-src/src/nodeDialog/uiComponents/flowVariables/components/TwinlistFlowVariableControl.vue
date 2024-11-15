<script setup lang="ts">
import { FlowVariableModel } from "@/nodeDialog/composables/components/useFlowVariableModel";
import { ConfigPath } from "@/nodeDialog/composables/components/useFlowVariables";
import { ValueSwitch } from "@knime/components";
import FlowVariableControl from "./FlowVariableControl.vue";
import { ref } from "vue";

const props = defineProps<{
  flowVariableModel: FlowVariableModel<"twinlist">;
  configPaths: ConfigPath[];
}>();

const { mode, updateMode } = props.flowVariableModel;

const modeValues = [
  { id: "manual", text: "Manual" },
  { id: "wildcard", text: "Wildcard" },
  { id: "regex", text: "Regex" },
  { id: "type", text: "Type" },
];

const excludeIncludeValues = [
  { id: "exclude", text: "Exclude" },
  { id: "include", text: "Include" },
];
const excludeInclude = ref("include");

const getConfigPath = (suffix: string) =>
  props.configPaths.find(({ configPath }) => configPath.endsWith(suffix))!;
</script>

<template>
  <div class="flex-gap-10">
    <ValueSwitch
      compact
      :possible-values="modeValues"
      :model-value="mode"
      @update:model-value="updateMode"
    />
    <template v-if="mode === 'manual'">
      <ValueSwitch
        v-model="excludeInclude"
        compact
        :possible-values="excludeIncludeValues"
      />
      <FlowVariableControl
        :config-name="
          excludeInclude === 'exclude' ? 'Excluded Columns' : 'Included Columns'
        "
        :config-path="
          getConfigPath(
            excludeInclude === 'exclude'
              ? 'manualFilter.manuallyDeselected'
              : 'manualFilter.manuallySelected',
          )
        "
        only-controlling
      />
    </template>
    <FlowVariableControl
      v-if="mode === 'regex' || mode === 'wildcard'"
      config-name="Pattern"
      :config-path="getConfigPath('patternFilter.pattern')"
      only-controlling
    />
    <FlowVariableControl
      v-if="mode === 'type'"
      config-name="Selected Types"
      :config-path="getConfigPath('typeFilter.selectedTypes')"
      only-controlling
    />
  </div>
</template>

<style lang="postcss" scoped>
.flex-gap-10 {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
