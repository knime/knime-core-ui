import CompactTabBar from "@s/components/CompactTabBar.vue";
import {
  COLUMN_INSERTION_EVENT,
  type InputOutputModel,
  type SubItem,
  type SubItemType,
} from "@s/components/InputOutputItem.vue";
import InputOutputPane from "@s/components/InputOutputPane.vue";
import OutputConsole, {
  type ConsoleHandler,
  type ConsoleText,
} from "@s/components/OutputConsole.vue";
import OutputTablePreview from "@s/components/OutputTablePreview.vue";
import ScriptingEditor from "@s/components/ScriptingEditor.vue";
import { type SettingsMenuItem } from "@s/components/SettingsPage.vue";
import {
  type InsertionEvent,
  insertionEventHelper,
} from "@s/components/utils/insertionEventHelper";
import { type PaneSizes } from "@s/components/utils/paneSizes";
import useShouldFocusBePainted from "@s/components/utils/shouldFocusBePainted";
import { consoleHandler, setConsoleHandler } from "@s/consoleHandler";
import { displayMode } from "@s/display-mode";
import type {
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
} from "@s/editor";
import editor from "@s/editor";
import {
  type InitMockData,
  getInitialData,
  getScriptingService,
  getSettingsService,
  init,
  initConsoleEventHandler,
  initMocked,
} from "@s/init";
import {
  type GenericInitialData,
  type InputConnectionInfo,
  type KAIConfig,
  type PortConfigs,
} from "@s/initial-data-service";
import { type ScriptingServiceType } from "@s/scripting-service";
import { type GenericNodeSettings } from "@s/settings-service";
import { setActiveEditorStoreForAi } from "@s/store/ai-bar";
import { useReadonlyStore } from "@s/store/readOnly";

import NodeDialogCore from "@knime/core-ui/src/nodeDialog/NodeDialogCore.vue";
import useFlowVariableSystem from "@knime/core-ui/src/nodeDialog/composables/useFlowVariableSystem";
import type { NodeDialogCoreRpcMethods } from "@knime/core-ui/src/nodeDialog/api/types/RpcTypes";
import type { InitialData } from "@knime/core-ui/src/nodeDialog/types/InitialData";

export {
  COLUMN_INSERTION_EVENT,
  CompactTabBar,
  consoleHandler,
  displayMode,
  editor,
  getInitialData,
  getScriptingService,
  getSettingsService,
  init,
  initConsoleEventHandler,
  initMocked,
  InputOutputPane,
  insertionEventHelper,
  OutputConsole,
  OutputTablePreview,
  ScriptingEditor,
  setActiveEditorStoreForAi,
  setConsoleHandler,
  useReadonlyStore,
  useShouldFocusBePainted,
  NodeDialogCore,
  useFlowVariableSystem,
};
export type {
  ConsoleHandler,
  ConsoleText,
  GenericInitialData,
  GenericNodeSettings,
  InitMockData,
  InputConnectionInfo,
  InputOutputModel,
  InsertionEvent,
  KAIConfig,
  PaneSizes,
  PortConfigs,
  ScriptingServiceType,
  SettingsMenuItem,
  SubItem,
  SubItemType,
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
  NodeDialogCoreRpcMethods,
  InitialData,
};
