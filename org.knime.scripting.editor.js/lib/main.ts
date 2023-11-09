import ScriptingEditor from "../src/components/ScriptingEditor.vue";
import CodeEditor from "../src/components/CodeEditor.vue";
import OutputConsole from "../src/components/OutputConsole.vue";
import { EditorService } from "../src/editor-service";
import { getScriptingService as getScriptingServiceInternal } from "../src/scripting-service";
import { type SettingsMenuItem } from "../src/components/SettingsPage.vue";
import type {
  NodeSettings,
  ScriptingServiceType,
} from "../src/scripting-service";

const getScriptingService = (mock?: Partial<ScriptingServiceType>) =>
  getScriptingServiceInternal(mock) as ScriptingServiceType;

export {
  ScriptingEditor,
  CodeEditor,
  OutputConsole,
  getScriptingService,
  EditorService,
};
export type { NodeSettings, ScriptingServiceType, SettingsMenuItem };
