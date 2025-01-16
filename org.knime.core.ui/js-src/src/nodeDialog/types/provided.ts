import type { Provided as ProvidedByJsonFormsDialog } from "@knime/jsonforms";
import type { JsonDataService } from "@knime/ui-extension-service";

import type { PossibleFlowVariable } from "../api/types";
import type { Result } from "../api/types/Result";
import type { IdsRecord } from "../composables/nodeDialog/useArrayIds";
import type { IndexedIsActive } from "../composables/nodeDialog/useTriggers";
import type { getPossibleValuesFromUiSchema } from "../utils";

import type { Control } from "./Control";
import type { PersistSchema } from "./Persist";
import type { SettingsData } from "./SettingsData";

type getPossibleValuesFromUiSchema = (
  control: Control,
) => ReturnType<typeof getPossibleValuesFromUiSchema>;

type registerWatcher = (params: {
  transformSettings: (
    dependencyData: SettingsData,
  ) => Promise<(newData: SettingsData) => void> | void;
  init?: (newData: SettingsData) => Promise<void>;
  dependencies: string[];
}) => Promise<() => void>;

type getData = (
  params: Parameters<JsonDataService["data"]>[0] & object,
) => Promise<any>;

export interface ProvidedByNodeDialog {
  getPossibleValuesFromUiSchema: getPossibleValuesFromUiSchema;
  registerWatcher: registerWatcher;
  isTriggerActive: (triggerId: {
    id: string;
    indexIds?: string[];
  }) => Promise<Result<IndexedIsActive[]>>;
  updateData: (path: string) => void;
  getData: getData;
  setSubPanelExpanded: (param: { isExpanded: boolean }) => void;
  getPanelsContainer: () => null | HTMLElement;
  createArrayAtPath: (path: string) => IdsRecord;
  getDialogPopoverTeleportDest: () => null | HTMLElement;
}

type ProvidedFlowVariablesApi = {
  getAvailableFlowVariables: (
    persistPath: string,
  ) => Promise<Record<string, PossibleFlowVariable[]>>;
  getFlowVariableOverrideValue: (
    persistPath: string,
    dataPath: string,
  ) => Promise<any>;
  clearControllingFlowVariable: (persistPath: string) => void;
};

export type Provided = ProvidedByNodeDialog & ProvidedByJsonFormsDialog;

export interface ProvidedForFlowVariables {
  flowVariablesApi: ProvidedFlowVariablesApi;
  getPersistSchema: () => PersistSchema;
}
