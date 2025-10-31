<script setup lang="ts">
import { computed, ref, useTemplateRef, watch } from "vue";

import { Button, InlineMessage } from "@knime/components";
import { SettingsSubPanel, type VueControlProps } from "@knime/jsonforms";
import SelectTableIcon from "@knime/styles/img/icons/browse-db-table.svg";

import FieldControl from "../FieldControl.vue";
import { setUpApplyButton } from "../fileChooser/settingsSubPanel";
import { GO_INTO_FOLDER_INJECTION_KEY } from "../fileChooser/settingsSubPanel/SettingsSubPanelForFileChooser.vue";

import DBTableChooserFileExplorer from "./DBTableChooserFileExplorer.vue";
import MessageWithLoadingIcon from "./MessageWithLoadingIcon.vue";
import type { DBTableSelection } from "./types";
import { useDbTableChooserBackend } from "./useDbTableChooserBackend";
import useDbTableChooserErrorMessages from "./useDbTableChooserErrorMessages";

const props = defineProps<VueControlProps<DBTableSelection>>();

const { itemType } = useDbTableChooserBackend();

const options = computed(() => props.control.uischema.options!);
const areCatalogsSupported = computed<boolean>(
  () => options.value.catalogsSupported,
);
const dbConnectionError = computed<string | undefined>(
  () => options.value.dbConnectionError,
);
const isDbConnected = computed<boolean>(
  () => typeof dbConnectionError.value === "undefined",
);
const validateSchema = computed<boolean>(() => options.value.validateSchema);
const validateTable = computed<boolean>(() => options.value.validateTable);

const initialPathParts = computed<(string | null)[]>(() => {
  const output: (string | null)[] = [];
  if (areCatalogsSupported.value) {
    const catalogName = props.control.data.catalogName;
    if (catalogName) {
      output.push(catalogName);
    } else {
      return [];
    }
  }
  const schemaName = props.control.data.schemaName;
  if (schemaName) {
    output.push(schemaName);
  }
  return output;
});

const settingsSubPanelRef = ref<typeof SettingsSubPanel | null>(null);

const lastIndex = -1;
const secondToLastIndex = -2;
const thirdToLastIndex = -3;

const onTableSelected = (pathParts: (string | null)[]) => {
  let newData: DBTableSelection = {
    tableName: pathParts.at(lastIndex) ?? "",
    schemaName: pathParts.at(secondToLastIndex) ?? null,
    catalogName: areCatalogsSupported.value
      ? pathParts.at(thirdToLastIndex) ?? ""
      : "",
  };
  props.changeValue(newData);
  settingsSubPanelRef.value?.close();
};

const close = () => {
  settingsSubPanelRef.value?.close();
};

const applyButtonTemplateRef = "applyButton";
const applyButton = useTemplateRef<HTMLElement>(applyButtonTemplateRef);
const {
  text: applyText,
  disabled: applyDisabled,
  onApply,
} = setUpApplyButton({
  element: applyButton,
  initialValue: { text: "Choose table" },
});

const goIntoFolderButtonTemplateRef = "goIntoFolderButton";
const goIntoFolderButton = useTemplateRef<HTMLElement>(
  goIntoFolderButtonTemplateRef,
);
const {
  text: enterFolderButtonText,
  disabled: enterFolderButtonDisabled,
  onApply: onEnterFolderButtonClicked,
} = setUpApplyButton({
  key: GO_INTO_FOLDER_INJECTION_KEY,
  element: goIntoFolderButton,
  initialValue: { text: "Go into" },
});

const apply = () =>
  onApply
    .value?.()
    .then(close)
    .catch(() => {});

const data = computed(() => props.control.data);
const catalogueName = computed(() => data.value.catalogName ?? "");
const schemaName = computed<string | null>(() => data.value.schemaName);
const tableName = computed(() => data.value.tableName);

// we enable error messages only after the first user interaction
const enableErrorMessages = ref(false);
watch(data, () => (enableErrorMessages.value = schemaName.value !== null));

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
  validateSchema,
  validateTable,
  enable: enableErrorMessages,
});
</script>

<template>
  <div class="flex-column">
    <InlineMessage
      v-if="dbConnectionError"
      variant="info"
      title="Database not connected"
      :description="dbConnectionError"
    />

    <template v-if="areCatalogsSupported">
      <FieldControl field-name="catalogName" :control />
      <MessageWithLoadingIcon
        :error-message="catalogueErrorMessage"
        :is-loading="catalogueErrorMessageIsLoading"
      />
    </template>
    <FieldControl
      field-name="schemaName"
      :control
      :options="{
        hideOnNull: true,
        default: '',
      }"
    />
    <MessageWithLoadingIcon
      :error-message="schemaErrorMessage"
      :is-loading="schemaErrorMessageIsLoading"
    />
    <div class="flex-column">
      <SettingsSubPanel
        ref="settingsSubPanelRef"
        hide-buttons-when-expanded
        :background-color-override="'var(--knime-porcelain)'"
      >
        <template #expand-button="{ expand }">
          <div class="browse-button-flex-row">
            <FieldControl class="flex-grow" field-name="tableName" :control />
            <div class="flex-fixed expand-button-container">
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
                <span class="label">Browse</span>
              </Button>
            </div>
          </div>
        </template>
        <template #default>
          <DBTableChooserFileExplorer
            :initial-path-parts="initialPathParts"
            :initial-table="tableName"
            @table-selected="onTableSelected"
          />
        </template>
        <template #bottom-content>
          <div class="bottom-buttons">
            <Button with-border compact @click="close"> Cancel </Button>
            <div>
              <Button
                :ref="goIntoFolderButtonTemplateRef"
                with-border
                compact
                :disabled="enterFolderButtonDisabled"
                @click="onEnterFolderButtonClicked"
              >
                {{ enterFolderButtonText }}
              </Button>
              <Button
                :ref="applyButtonTemplateRef"
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
      <MessageWithLoadingIcon
        :error-message="
          schemaName === null
            ? validateTable
              ? 'Validation disabled when using default schema.'
              : null
            : tableErrorMessage
        "
        :is-loading="tableErrorMessageIsLoading && schemaName !== null"
        :type="schemaName === null ? 'INFO' : 'ERROR'"
      />
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.flex-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.browse-button-flex-row {
  display: flex;
  flex-direction: row;
  gap: var(--space-8);
  align-items: flex-end;

  & .flex-grow {
    flex: 1 1 0;
    min-width: 0;
  }

  & .flex-fixed {
    flex: 0 0 auto;
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
