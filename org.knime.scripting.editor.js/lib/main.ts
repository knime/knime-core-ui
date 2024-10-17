import CompactTabBar from "@/components/CompactTabBar.vue";
import OutputConsole, {
  type ConsoleHandler,
  type ConsoleText,
} from "@/components/OutputConsole.vue";
import ScriptingEditor from "@/components/ScriptingEditor.vue";
import { setConsoleHandler, consoleHandler } from "@/consoleHandler";
import { type SettingsMenuItem } from "@/components/SettingsPage.vue";
import useShouldFocusBePainted from "@/components/utils/shouldFocusBePainted";
import {
  MIN_WIDTH_FOR_DISPLAYING_PANES,
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
} from "@/components/utils/paneSizes";
import {
  type InputOutputModel,
  type SubItem,
  COLUMN_INSERTION_EVENT,
} from "@/components/InputOutputItem.vue";
import InputOutputPane from "@/components/InputOutputPane.vue";
import type {
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
} from "@/editor";
import editor from "@/editor";
import { useReadonlyStore } from "@/store/readOnly";
import {
  getScriptingService,
  initConsoleEventHandler,
  type ScriptingServiceType,
} from "@/scripting-service";
import { setActiveEditorStoreForAi } from "@/store/ai-bar";
import {
  insertionEventHelper,
  type InsertionEvent,
} from "@/components/utils/insertionEventHelper";
import OutputTablePreview from "@/components/OutputTablePreview.vue";
import {
  getInitialDataService,
  type InitialDataServiceType,
  type GenericInitialData,
  type KAIConfig,
  type PortConfigs,
  type InputConnectionInfo,
} from "@/initial-data-service";
import {
  type GenericNodeSettings,
  getSettingsService,
} from "@/settings-service";

export {
  COLUMN_INSERTION_EVENT,
  CompactTabBar,
  consoleHandler,
  editor,
  initConsoleEventHandler,
  getInitialDataService,
  getScriptingService,
  getSettingsService,
  insertionEventHelper,
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
  MIN_WIDTH_FOR_DISPLAYING_PANES,
  OutputConsole,
  OutputTablePreview,
  ScriptingEditor,
  InputOutputPane,
  setActiveEditorStoreForAi,
  setConsoleHandler,
  useShouldFocusBePainted,
  useReadonlyStore,
};
export type {
  ConsoleHandler,
  ConsoleText,
  GenericInitialData,
  GenericNodeSettings,
  InitialDataServiceType,
  SubItem,
  InputOutputModel,
  InsertionEvent,
  KAIConfig,
  PortConfigs,
  InputConnectionInfo,
  ScriptingServiceType,
  SettingsMenuItem,
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
};
