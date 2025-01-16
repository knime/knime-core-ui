import { type InjectionKey, type Ref, computed, inject } from "vue";

import type { FlowSettings } from "../../api/types";
import { injectForFlowVariables } from "../../utils/inject";
import { getConfigPaths } from "../../utils/paths";
import type { FlowVariablesForSettings } from "../nodeDialog/useDirtySettings";

import { getFlowVariablesMap } from "./useProvidedFlowVariablesMap";

export interface ConfigPath {
  configPath: string;
  dataPath: string;
  deprecatedConfigPaths: string[];
}

export interface FlowVariableSettingsProvidedByControl {
  flowSettings: Ref<FlowSettings | null>;
  configPaths: Ref<ConfigPath[]>;
  getSettingStateFlowVariables: () => FlowVariablesForSettings;
}

/** Exported only for tests */
export const injectionKey: InjectionKey<FlowVariableSettingsProvidedByControl> =
  Symbol("flowVariableSettingsProvidedByControl");
export const getFlowVariableSettingsProvidedByControl = () =>
  inject(injectionKey)!;

const getFlowSettingsFromMap =
  (flowVariablesMap: Record<string, FlowSettings>) =>
  (configPaths: string[]) => {
    return configPaths.map((key) => flowVariablesMap[key]).filter(Boolean);
  };

const toFlowSetting = (
  flowVariablesMap: Record<string, FlowSettings>,
  configPaths: {
    configPath: string;
    deprecatedConfigPaths: string[];
  }[],
) => {
  const getFlowSettings = getFlowSettingsFromMap(flowVariablesMap);
  const deprecatedFlowSettings = getFlowSettings(
    configPaths.flatMap(({ deprecatedConfigPaths }) => deprecatedConfigPaths),
  );
  const flowSettings = getFlowSettings(
    configPaths.map(({ configPath }) => configPath),
  );

  return [...deprecatedFlowSettings, ...flowSettings].reduce(
    (a, b) => {
      return {
        controllingFlowVariableAvailable:
          a?.controllingFlowVariableAvailable || // NOSONAR
          b?.controllingFlowVariableAvailable,
        controllingFlowVariableName: a?.controllingFlowVariableName
          ? a?.controllingFlowVariableName
          : b?.controllingFlowVariableName,
        exposedFlowVariableName: a?.exposedFlowVariableName
          ? a?.exposedFlowVariableName
          : b?.exposedFlowVariableName,
      };
    },
    null as FlowSettings | null,
  );
};

export interface UseFlowSettingsProps {
  path: Ref<string>;
}

export const useFlowSettings = (
  params: UseFlowSettingsProps,
): {
  flowSettings: Ref<FlowSettings | null>;
  disabledByFlowVariables: Ref<boolean>;
  configPaths: Ref<ConfigPath[]>;
} => {
  const { path } = params;
  const flowVariablesMap = getFlowVariablesMap();
  const persistSchema = injectForFlowVariables("getPersistSchema")();
  const configPaths = computed(() =>
    getConfigPaths({ persistSchema, path: path.value }),
  );
  const flowSettings = computed(() => {
    return toFlowSetting(flowVariablesMap, configPaths.value);
  });

  const hasDeprecatedVariables = computed(
    () =>
      configPaths.value
        .flatMap(({ deprecatedConfigPaths }) => deprecatedConfigPaths)
        .filter(
          (deprecatedConfigPath) => flowVariablesMap[deprecatedConfigPath],
        ).length > 0,
  );

  const disabledByFlowVariables = computed(
    () =>
      Boolean(flowSettings.value?.controllingFlowVariableName) ||
      hasDeprecatedVariables.value,
  );

  return { flowSettings, configPaths, disabledByFlowVariables };
};
