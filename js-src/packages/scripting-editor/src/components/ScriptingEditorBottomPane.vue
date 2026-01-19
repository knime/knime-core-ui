<script setup lang="ts">
import { type Ref, computed, watch } from "vue";
import { computedAsync } from "@vueuse/core";

import { getInitialData, getScriptingService } from "../init";
import { type PortConfig } from "../initial-data-service";

import CompactTabBar from "./CompactTabBar.vue";
import InputPortTables from "./InputPortTables.vue";
import useShouldFocusBePainted from "./utils/shouldFocusBePainted";

const paintFocus = useShouldFocusBePainted();

export type BottomPaneTabSlotName = `bottomPaneTabSlot:${string}`;
export type BottomPaneTabControlsSlotName =
  `bottomPaneTabControlsSlot:${string}`;
export type SlottedTab = {
  label: string;
  slotName: BottomPaneTabSlotName;
  associatedControlsSlotName?: BottomPaneTabControlsSlotName;
};
type PropsType = {
  slottedTabs?: SlottedTab[];
};
const props = withDefaults(defineProps<PropsType>(), {
  slottedTabs: () => [] as SlottedTab[],
});

const makeNodePortId = (
  nodeId: string,
  portIdx: number,
): BottomPaneTabSlotName => `bottomPaneTabSlot:${nodeId}-${portIdx}`;

const makeNodePortIdFromPort = (port: PortConfig): BottomPaneTabSlotName =>
  makeNodePortId(port.nodeId!, port.portIdx);

const activeTab = defineModel<string>();

const makeGrabFocusFunction = (tabValue: string) => {
  return () => {
    activeTab.value = tabValue;
  };
};

type InputPortTabInfo = { port: PortConfig; tab: SlottedTab };
const inputPortTabInfo: Ref<InputPortTabInfo[]> = computedAsync(async () => {
  const inputPorts = getInitialData().inputPortConfigs.inputPorts;

  // Check if there is at least one input port and the Call KNIME UI API is available
  const existingPort = inputPorts.find((port) => port.nodeId);
  if (
    !existingPort ||
    !(await getScriptingService().isCallKnimeUiApiAvailable(existingPort))
  ) {
    return [];
  }

  const info: InputPortTabInfo[] = [];

  // NB: Start from index 1 to skip the variable port at index 0
  for (let i = 1; i < inputPorts.length; i++) {
    const port = inputPorts[i];
    // Skip ports without nodeId
    if (port.nodeId) {
      info.push({
        port,
        tab: {
          slotName: makeNodePortIdFromPort(port),
          label: `${i}: ${port.portName}`,
        },
      });
    }
  }

  // Add the variable port at the end if it has a nodeId
  const variablePort = inputPorts[0];
  if (variablePort.nodeId) {
    info.push({
      port: variablePort,
      tab: {
        slotName: makeNodePortIdFromPort(variablePort),
        label: `${variablePort.portName}`,
      },
    });
  }

  return info;
}, []);

const allPossibleTabvalues = computed(() => {
  return [
    ...inputPortTabInfo.value.map((info) => info.tab),
    ...props.slottedTabs,
  ].map((slottedTab: SlottedTab) => ({
    value: slottedTab.slotName,
    label: slottedTab.label,
  }));
});

// Select the first tab if tabs become available and no tab is active yet
watch(allPossibleTabvalues, (newVal) => {
  if (newVal.length > 0 && !activeTab.value) {
    activeTab.value = newVal[0].value;
  }
});

// Expose whether there are currently any tabs to the parent component
const hasTabs = computed(() => allPossibleTabvalues.value.length > 0);
defineExpose({ hasTabs });
</script>

<template>
  <div class="scripting-editor-bottom-pane">
    <span class="tab-bar-and-buttons">
      <CompactTabBar
        v-model="activeTab"
        class="scripting-editor-tab-bar"
        :possible-values="allPossibleTabvalues"
        :class="{ 'focus-painted': paintFocus }"
      />
      <span class="tab-bar-buttons">
        <slot name="status-label" />
        <template v-for="slot in props.slottedTabs" :key="slot.value">
          <div v-show="activeTab === slot.slotName">
            <slot :name="slot.associatedControlsSlotName" />
          </div>
        </template>
      </span>
    </span>
    <template v-for="info in inputPortTabInfo" :key="info.tab.slotName">
      <InputPortTables
        v-show="activeTab === info.tab.slotName"
        :input-node-id="info.port.nodeId!"
        :port-idx="info.port.portIdx"
        :port-view-configs="info.port.portViewConfigs"
      />
    </template>
    <template v-for="slot in props.slottedTabs" :key="slot.value">
      <div v-show="activeTab === slot.slotName" class="tab-content">
        <slot
          :name="slot.slotName"
          :grab-focus="makeGrabFocusFunction(slot.slotName)"
        />
      </div>
    </template>
  </div>
</template>

<style scoped lang="postcss">
.scripting-editor-bottom-pane {
  display: flex;
  flex-direction: column;
  height: 100%;

  & .tab-content {
    display: flex;
    flex: 1;
    flex-direction: column;
    position: relative;
    min-height: 0;
    padding: 0 var(--space-8);
    overflow: hidden;
  }

  & .tab-bar-and-buttons {
    display: flex;
    align-items: center;
    position: relative;
    padding: 0 var(--space-8);
    border-bottom: 1px solid var(--color-border);
    height: var(--space-48);
    flex-shrink: 0;

    & .tab-bar-buttons {
      position: absolute;
      right: var(--space-16);
      top: var(--space-4);
      display: flex;
      flex-wrap: nowrap;
    }

    & .scripting-editor-tab-bar {
      flex-grow: 1;
    }
  }
}
</style>
