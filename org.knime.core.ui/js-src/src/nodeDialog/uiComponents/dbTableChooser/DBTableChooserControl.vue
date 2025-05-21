<script setup lang="ts">
import { computed, ref } from "vue";
import { computedAsync } from "@vueuse/core";

import { Button } from "@knime/components";
import { SettingsSubPanel, type VueControlProps } from "@knime/jsonforms";

import FieldControl from "../FieldControl.vue";

import DBTableChooserFileExplorer from "./DBTableChooserFileExplorer.vue";
import type { DBTableSelection } from "./types";
import { useDbTableChooserBackend } from "./useDbTableChooserBackend";

const props = defineProps<VueControlProps<DBTableSelection>>();

const { supportsCatalogs } = useDbTableChooserBackend();

const areCatalogsSupported = computedAsync(supportsCatalogs, false);

const pathParts = computed<string[]>({
  get: () => {
    const output: string[] = [];
    if (areCatalogsSupported.value) {
      if (props.control.data.catalogName) {
        output.push(props.control.data.catalogName);
      } else {
        return [];
      }
    }
    if (props.control.data.schemaName) {
      output.push(props.control.data.schemaName);
      if (props.control.data.tableName) {
        output.push(props.control.data.tableName);
      }
    }

    return output;
  },
  set: (value: string[]) => {
    if (areCatalogsSupported.value) {
      props.changeValue({
        catalogName: value[0],
        schemaName: value[1],
        tableName: value[2],
      });
    } else {
      props.changeValue({
        catalogName: null,
        schemaName: value[0],
        tableName: value[1],
      });
    }
  },
});

const currentFolderPath = computed<string[]>(() => {
  const pathPossiblyIncludingTable = pathParts.value;

  const lengthIfTableIsIncluded = areCatalogsSupported.value ? 3 : 2;

  return pathPossiblyIncludingTable.slice(
    0,
    Math.min(lengthIfTableIsIncluded - 1, pathPossiblyIncludingTable.length),
  );
});

const settingsSubPanelRef = ref<typeof SettingsSubPanel | null>(null);

const onTableSelected = (pathParts: string[]) => {
  let newData: DBTableSelection = {
    tableName: pathParts.at(-1) ?? "",
    schemaName: pathParts.at(-2) ?? "",
    catalogName: areCatalogsSupported.value ? pathParts.at(-3) ?? "" : "",
  };
  props.changeValue(newData);
  settingsSubPanelRef.value?.close();
};
</script>

<template>
  <div class="flex-column">
    <SettingsSubPanel ref="settingsSubPanelRef">
      <template #expand-button="{ expand }">
        <Button primary variant="secondary" @click="expand">Expand</Button>
      </template>
      <template #default>
        <DBTableChooserFileExplorer
          :path="currentFolderPath"
          @table-selected="onTableSelected"
        />
      </template>
    </SettingsSubPanel>
    <FieldControl
      v-if="areCatalogsSupported"
      field-name="catalogName"
      :control
    />
    <FieldControl field-name="schemaName" :control />
    <FieldControl field-name="tableName" :control />
  </div>
</template>

<style lang="postcss" scoped>
.flex-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}
</style>
