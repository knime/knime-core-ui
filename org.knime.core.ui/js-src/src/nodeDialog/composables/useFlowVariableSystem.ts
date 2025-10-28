import { provide } from "vue";

import * as flowVariablesApi from "@/nodeDialog/api/flowVariables";
import type { FlowVariablesRpcMethods } from "../api/flowVariables";
import type { FlowSettings } from "../api/types";
import type { ProvidedForFlowVariables } from "../types/provided";

import useProvidedFlowVariablesMap from "./components/useProvidedFlowVariablesMap";
import type { DialogSettings } from "./nodeDialog/useUpdates";

/**
 * Sets up the flow variable system for reuse in any app.
 * Enables using the `FlowVariableButtonWrapper`.
 *
 * @param getCurrentData - Methods for interacting with flow variables backend
 * @param getCurrentData - accessor for the current dialog settings data
 */
export default ({
  callRpcMethod,
  getCurrentData,
}: {
  callRpcMethod: FlowVariablesRpcMethods;
  getCurrentData: () => DialogSettings;
}) => {
  // Set up the flow variables map with provide/inject
  const { flowVariablesMap, setInitialFlowVariablesMap } =
    useProvidedFlowVariablesMap();

  const getDataAndFlowVariables = () => ({
    data: getCurrentData(),
    flowVariableSettings: flowVariablesMap,
  });

  const getAvailableFlowVariables = (persistPath: string) =>
    flowVariablesApi.getAvailableFlowVariables(
      callRpcMethod,
      persistPath,
      getDataAndFlowVariables(),
    );
  const getFlowVariableOverrideValue = (dataPath: string) =>
    flowVariablesApi.getFlowVariableOverrideValue(
      callRpcMethod,
      dataPath,
      getDataAndFlowVariables(),
    );

  // Provide the API methods that flow variable components need
  provide<ProvidedForFlowVariables["flowVariablesApi"]>("flowVariablesApi", {
    getAvailableFlowVariables,
    getFlowVariableOverrideValue,
    clearControllingFlowVariable: () => {}, // Nothing to clear if flawed variables are not managed here
  });

  return {
    flowVariablesMap,
    setInitialFlowVariablesMap,
  };
};

/**
 * Type for initializing flow variable settings from initial data
 */
export type InitialFlowVariableSettings = Record<string, FlowSettings>;
