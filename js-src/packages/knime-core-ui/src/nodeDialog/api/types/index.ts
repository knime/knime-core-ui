import type { IdAndText } from "../../types/ChoicesUiSchema";

export type PossibleFlowVariable = {
  name: string;
  value: string;
  abbreviated: boolean;
  type: IdAndText;
};

export type FlowSettings = {
  controllingFlowVariableName: string | null;
  exposedFlowVariableName: string | null;
  controllingFlowVariableAvailable: boolean;
  controllingFlowVariableFlawed?: boolean;
};
