<script setup lang="ts">
import { computed, ref } from "vue";
import { computedAsync } from "@vueuse/core";

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

const props = defineProps<VueControlProps<DBTableSelection>>();

const { supportsCatalogs, isDbConnected, itemType } =
  useDbTableChooserBackend();

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

const browseButtonEnabled = computedAsync(isDbConnected, false);

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
const catalogueErrorMessageIsLoading = ref(false);
const catalogueErrorMessage = computedAsync(
  async () => {
    if (!areCatalogsSupported.value) {
      return null;
    }

    const pathUpToCatalogue = [catalogueName.value ?? ""];

    if (!catalogueName.value) {
      return "Please select a database.";
    } else if ((await itemType(pathUpToCatalogue)) === null) {
      return `Database "${catalogueName.value}" does not exist.`;
    } else {
      return null;
    }
  },
  null,
  catalogueErrorMessageIsLoading,
);

const schemaName = computed(() => props.control.data.schemaName);
const schemaErrorMessageIsLoading = ref(false);
const schemaErrorMessage = computedAsync(
  async () => {
    const pathUpToSchema = [
      ...(areCatalogsSupported.value ? [catalogueName.value] : []),
      schemaName.value ?? "",
    ];

    // schema can be blank, so we only check if it exists
    if (catalogueErrorMessage.value) {
      return "There are errors in the database selection.";
    } else if ((await itemType(pathUpToSchema)) === null) {
      return `Schema "${schemaName.value}" does not exist in database.`;
    } else {
      return null;
    }
  },
  null,
  schemaErrorMessageIsLoading,
);

const tableName = computed(() => props.control.data.tableName);
const tableErrorMessageIsLoading = ref(false);
const tableErrorMessage = computedAsync(
  async () => {
    const pathUpToTable = [
      ...(areCatalogsSupported.value ? [catalogueName.value ?? ""] : []),
      schemaName.value ?? "",
      tableName.value ?? "",
    ];

    if (schemaErrorMessage.value) {
      return "There are errors in the schema selection.";
    } else if ((await itemType(pathUpToTable)) === null) {
      return `Table "${tableName.value}" does not exist in schema.`;
    } else {
      return null;
    }
  },
  null,
  tableErrorMessageIsLoading,
);

const errorMessageDescription =
  "No database is currently connected. To proceed, connect a database to the input port.";
</script>

<template>
  <div class="flex-column">
    <InlineMessage
      v-if="!browseButtonEnabled"
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
            :disabled="!browseButtonEnabled"
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
          :path="currentFolderPath"
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
    <FieldControl
      v-if="areCatalogsSupported"
      field-name="catalogName"
      :control
    />
    <ErrorMessageWithLoadingIcon
      v-if="areCatalogsSupported"
      :error-message="catalogueErrorMessage"
      :is-loading="catalogueErrorMessageIsLoading"
    />
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
