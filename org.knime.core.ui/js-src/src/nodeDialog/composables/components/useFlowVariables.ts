import { InjectionKey, Ref, computed, inject, provide } from "vue";
import { getConfigPaths, getDataPaths } from "@/nodeDialog/utils/paths";
import { FlowSettings } from "@/nodeDialog/api/types";
import Control from "@/nodeDialog/types/Control";
import { SettingStateWrapper } from "../nodeDialog/useDirtySettings";
import { getFlowVariablesMap } from "./useProvidedFlowVariablesMap";

export interface FlowVariableSettingsProvidedByControl {
  flowSettings: Ref<FlowSettings | null>;
  dataPaths: Ref<string[]>;
  configPaths: Ref<
    {
      configPath: string;
      deprecatedConfigPaths: string[];
    }[]
  >;
  settingStateFlowVariables: SettingStateWrapper["flowVariables"];
  hideFlowVariableButton?: true;
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

const getProvidedSettingStateFlowVariables = (
  {
    settingState,
    isNew,
  }: { settingState: SettingStateWrapper; isNew: boolean },
  configPaths: string[],
  flowVariablesMap: Record<string, FlowSettings>,
) => {
  if (isNew) {
    configPaths?.forEach((persistPath) => {
      const {
        controllingFlowVariableName = null,
        exposedFlowVariableName = null,
      } = flowVariablesMap[persistPath] ?? {};
      settingState.flowVariables.controlling.create(
        persistPath,
        controllingFlowVariableName,
      );
      settingState.flowVariables.exposed.create(
        persistPath,
        exposedFlowVariableName,
      );
    });
  }
  return settingState.flowVariables;
};

export interface UseFlowSettingsProps {
  control: Ref<Control>;
  settingState: { settingState: SettingStateWrapper; isNew: boolean };
  hideFlowVariableButton?: true;
}

export const useFlowSettings = (
  params: UseFlowSettingsProps,
): {
  flowSettings: Ref<FlowSettings | null>;
  disabledByFlowVariables: Ref<boolean>;
} => {
  const { control, settingState, hideFlowVariableButton } = params;
  const flowVariablesMap = getFlowVariablesMap();
  const path = computed(() => control.value.path);
  const configPaths = computed(() =>
    getConfigPaths({ control: control.value, path: path.value }),
  );
  const dataPaths = computed(() =>
    getDataPaths({ control: control.value, path: path.value }),
  );
  const flowSettings = computed(() => {
    return toFlowSetting(flowVariablesMap, configPaths.value);
  });

  const allConfigPaths = configPaths.value.flatMap(
    ({ configPath, deprecatedConfigPaths }) => [
      configPath,
      ...deprecatedConfigPaths,
    ],
  );

  provide(injectionKey, {
    flowSettings,
    dataPaths,
    configPaths,
    settingStateFlowVariables: getProvidedSettingStateFlowVariables(
      settingState,
      allConfigPaths,
      flowVariablesMap,
    ),
    hideFlowVariableButton,
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

  return { flowSettings, disabledByFlowVariables };
};
