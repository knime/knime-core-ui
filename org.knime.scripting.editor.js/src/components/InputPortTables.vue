<script setup lang="ts">
import { computed, ref } from "vue";
import type { PortViewConfig } from "@/scripting-service";
import { ValueSwitch } from "@knime/components";
import InputPortTableView from "@/components/InputPortTableView.vue";

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
    <div class="view-value-switch">
      <ValueSwitch
        v-model="selectedView"
        compact
        :possible-values="valueSwitchPossibleValues"
      />
    </div>
    <InputPortTableView
      v-if="selectedViewIndex"
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
