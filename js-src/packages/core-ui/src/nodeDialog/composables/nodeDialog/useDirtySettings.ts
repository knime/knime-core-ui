import { type InjectionKey, inject, provide } from "vue";

import {
  DialogService,
  type SettingComparator,
  type SettingState,
} from "@knime/ui-extension-service";

import type { FlowSettings } from "../../api/types";

type ControllingVariable = ReturnType<
  SettingState["addControllingFlowVariable"]
>;
type ExposedVariable = ReturnType<SettingState["addExposedFlowVariable"]>;
type SetValue<T> = SettingState<T>["setValue"];

interface FlowVariables<T> {
  get: (persistPath: string) => T | null;
  create: (persistPath: string, flowVarName: string | null) => T;
}

const toFlowVariablesForSettings = <T>(
  createNew: (flowVarName: string | null) => T,
): FlowVariables<T> => {
  const variables: Map<string, T> = new Map();
  return {
    get: (persistPath) => variables.get(persistPath) ?? null,
    create: (persistPath, flowVarName) => {
      const newVar = createNew(flowVarName);
      variables.set(persistPath, newVar);
      return newVar;
    },
  };
};

export interface FlowVariablesForSettings {
  controlling: FlowVariables<ControllingVariable>;
  exposed: FlowVariables<ExposedVariable>;
}

export const settingStateToFlowVariablesForSettings = <T>({
  settingState,
  persistPaths,
  flowVariablesMap,
}: {
  settingState: SettingState<T>;
  persistPaths: string[];
  flowVariablesMap: Record<string, FlowSettings>;
}): FlowVariablesForSettings => {
  const flowVariablesForSettings = {
    controlling: toFlowVariablesForSettings(
      settingState.addControllingFlowVariable,
    ),
    exposed: toFlowVariablesForSettings(settingState.addExposedFlowVariable),
  };
  persistPaths?.forEach((persistPath) => {
    const {
      controllingFlowVariableName = null,
      exposedFlowVariableName = null,
    } = flowVariablesMap[persistPath] ?? {};
    flowVariablesForSettings.controlling.create(
      persistPath,
      controllingFlowVariableName,
    );
    flowVariablesForSettings.exposed.create(
      persistPath,
      exposedFlowVariableName,
    );
  });
  return flowVariablesForSettings;
};

/**
 * Exported for testing only
 */
export const injectionKey: InjectionKey<{
  getSettingState: <T>(dataPath: string) => SetValue<T> | null;
  constructSettingState: <T>(
    dataPath: string,
    params: {
      initialValue: T;
      valueComparator: SettingComparator<T>;
    },
  ) => SetValue<T>;
  getFlowVariableDirtyState: (
    dataPath: string,
  ) => FlowVariablesForSettings | null;
  constructFlowVariableDirtyState: (
    dataPath: string,
    configPaths: string[],
    flowVariablesMap: Record<string, FlowSettings>,
  ) => FlowVariablesForSettings;
  registerNonControlState: typeof DialogService.prototype.registerSettings;
}> = Symbol("providedByUseDirtySettings");

export const getModelOrView = (persistPath: string) => {
  const firstPathSegment = persistPath.split(".")[0] as
    | "model"
    | "view"
    | "job-manager";
  return firstPathSegment === "job-manager" ? "model" : firstPathSegment;
};
export const provideAndGetSetupMethodForDirtySettings = () => {
  /**
   * Maps data path to setting
   */
  const settings: Map<string, SettingState> = new Map();

  let _registerSettings: typeof DialogService.prototype.registerSettings;

  const getSettingState = <T>(dataPath: string): SetValue<T> | null => {
    return settings.get(dataPath)?.setValue ?? null;
  };

  const flowVariableStatesForSettings: Map<string, FlowVariablesForSettings> =
    new Map();

  const constructFlowVariableDirtyState = (
    dataPath: string,
    persistPaths: string[],
    flowVariablesMap: Record<string, FlowSettings>,
  ): FlowVariablesForSettings => {
    const settingState = settings.get(dataPath);
    if (!settingState) {
      throw new Error(`Setting state for ${dataPath} not found`);
    }
    const flowVariablesForSettings = settingStateToFlowVariablesForSettings({
      settingState,
      persistPaths,
      flowVariablesMap,
    });
    flowVariableStatesForSettings.set(dataPath, flowVariablesForSettings);
    return flowVariablesForSettings;
  };

  const getFlowVariableDirtyState = (
    dataPath: string,
  ): FlowVariablesForSettings | null =>
    flowVariableStatesForSettings.get(dataPath) ?? null;

  const constructSettingState = <T>(
    dataPath: string,
    params: {
      initialValue: T;
      valueComparator: SettingComparator<T>;
    },
  ): SetValue<T> => {
    const modelOrView = getModelOrView(dataPath);
    const newSetting = _registerSettings(modelOrView)(params);
    settings.set(dataPath, newSetting as SettingState<unknown>);
    return newSetting.setValue;
  };

  const setRegisterSettingsMethod = (
    registerSettings: typeof DialogService.prototype.registerSettings,
  ) => {
    _registerSettings = registerSettings;
  };

  provide(injectionKey, {
    constructSettingState,
    getSettingState,
    getFlowVariableDirtyState,
    constructFlowVariableDirtyState,
    registerNonControlState: (modelOrView: "model" | "view") =>
      _registerSettings(modelOrView),
  });

  return { setRegisterSettingsMethod };
};

export default () => inject(injectionKey)!;
