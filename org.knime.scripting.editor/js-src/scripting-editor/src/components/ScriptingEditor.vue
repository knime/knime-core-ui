<script lang="ts">
import type { PropType } from 'vue';
import { defineComponent, inject } from 'vue';
import * as monaco from 'monaco-editor';

import Button from 'webapps-common/ui/components/Button.vue';
import CodeEditor from './CodeEditor.vue';
import TabPane from './TabPane.vue';

import type { TabOption } from './TabPane.vue';
import type { NodeSettings, ScriptingServiceImpl } from '../utils/scripting-service';
import type { CEFWindow } from '../utils/cef-window';

import EditorLayout from './EditorLayout.vue';

declare let window: CEFWindow;

export default defineComponent({
    name: 'ScriptingEditor',
    components: {
        EditorLayout,
        CodeEditor,
        Button,
        TabPane
    },
    props: {
        language: {
            type: String,
            default: null
        },
        initialLeftTab: {
            type: String,
            default: ''
        },
        leftTabs: {
            type: Array as PropType<TabOption[]>,
            default() {
                return [];
            }
        },
        initialRightTab: {
            type: String,
            default: ''
        },
        rightTabs: {
            type: Array as PropType<TabOption[]>,
            default() {
                return [];
            }
        },
        initialBottomTab: {
            type: String,
            default: ''
        },
        bottomTabs: {
            type: Array as PropType<TabOption[]>,
            default() {
                return [];
            }
        }
    },
    emits: ['monaco-created'],
    setup() {
        const scriptingService = inject('scriptingService') as ScriptingServiceImpl<NodeSettings>;

        return {
            scriptingService
        };
    },
    data() {
        return {
            bottomActiveTab: 'console',
            leftActiveTab: 'inputs'
        };
    },
    computed: {
        initialScript(): string {
            return this.scriptingService.getInitialScript();
        }
    },
    methods: {
        // Called by general control buttons
        async applySettings() {
            // NB:
            // Old nodes open a popup to warn the user if the node will be reset.
            // However, we don't have to warn the user because this is expected.
            await this.scriptingService.applySettings();
        },
        closeDialog() {
            // TODO closing a dialog should be abstracted in the UI-Extensions framework
            window.closeCEFWindow();
        },
        async applySettingsCloseDialog() {
            await this.applySettings();
            this.closeDialog();
        },

        onMonacoCreated({
            editor,
            editorModel
        }: {
            editor: monaco.editor.IStandaloneCodeEditor;
            editorModel: monaco.editor.ITextModel;
        }) {
            // Update script when the user changes it
            editorModel.onDidChangeContent(() => {
                this.scriptingService.setScript(editorModel.getValue());
            });

            // Bind Ctrl-S to applying the settings
            // eslint-disable-next-line no-bitwise
            editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => {
                this.applySettings();
            });

            // Bind Ctrl-P to the Command Palette
            // eslint-disable-next-line no-bitwise
            editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyP, () => {
                editor.trigger('', 'editor.action.quickCommand', {});
            });

            this.$emit('monaco-created', { editor, editorModel });
        }
    }
});
</script>

<template>
  <!-- TODO(AP-19344) cleanup the layout + make panes and tabs optional -->
  <!-- TODO(AP-19344) should we use the splitpanes package or something else? -->
  <!-- TODO(AP-19344) should tabs be shown if there is only one option? -->
  <EditorLayout>
    <template #header>
      <slot name="buttons" />
    </template>
    <template #inputs>
      <slot
        name="inputs"
      />
    </template>
    <template #conda_env>
      <slot name="conda_env" />
    </template>
    <template #right-pane>
      <TabPane
        #default="{ activeTab }"
        name="righttabpane"
        :initial-tab="initialRightTab"
        :tabs="rightTabs"
      >
        <slot
          name="righttabs"
          :active-tab="activeTab"
        />
      </TabPane>
    </template>
    <template #editor>
      <CodeEditor
        :language="language"
        :initial-script="initialScript"
        @monaco-created="onMonacoCreated"
      />
    </template>
    <template #bottom>
      <TabPane
        #default="{ activeTab }"
        name="bottomtabpane"
        :initial-tab="initialBottomTab"
        :tabs="bottomTabs"
      >
        <slot
          name="bottomtabs"
          :active-tab="activeTab"
        />
      </TabPane>
    </template>
    <template #footer>
      <Button
        with-border
        compact
        @click="closeDialog"
      >
        Cancel
      </Button>
      <Button
        primary
        compact
        @click.prevent="applySettingsCloseDialog"
      >
        Ok
      </Button>
    </template>
  </EditorLayout>
</template>

<style lang="postcss" scoped>

.tab-pane{
  flex: 1;
}
</style>
