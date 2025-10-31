import CompactTabBar from "@knime-scripting-editor/components/CompactTabBar.vue";
import {
  COLUMN_INSERTION_EVENT,
  type InputOutputModel,
  type SubItem,
  type SubItemType,
} from "@knime-scripting-editor/components/InputOutputItem.vue";
import InputOutputPane from "@knime-scripting-editor/components/InputOutputPane.vue";
import OutputConsole, {
  type ConsoleHandler,
  type ConsoleText,
} from "@knime-scripting-editor/components/OutputConsole.vue";
import OutputTablePreview from "@knime-scripting-editor/components/OutputTablePreview.vue";
import ScriptingEditor from "@knime-scripting-editor/components/ScriptingEditor.vue";
import { type SettingsMenuItem } from "@knime-scripting-editor/components/SettingsPage.vue";
import {
  type InsertionEvent,
  insertionEventHelper,
} from "@knime-scripting-editor/components/utils/insertionEventHelper";
import { type PaneSizes } from "@knime-scripting-editor/components/utils/paneSizes";
import useShouldFocusBePainted from "@knime-scripting-editor/components/utils/shouldFocusBePainted";
import { consoleHandler, setConsoleHandler } from "@knime-scripting-editor/consoleHandler";
import { displayMode } from "@knime-scripting-editor/display-mode";
import type {
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
} from "@knime-scripting-editor/editor";
import editor from "@knime-scripting-editor/editor";
import {
  type InitMockData,
  getInitialData,
  getScriptingService,
  getSettingsService,
  init,
  initConsoleEventHandler,
  initMocked,
} from "@knime-scripting-editor/init";
import {
  type GenericInitialData,
  type InputConnectionInfo,
  type KAIConfig,
  type PortConfigs,
} from "@knime-scripting-editor/initial-data-service";
import { type ScriptingServiceType } from "@knime-scripting-editor/scripting-service";
import { type GenericNodeSettings } from "@knime-scripting-editor/settings-service";
import { setActiveEditorStoreForAi } from "@knime-scripting-editor/store/ai-bar";
import { useReadonlyStore } from "@knime-scripting-editor/store/readOnly";

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
};
