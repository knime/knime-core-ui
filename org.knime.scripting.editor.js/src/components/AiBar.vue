<script setup lang="ts">
import { computed, ref, type PropType } from "vue";
import { useTextareaAutosize } from "@vueuse/core";
import SendIcon from "webapps-common/ui/assets/img/icons/paper-flier.svg";
import AbortIcon from "webapps-common/ui/assets/img/icons/circle-close.svg";
import ExportIcon from "webapps-common/ui/assets/img/icons/export.svg";
import Button from "webapps-common/ui/components/Button.vue";
import { getScriptingService } from "@/scripting-service";
import CodeEditor from "./CodeEditor.vue";
import { editor } from "monaco-editor";
import {
  usePromptResponseStore,
  clearPromptResponseStore,
  type PromptResponseStore,
  type Message,
} from "@/store";
import type { PaneSizes } from "./ScriptingEditor.vue";

type Status = "idle" | "error" | "waiting";

// sizes in viewport width/height
const DEFAULT_AI_BAR_WIDTH = 65;
const DEFAULT_AI_BAR_HEIGHT = 40;
const DEFAULT_LEFT_OVERFLOW = 10;

const { textarea, input } = useTextareaAutosize();

const props = defineProps({
  currentPaneSizes: {
    type: Object as PropType<PaneSizes>,
    default: () => ({ left: 20, right: 25, bottom: 30 }),
  },
  language: {
    type: String,
    default: null,
  },
});

const emit = defineEmits<{
  (e: "accept-suggestion", value: boolean): void;
  (e: "close-ai-bar"): void;
}>();

const promptResponseStore: PromptResponseStore = usePromptResponseStore();
const status = ref<Status>("idle" as Status);
let message: Message | null =
  promptResponseStore.promptResponse?.message ?? null;
let history: Array<Message | null> = [];
const scriptingService = getScriptingService();
let diffEditor: editor.IStandaloneDiffEditor;

const abortRequest = () => {
  scriptingService?.sendToService("abortRequest");
  // this should be handled in the response of the eventhandler
  status.value = "error";
};

const acceptSuggestion = () => {
  history.push(message);
  status.value = "idle";
  const acceptedCode = diffEditor.getModifiedEditor().getValue();
  scriptingService.setScript(acceptedCode);
  clearPromptResponseStore();
  emit("accept-suggestion", true);
};

type CodeSuggestion = {
  code: string;
  status: "SUCCESS" | "ERROR";
  error: string | undefined;
};

const handleCodeSuggestion = (codeSuggestion: CodeSuggestion) => {
  if (codeSuggestion.status === "ERROR") {
    scriptingService.sendToConsole({
      text: `ERROR: ${codeSuggestion.error}`,
    });
    status.value = "error";
  } else {
    const suggestedCode = JSON.parse(codeSuggestion.code).code;
    message = { role: "request", content: input.value };
    promptResponseStore.promptResponse = {
      suggestedCode,
      message,
    };
    status.value = "idle";
    input.value = "";
  }
};

scriptingService.registerEventHandler("codeSuggestion", handleCodeSuggestion);

const request = async () => {
  status.value = "waiting";

  // code suggestions will be returned via events, see the response handler above
  await scriptingService.sendToService("suggestCode", [
    input.value,
    scriptingService.getScript(),
  ]);
};

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    if (status.value !== "waiting") {
      request();
    }
  }
  if (e.key === "ArrowUp" && !input.value) {
    // show last history message if available
    input.value = history.length ? history[history.length - 1]!.content : "";
  }
};

const onDiffEditorCreated = ({
  editor,
}: {
  editor: editor.IStandaloneDiffEditor;
}) => {
  diffEditor = editor;
};
const leftPosition = computed(() =>
  Math.max(props.currentPaneSizes.left - DEFAULT_LEFT_OVERFLOW, 0),
);
const leftOverflow = computed(
  () => props.currentPaneSizes.left - leftPosition.value,
);
const bottomPosition = computed(
  () =>
    // (100vh - 98px) * bottomPaneSize = actual size of bottom pane (49 px is size of controls)
    // + 2*49px offset (footer and control bar)
    `calc( ((100vh - 98px) * ${props.currentPaneSizes.bottom / 100}) + 98px )`,
);
const aiBarWidth = computed(() => {
  return Math.min(DEFAULT_AI_BAR_WIDTH, 100 - leftPosition.value);
});
</script>

