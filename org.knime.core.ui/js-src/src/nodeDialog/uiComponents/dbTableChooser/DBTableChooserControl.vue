<script setup lang="ts">
import { computed, ref } from "vue";

import { Button, InlineMessage } from "@knime/components";
import { SettingsSubPanel, type VueControlProps } from "@knime/jsonforms";
import SelectTableIcon from "@knime/styles/img/icons/browse-db-table.svg";

import FieldControl from "../FieldControl.vue";
import { setUpApplyButton } from "../fileChooser/settingsSubPanel";
import { GO_INTO_FOLDER_INJECTION_KEY } from "../fileChooser/settingsSubPanel/SettingsSubPanelForFileChooser.vue";

import DBTableChooserFileExplorer from "./DBTableChooserFileExplorer.vue";
import ErrorMessageWithLoadingIcon from "./ErrorMessageWithLoadingIcon.vue";
import type { DBTableSelection } from "./types";
import { useDbTableChooserBackend } from "./useDbTableChooserBackend";
import useDbTableChooserErrorMessages from "./useDbTableChooserErrorMessages";

const props = defineProps<VueControlProps<DBTableSelection>>();

const { itemType } = useDbTableChooserBackend();

const areCatalogsSupported = computed<boolean>(
  () => props.control.uischema.options!.catalogsSupported,
);

const isDbConnected = computed<boolean>(
  () => props.control.uischema.options!.dbConnected,
);

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
    if (props.control.data.schemaName || props.control.data.tableName) {
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

const close = () => {
  settingsSubPanelRef.value?.close();
};

const {
  text: applyText,
  disabled: applyDisabled,
  element: applyButton,
  onApply,
} = setUpApplyButton();
applyText.value = "Choose table";

const {
  text: enterFolderButtonText,
  disabled: enterFolderButtonDisabled,
  element: goIntoFolderButton,
  onApply: onEnterFolderButtonClicked,
} = setUpApplyButton(GO_INTO_FOLDER_INJECTION_KEY);

const apply = () =>
  onApply
    .value?.()
    .then(close)
    .catch(() => {});

const catalogueName = computed(() => props.control.data.catalogName ?? "");
const schemaName = computed(() => props.control.data.schemaName);
const tableName = computed(() => props.control.data.tableName);

const {
  catalogueErrorMessage,
  schemaErrorMessage,
  tableErrorMessage,
  catalogueErrorMessageIsLoading,
  schemaErrorMessageIsLoading,
  tableErrorMessageIsLoading,
} = useDbTableChooserErrorMessages({
  itemTypeExtractor: itemType,
  cataloguesSupported: areCatalogsSupported,
  catalogueName,
  schemaName,
  tableName,
  isDbConnected,
});

const errorMessageDescription =
  "No database is currently connected. To proceed, connect a database to the input port, or re-execute any upstream connector nodes.";
</script>

<template>
  <div class="flex-column">
    <InlineMessage
      v-if="!isDbConnected"
      variant="info"
      title="Database not connected"
      :description="errorMessageDescription"
    />
    <SettingsSubPanel ref="settingsSubPanelRef" hide-buttons-when-expanded>
      <template #expand-button="{ expand }">
        <div class="expand-button-container">
          <Button
            primary
            compact
            :disabled="!isDbConnected"
            class="expand-button"
            @click="expand"
          >
            <span class="icon">
              <SelectTableIcon />
            </span>
            <span class="label">Browse table</span>
          </Button>
        </div>
      </template>
      <template #default>
        <DBTableChooserFileExplorer
          :initial-path-parts="currentFolderPath"
          :initial-table="tableName"
          @table-selected="onTableSelected"
        />
      </template>
      <template #bottom-content>
        <div class="bottom-buttons">
          <Button with-border compact @click="close"> Cancel </Button>
          <div>
            <Button
              ref="goIntoFolderButton"
              with-border
              compact
              :disabled="enterFolderButtonDisabled"
              @click="onEnterFolderButtonClicked"
            >
              {{ enterFolderButtonText }}
            </Button>
            <Button
              ref="applyButton"
              compact
              primary
              :disabled="applyDisabled"
              @click="apply"
            >
              {{ applyText }}
            </Button>
          </div>
        </div>
      </template>
    </SettingsSubPanel>
    <template v-if="areCatalogsSupported">
      <FieldControl field-name="catalogName" :control />
      <ErrorMessageWithLoadingIcon
        :error-message="catalogueErrorMessage"
        :is-loading="catalogueErrorMessageIsLoading"
      />
    </template>
    <FieldControl field-name="schemaName" :control />
    <ErrorMessageWithLoadingIcon
      :error-message="schemaErrorMessage"
      :is-loading="
        schemaErrorMessageIsLoading || catalogueErrorMessageIsLoading
      "
    />
    <FieldControl field-name="tableName" :control />
    <ErrorMessageWithLoadingIcon
      :error-message="tableErrorMessage"
      :is-loading="
        tableErrorMessageIsLoading ||
        schemaErrorMessageIsLoading ||
        catalogueErrorMessageIsLoading
      "
    />
  </div>
</template>

<style lang="postcss" scoped>
.flex-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.expand-button-container {
  width: 100%;

  & .expand-button .icon svg {
    width: 16px;
    height: 16px;
  }
}

.bottom-buttons {
  display: flex;
  justify-content: space-between;
  height: 60px;
  padding: 14px 20px;

  & > div {
    display: flex;
    gap: var(--space-8);
    flex-direction: row;
  }
}
</style>
