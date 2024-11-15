import { inject, InjectionKey, provide, type Ref } from "vue";

export type FlowVariableModels = {
  type: "twinlist";
  model: {
    updateMode: (mode: string) => void;
    mode: Ref<string>;
  };
};

export type FlowVariableModel<T extends FlowVariableModels["type"]> =
  (FlowVariableModels & { type: T })["model"];

const injectionKey: InjectionKey<FlowVariableModels> =
  Symbol("flowVariableModel");

export const setFlowVariableModel = (flowVariableModel: FlowVariableModels) =>
  provide(injectionKey, flowVariableModel);

export const useFlowVariableModel = () => inject(injectionKey);
