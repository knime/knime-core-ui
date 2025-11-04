import type { InputOutputModel } from "@/components/InputOutputItem.vue";

export type PortViewConfig = {
  label: string;
  portViewIdx: number;
};

export type PortConfig = {
  /** undefined if no node is connected to an input port */
  nodeId?: string;
  portIdx: number;
  portViewConfigs: PortViewConfig[];
  portName: string;
};

export type PortConfigs = {
  inputPorts: PortConfig[];
};

export type ConnectionStatus =
  /** The input is not connected */
  | "MISSING_CONNECTION"
  /** The input is connected, but the predecessor is not configured */
  | "UNCONFIGURED_CONNECTION"
  /** The input is connected and configured, but the predecessor is not executed */
  | "UNEXECUTED_CONNECTION"
  /** The input is connected, configured, and executed */
  | "OK";

export type InputConnectionInfo = {
  status: ConnectionStatus;
  isOptional: boolean;
};

export type KAIConfig = {
  hubId: string;
  isKaiEnabled: boolean;
};

export type GenericInitialData = {
  inputPortConfigs: PortConfigs;
  inputObjects: InputOutputModel[];
  flowVariables: InputOutputModel;
  inputConnectionInfo: InputConnectionInfo[];
  outputObjects?: InputOutputModel[];
  kAiConfig: KAIConfig;
  [key: string]: unknown;
};
