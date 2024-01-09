import CompactTabBar from "@/components/CompactTabBar.vue";
import type {
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
} from "../src/editor";
import editor from "../src/editor";
import OutputConsole from "../src/components/OutputConsole.vue";
import ScriptingEditor from "../src/components/ScriptingEditor.vue";
import { type SettingsMenuItem } from "../src/components/SettingsPage.vue";
import type {
  NodeSettings,
  ScriptingServiceType,
} from "../src/scripting-service";
import { getScriptingService as getScriptingServiceInternal } from "../src/scripting-service";
import * as scriptingServiceBrowserMock from "../src/scripting-service-browser-mock";

const getScriptingService = (mock?: Partial<ScriptingServiceType>) =>
  getScriptingServiceInternal(mock) as ScriptingServiceType;

export {
  CompactTabBar,
  OutputConsole,
  ScriptingEditor,
  editor,
  getScriptingService,
  scriptingServiceBrowserMock,
};
export type {
  NodeSettings,
  ScriptingServiceType,
  SettingsMenuItem,
  UseCodeEditorParams,
  UseCodeEditorReturn,
  UseDiffEditorParams,
  UseDiffEditorReturn,
};
