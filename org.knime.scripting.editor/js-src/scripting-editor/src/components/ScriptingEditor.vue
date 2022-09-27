<script lang="ts">
import type { PropType } from 'vue';
import { defineComponent } from 'vue';
import * as monaco from 'monaco-editor';
import { Splitpanes, Pane } from 'splitpanes';
import Button from 'webapps-common/ui/components/Button.vue';
import CodeEditor from './CodeEditor.vue';
import TabPane from './TabPane.vue';
import type { TabOption } from './TabPane.vue';
import type { NodeSettings, ScriptingService } from '../utils/scripting-service';
import type { CEFWindow } from '../utils/cef-window';

declare let window: CEFWindow;

export default defineComponent({
    name: 'ScriptingEditor',
    components: {
        CodeEditor,
        Button,
        Splitpanes,
        Pane,
        TabPane
    },
    inject: ['getScriptingService'],
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
    data() {
        return {
            bottomActiveTab: 'console',
            leftActiveTab: 'inputs',
            // TODO(review) is this the right way to make the scripting service available?
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore type inference does not work!
            scriptingService: this.getScriptingService()
        } as {
            scriptingService: ScriptingService<NodeSettings>;
            bottomActiveTab: string;
            leftActiveTab: string;
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
  <div class="dialog">
    <div class="scripting-controls">
      <slot name="buttons" />
    </div>
    <!-- TODO(AP-19344) cleanup the layout + make panes and tabs optional -->
    <!-- TODO(AP-19344) should we use the splitpanes package or something else? -->
    <!-- TODO(AP-19344) should tabs be shown if there is only one option? -->
    <Splitpanes class="scripting default-theme">
      <Pane
        class="scripting-left-tabs"
        size="20"
      >
        <TabPane
          #default="{ activeTab }"
          name="lefttabpane"
          :initial-tab="initialLeftTab"
          :tabs="leftTabs"
        >
          <slot
            name="lefttabs"
            :active-tab="activeTab"
          />
        </TabPane>
      </Pane>
      <Pane
        class="scripting-content"
        min-size="50"
      >
        <Splitpanes horizontal>
          <Pane min-size="20">
            <Splitpanes vertical>
              <Pane min-size="50">
                <CodeEditor
                  :language="language"
                  :initial-script="initialScript"
                  @monaco-created="onMonacoCreated"
                />
              </Pane>
              <Pane size="20">
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
              </Pane>
            </Splitpanes>
          </Pane>
          <Pane>
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
          </Pane>
        </Splitpanes>
      </Pane>
    </Splitpanes>
    <div class="controls">
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
    </div>
  </div>
</template>

<style>
@import 'splitpanes/dist/splitpanes.css';
</style>

<style scoped lang="postcss">
.dialog {
    --controls-height: 49px;
    --description-button-size: 15px;

    display: flex;
    flex-direction: column;
    justify-content: space-between;
    height: 100vh;
    background-color: var(--knime-gray-ultra-light);

    & .scripting-controls {
        justify-content: space-between;
        height: var(--controls-height);
        padding: 13px 20px 5px 20px;
        background-color: var(--knime-porcelain);
        border-bottom: 1px solid var(--knime-silver-sand);
    }

    & .scripting {
        height: calc(100vh - var(--controls-height));
        overflow: hidden;
        overflow-y: auto;

        & .scripting-content {
            display: flex;
            flex-direction: column;
        }
    }

    & .controls {
        display: flex;
        justify-content: space-between;
        height: var(--controls-height);
        padding: 13px 20px 5px 20px;
        background-color: var(--knime-gray-light-semi);
        border-top: 1px solid var(--knime-silver-sand);
    }
}
</style>
