import { type InjectionKey, inject, provide, reactive } from "vue";

import type { FlowSettings } from "../../api/types";

// exported for tests
export const injectionKey: InjectionKey<Record<string, FlowSettings>> =
  Symbol("flowVariablesMap");
/**
 * A small composable which allows to make the flow variables map provided via the
 * initial data a reactive object available to all components.
 */
export default () => {
  const flowVariablesMap = reactive<Record<string, FlowSettings>>({});
  const setInitialFlowVariablesMap = (
    initialMap: Record<string, FlowSettings>,
  ) => {
    Object.keys(initialMap).forEach((key) => {
      flowVariablesMap[key] = initialMap[key];
    });
  };
  provide(injectionKey, flowVariablesMap);
  return {
    flowVariablesMap,
    setInitialFlowVariablesMap,
  };
};

export const getFlowVariablesMap = () => inject(injectionKey)!;
