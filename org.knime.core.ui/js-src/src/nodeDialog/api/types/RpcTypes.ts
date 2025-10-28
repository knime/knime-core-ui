/**
 * Combined type for all RPC method calls used in NodeDialogCore
 *
 * This file imports and combines all RPC method families from various sources
 * to create a unified type for the callRpcMethod prop in NodeDialogCore.
 */

import type { DBTableChooserRpcMethods } from "../../uiComponents/dbTableChooser/useDbTableChooserBackend";
import type { FileChooserRpcMethods } from "../../uiComponents/fileChooser/composables/useFileChooserBackend";
import type { FileFilterPreviewRpcMethods } from "../../uiComponents/fileChooser/composables/useFileFilterPreviewBackend";
import type { FlowVariablesRpcMethods } from "../flowVariables";
import type { SettingsRpcMethods } from "../settings";

/**
 * Combined type for all RPC methods used in NodeDialogCore
 * This is the type that should be used for the callRpcMethod prop
 */
export type NodeDialogCoreRpcMethods = FlowVariablesRpcMethods &
  SettingsRpcMethods &
  FileChooserRpcMethods &
  FileFilterPreviewRpcMethods &
  DBTableChooserRpcMethods;
