import CompactTabBar from "../src/components/CompactTabBar.vue";
import {
  COLUMN_INSERTION_EVENT,
  type InputOutputModel,
  type SubItem,
  type SubItemType,
} from "../src/components/InputOutputItem.vue";
import InputOutputPane from "../src/components/InputOutputPane.vue";
import OutputConsole, {
  type ConsoleHandler,
  type ConsoleText,
} from "../src/components/OutputConsole.vue";
import OutputTablePreview from "../src/components/OutputTablePreview.vue";
import ScriptingEditor from "../src/components/ScriptingEditor.vue";
import { type SettingsMenuItem } from "../src/components/SettingsPage.vue";
import {
  type InsertionEvent,
  insertionEventHelper,
} from "../src/components/utils/insertionEventHelper";
import { type PaneSizes } from "../src/components/utils/paneSizes";
import useShouldFocusBePainted from "../src/components/utils/shouldFocusBePainted";
import { consoleHandler, setConsoleHandler } from "../src/consoleHandler";
import { displayMode } from "../src/display-mode";
import type {
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
} from "../src/editor";
import editor from "../src/editor";
import {
  type InitMockData,
  getInitialData,
  getScriptingService,
  getSettingsService,
  init,
  initConsoleEventHandler,
  initMocked,
} from "../src/init";
import {
  type GenericInitialData,
  type InputConnectionInfo,
  type KAIConfig,
  type PortConfigs,
} from "../src/initial-data-service";
import { type ScriptingServiceType } from "../src/scripting-service";
import {
  type GenericNodeSettings,
  type SettingsInitialData,
} from "../src/settings-service";
import { joinSettings } from "../src/settings-service";
import { setActiveEditorStoreForAi } from "../src/store/ai-bar";
import { useReadonlyStore } from "../src/store/readOnly";

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
  joinSettings,
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
  SettingsInitialData,
  SettingsMenuItem,
  SubItem,
  SubItemType,
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
};