<template>
  <div
    class="ai-bar-container"
    :style="{
      bottom: `${bottomPosition}`,
      left: `${leftPosition}vw`,
      width: `${aiBarWidth}vw`,
      height: `${DEFAULT_AI_BAR_HEIGHT}`,
    }"
  >
    <Transition name="slide-fade">
      <div
        v-if="promptResponseStore.promptResponse"
        class="diff-editor-container"
      >
        <CodeEditor
          :language="language"
          class="diff-editor"
          :diff-script="promptResponseStore.promptResponse.suggestedCode"
          @monaco-created="onDiffEditorCreated"
        />
        <div class="accept-decline-buttons">
          <Button with-border compact @click="acceptSuggestion">
            <ExportIcon /> Insert in editor
          </Button>
        </div>
      </div>
    </Transition>
    <div
      class="chat-controls"
      :style="{ '--left-distance': `calc(${leftOverflow}vw + 30px)` }"
    >
      <Transition name="slide-fade">
        <div v-if="promptResponseStore.promptResponse" class="prompt-bar">
          {{ promptResponseStore.promptResponse.message.content }}
        </div>
      </Transition>
      <div class="chat-controls-text-input">
        <textarea
          ref="textarea"
          v-model="input"
          :readonly="status === 'waiting'"
          class="textarea"
          placeholder="Type your prompt"
          @keydown="handleKeyDown"
        />
        <div class="chat-controls-buttons">
          <Button
            v-show="status === 'error' || status === 'idle'"
            ref="sendButton"
            title="Send"
            :disabled="!input"
            class="textarea-button"
            @click="request"
          >
            <SendIcon class="icon" />
          </Button>
          <Button
            v-show="status === 'waiting'"
            ref="abortButton"
            title="Cancel"
            class="textarea-button"
            @click="abortRequest"
          >
            <AbortIcon class="icon" />
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.8s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(30px);
  opacity: 0;
}

.dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background-color: var(--knime-silver-sand);
  animation-name: loading;
  animation-duration: 4s;
  animation-iteration-count: infinite;
  margin-left: 2px;

  &:nth-child(1) {
    animation-delay: 1s;
  }

  &:nth-child(2) {
    animation-delay: 0.5s;
  }

  &:nth-child(3) {
    animation-delay: 1.5s;
  }
}

@keyframes loading {
  0% {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }

  50% {
    opacity: 0.5;
    transform: translate3d(0, 0, 0);
  }

  75% {
    opacity: 0;
    transform: translate3d(0, 0, 0);
  }

  100% {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

.ai-bar-container {
  --ai-bar-margin: 13px;

  display: flex;
  flex-direction: column;
  position: absolute;
  bottom: 20px;
  background-color: var(--knime-porcelain);
  border: 1px solid var(--knime-silver-sand);
  font-size: 13px;
  line-height: 17px;
  z-index: 11; /* to display ai bar above the main code editor's scroll bar */
  overflow: visible;
  box-shadow: 0 -2px 8px 0 var(--knime-gray-dark);
  margin-bottom: 30px; /* to hover above ai icon */

  & .subtitle {
    color: var(--knime-black);
    margin-top: 9px;
    font-style: italic;
    display: flex;
    justify-content: flex-start;
    align-items: baseline;
    flex-direction: row;

    & .text {
      margin-right: 2px;
    }

    & .button {
      position: absolute;
      bottom: 4px;
      right: 79px;

      &.primary {
        right: 4px;
      }
    }
  }

  & .diff-editor-container {
    margin: var(--ai-bar-margin);
    min-height: 200px;
    height: 40vh;
    border-bottom: 1px solid var(--knime-porcelain);
    display: flex;
    flex-direction: column;

    & .diff-editor {
      flex-grow: 1;
    }

    & .accept-decline-buttons {
      & .button {
        float: right;
        margin-top: var(--ai-bar-margin);
      }
    }
  }

  & .chat-controls {
    display: flex;
    flex-direction: column;
    border-top: 1px solid var(--knime-silver-sand);
    position: relative;

    & .prompt-bar {
      margin-top: var(--ai-bar-margin);
      margin-right: var(--ai-bar-margin);
      margin-left: var(--ai-bar-margin);
      line-height: 15.23px;
    }

    & .chat-controls-text-input {
      display: flex;
      flex-direction: row;

      & .textarea {
        flex-grow: 1;
        border: 1px solid var(--knime-silver-sand);
        overflow: hidden;
        resize: none;
        border-radius: 0;
        bottom: -1;
        padding: 9px;
        padding-right: 30px;
        margin: var(--ai-bar-margin);
        color: var(--knime-masala);

        &::placeholder {
          color: var(--knime-stone);
        }

        &:focus {
          outline: none;
        }
      }

      & .chat-controls-buttons {
        & .textarea-button {
          position: absolute;
          right: -14px;
          bottom: 8px;
        }
      }
    }
  }

  & :deep(.chat-controls::after) {
    --arrow-size: 18px;

    width: var(--arrow-size);
    height: var(--arrow-size);
    left: var(--left-distance);
    content: "";
    position: absolute;
    background-color: var(--knime-porcelain);
    bottom: 0;
    z-index: 1;
    border-right: 1px solid var(--knime-silver-sand);
    border-top: 1px solid var(--knime-silver-sand);
    transform: translate(-50%, 50%) rotate(135deg);
  }
}
</style>
