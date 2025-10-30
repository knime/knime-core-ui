import CompactTabBar from "@/components/CompactTabBar.vue";
import {
  COLUMN_INSERTION_EVENT,
  type InputOutputModel,
  type SubItem,
  type SubItemType,
} from "@/components/InputOutputItem.vue";
import InputOutputPane from "@/components/InputOutputPane.vue";
import OutputConsole, {
  type ConsoleHandler,
  type ConsoleText,
} from "@/components/OutputConsole.vue";
import OutputTablePreview from "@/components/OutputTablePreview.vue";
import ScriptingEditor from "@/components/ScriptingEditor.vue";
import { type SettingsMenuItem } from "@/components/SettingsPage.vue";
import {
  type InsertionEvent,
  insertionEventHelper,
} from "@/components/utils/insertionEventHelper";
import { type PaneSizes } from "@/components/utils/paneSizes";
import useShouldFocusBePainted from "@/components/utils/shouldFocusBePainted";
import { consoleHandler, setConsoleHandler } from "@/consoleHandler";
import { displayMode } from "@/display-mode";
import type {
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
} from "@/editor";
import editor from "@/editor";
import {
  type InitMockData,
  getInitialData,
  getScriptingService,
  getSettingsService,
  init,
  initConsoleEventHandler,
  initMocked,
} from "@/init";
import {
  type GenericInitialData,
  type InputConnectionInfo,
  type KAIConfig,
  type PortConfigs,
} from "@/initial-data-service";
import { type ScriptingServiceType } from "@/scripting-service";
import { type GenericNodeSettings } from "@/settings-service";
import { setActiveEditorStoreForAi } from "@/store/ai-bar";
import { useReadonlyStore } from "@/store/readOnly";

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
