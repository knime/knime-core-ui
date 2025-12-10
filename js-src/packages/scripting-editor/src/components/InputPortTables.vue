<script setup lang="ts">
import { computed, ref } from "vue";

import { ValueSwitch } from "@knime/components";

import { type PortViewConfig } from "../initial-data-service";

import InputPortTableView from "./InputPortTableView.vue";

interface Props {
  inputNodeId: string;
  portIdx: number;
  portViewConfigs: PortViewConfig[];
}

const props = withDefaults(defineProps<Props>(), {});

const valueSwitchPossibleValues = computed(() =>
  props.portViewConfigs.map((view) => ({
    id: view.label,
    text: view.label,
  })),
);
const selectedView = ref<string>(props.portViewConfigs[0].label);
const selectedViewIndex = computed(
  () =>
    props.portViewConfigs.find((view) => view.label === selectedView.value)
      ?.portViewIdx,
);
</script>

<template>
  <div style="height: 100%; width: 100%">
    <div v-if="props.portViewConfigs.length > 1" class="view-value-switch">
      <!-- NB: The name is used as the radio-button name attribute and must
           be different to the one used by other radio button groups -->
      <ValueSwitch
        v-model="selectedView"
        :name="`port-view-config-selector-${props.portIdx}`"
        compact
        :possible-values="valueSwitchPossibleValues"
      />
    </div>
    <InputPortTableView
      v-if="typeof selectedViewIndex !== 'undefined'"
      :input-node-id="props.inputNodeId"
      :port-idx="props.portIdx"
      :view-idx="selectedViewIndex"
    />
  </div>
</template>

<style scoped>
.view-value-switch {
  display: flex;
  justify-content: center;
}
</style>
