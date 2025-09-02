<script setup lang="ts">
import { type Ref, computed } from "vue";
import { computedAsync } from "@vueuse/core";

import CompactTabBar from "@/components/CompactTabBar.vue";
import useShouldFocusBePainted from "@/components/utils/shouldFocusBePainted";
import { getInitialDataService, getScriptingService } from "@/init";
import { type PortConfig, type PortConfigs } from "@/initial-data-service";

import InputPortTables from "./InputPortTables.vue";

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

const portConfigs: Ref<PortConfigs> = computedAsync(
  async () => {
    const inputPorts = getInitialDataService()
      .getInitialData()
      .inputPortConfigs.inputPorts.filter((port) => port.nodeId !== null);

    if (
      !(await getScriptingService().isCallKnimeUiApiAvailable(inputPorts[0]))
    ) {
      return {
        inputPorts: [],
      };
    }

    return {
      inputPorts,
    };
  },
  {
    inputPorts: [],
  },
);

const portConfigTabBarOpts: Ref<SlottedTab[]> = computed(() => {
  return portConfigs.value.inputPorts
    .slice()
    .reverse()
    .map((port, index) => ({
      slotName: makeNodePortIdFromPort(port),
      label: `${index}: ${port.portName}`,
    }));
});

const allPossibleTabvalues = computed(() => {
  return [...portConfigTabBarOpts.value, ...props.slottedTabs].map(
    (slottedTab: SlottedTab) => ({
      value: slottedTab.slotName,
      label: slottedTab.label,
    }),
  );
});
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
    <template v-for="port in portConfigs.inputPorts" :key="port.portName">
      <InputPortTables
        v-show="activeTab === makeNodePortIdFromPort(port)"
        :input-node-id="port.nodeId!"
        :port-idx="port.portIdx"
        :port-view-configs="port.portViewConfigs"
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
