<script setup lang="ts">
import { computed, type Ref } from "vue";
import { computedAsync } from "@vueuse/core";
import CompactTabBar from "@/components/CompactTabBar.vue";
import OutputConsole, {
  type ConsoleHandler,
} from "@/components/OutputConsole.vue";
import { setConsoleHandler, consoleHandler } from "@/consoleHandler";
import useShouldFocusBePainted from "@/components/utils/shouldFocusBePainted";
import TrashIcon from "@knime/styles/img/icons/trash.svg";
import { FunctionButton } from "@knime/components";
import {
  initConsoleEventHandler,
  getScriptingService,
  type PortConfig,
  type PortConfigs,
} from "@/scripting-service";
import InputPortTables from "./InputPortTables.vue";

const paintFocus = useShouldFocusBePainted();

const activeTab = defineModel<string>({ default: "console" });

type SlottedTab = {
  label: string;
  value: string;
};
type PropsType = {
  slottedTabs: SlottedTab[];
};
const props = withDefaults(defineProps<PropsType>(), {
  slottedTabs: () => [] as SlottedTab[],
});

const onConsoleCreated = (console: ConsoleHandler) => {
  setConsoleHandler(console);
  initConsoleEventHandler();
};

const makeNodePortId = (nodeId: string, portIdx: number): string =>
  `${nodeId}-${portIdx}`;

const makeNodePortIdFromPort = (port: PortConfig): string =>
  makeNodePortId(port.nodeId!, port.portIdx);

const makeGrabFocusFunction = (tabValue: string) => {
  return () => {
    activeTab.value = tabValue;
  };
};

const portConfigs: Ref<PortConfigs> = computedAsync(
  async () => {
    const inputPorts = (
      await getScriptingService().getPortConfigs()
    ).inputPorts.filter((port) => port.nodeId !== null);

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
      value: makeNodePortIdFromPort(port),
      label: `${index}: ${port.portName}`,
    }));
});

const allTabs: Ref<SlottedTab[]> = computed(() => [
  { label: "Console", value: "console" },
  ...portConfigTabBarOpts.value,
  ...props.slottedTabs,
]);
</script>

<template>
  <div class="scripting-editor-bottom-pane">
    <span class="tab-bar-and-buttons">
      <CompactTabBar
        v-model="activeTab"
        class="scripting-editor-tab-bar"
        :possible-values="allTabs"
        :class="{ 'focus-painted': paintFocus }"
      />
      <span v-show="activeTab === 'console'" class="tab-bar-buttons">
        <slot name="console-status" />
        <FunctionButton class="clear-button" @click="consoleHandler.clear()">
          <TrashIcon />
        </FunctionButton>
      </span>
    </span>
    <div v-show="activeTab === 'console'" class="tab-content">
      <OutputConsole class="console" @console-created="onConsoleCreated" />
    </div>
    <template v-for="port in portConfigs.inputPorts" :key="port.portName">
      <InputPortTables
        v-show="activeTab === makeNodePortIdFromPort(port)"
        :input-node-id="port.nodeId!"
        :port-idx="port.portIdx"
        :port-view-configs="port.portViewConfigs"
      />
    </template>
    <template v-for="slot in props.slottedTabs" :key="slot.value">
      <div v-show="activeTab === slot.value" class="tab-content">
        <slot
          :name="slot.value"
          :grab-focus="makeGrabFocusFunction(slot.value)"
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
    height: 100%;
    flex-direction: column;
    position: relative;
    min-height: 0;
    padding: 0 var(--space-8);
  }

  & .tab-bar-and-buttons {
    display: flex;
    align-items: center;
    position: relative;
    padding: 0 var(--space-8);
    border-bottom: 1px solid var(--color-border);
    height: var(--space-48);
    flex-grow: 1;

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
