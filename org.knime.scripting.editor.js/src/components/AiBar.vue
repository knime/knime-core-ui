<script setup lang="ts">
import {
  computed,
  ref,
  type PropType,
  onMounted,
  onUnmounted,
  nextTick,
} from "vue";
import { useTextareaAutosize } from "@vueuse/core";
import SendIcon from "webapps-common/ui/assets/img/icons/paper-flier.svg";
import AbortIcon from "webapps-common/ui/assets/img/icons/cancel-execution.svg";
import ExportIcon from "webapps-common/ui/assets/img/icons/export.svg";
import LinkIcon from "webapps-common/ui/assets/img/icons/link.svg";
import LoadingIcon from "webapps-common/ui/components/LoadingIcon.vue";
import Button from "webapps-common/ui/components/Button.vue";
import FunctionButton from "webapps-common/ui/components/FunctionButton.vue";
import { getScriptingService } from "@/scripting-service";
import CodeEditor from "./CodeEditor.vue";
import { editor } from "monaco-editor";
import {
  usePromptResponseStore,
  clearPromptResponseStore,
  showDisclaimer,
  type PromptResponseStore,
  type Message,
} from "@/store/ai-bar";
import type { PaneSizes } from "./ScriptingEditor.vue";

type Status =
  | "idle"
  | "error"
  | "waiting"
  | "uninstalled"
  | "unauthorized"
  | "readonly";

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
const mouseOverLoadingSpinner = ref(false);

const abortRequest = () => {
  scriptingService?.sendToService("abortSuggestCodeRequest");
  // this should be handled in the response of the eventhandler
  status.value = "waiting";
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
  status: "SUCCESS" | "ERROR" | "CANCELLED";
  error: string | undefined;
};

const handleCodeSuggestion = (codeSuggestion: CodeSuggestion) => {
  if (codeSuggestion.status === "ERROR") {
    scriptingService.sendToConsole({
      text: `ERROR: ${codeSuggestion.error}`,
    });
    status.value = "error";
  } else if (codeSuggestion.status === "CANCELLED") {
    status.value = "idle";
  } else {
    const suggestedCode = JSON.parse(codeSuggestion.code).code;
    message = { role: "request", content: input.value };
    promptResponseStore.promptResponse = {
      suggestedCode,
      message,
    };
    status.value = "idle";
    input.value = "";

    // NB: the initialScript in the CodeEditor is not reactive,
    // so we need to update the diff editor if it is already created
    diffEditor?.getModel()?.modified.setValue(suggestedCode);
  }
  nextTick(() => {
    textarea.value.focus();
  });
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

onMounted(async () => {
  if (!(await getScriptingService().isCodeAssistantInstalled())) {
    status.value = "uninstalled";
  } else if (
    (await getScriptingService().getInitialSettings()).scriptUsedFlowVariable
  ) {
    status.value = "readonly";
  } else if (!(await getScriptingService().sendToService("isLoggedIn"))) {
    status.value = "unauthorized";
  }
});

onUnmounted(() => {
  if (status.value === "waiting") {
    abortRequest();
  }
});

const handleLoginStatus = (loginStatus: boolean) => {
  if (loginStatus) {
    status.value = "idle";
  }
};

scriptingService.registerEventHandler("hubLogin", handleLoginStatus);

const tryLogin = () => {
  getScriptingService().sendToService("loginToHub");
};

// used to add a divider between notification bar / chat controls and above
const hasTopContent = computed(() => {
  return (
    status.value === "uninstalled" ||
    status.value === "unauthorized" ||
    showDisclaimer.value ||
    promptResponseStore.promptResponse
  );
});

// The id of the hub is used to display the name of the hub in the login button
// Updated with the actual hub id once the scripting service is ready
const hubId = ref<string>("KNIME Hub");
scriptingService.sendToService("getHubId").then((id) => {
  if (id !== null) {
    hubId.value = id;
  }
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
      ...(promptResponseStore.promptResponse ? {} : { 'max-width': '800px' }),
    }"
  >
    <div
      v-show="status === 'uninstalled'"
      class="notification-bar"
      :style="{ '--left-distance': `calc(${leftOverflow}vw + 30px)` }"
    >
      <span class="notification">
        To start generating code with our AI assistant, install the
        <i>KNIME AI Assistant</i> extension
      </span>
      <Button
        compact
        primary
        :to="'https://hub.knime.com/knime/extensions/org.knime.features.ai.assistant/latest'"
        class="notification-button"
      >
        <LinkIcon />Download from KNIME Hub
      </Button>
    </div>
    <div
      v-show="status === 'unauthorized'"
      class="notification-bar"
      :style="{ '--left-distance': `calc(${leftOverflow}vw + 30px)` }"
    >
      <span class="notification">
        To start generating code with our AI assistant, please log into your
        <i>KNIME Hub</i> account
      </span>
      <Button compact primary class="notification-button" @click="tryLogin()">
        <LinkIcon />Login to {{ hubId }}
      </Button>
    </div>
    <div
      v-show="status === 'readonly'"
      class="notification-bar"
      :style="{ '--left-distance': `calc(${leftOverflow}vw + 30px)` }"
    >
      <span class="notification">
        Script is overwritten by a flow variable.
      </span>
    </div>
    <div
      v-show="
        status !== 'uninstalled' &&
        status !== 'unauthorized' &&
        status !== 'readonly'
      "
    >
      <Transition name="disclaimer-slide-fade">
        <div v-if="showDisclaimer" class="disclaimer-container">
          <div class="disclaimer-text">
            <p style="font-weight: bold">Disclaimer</p>
            <p>
              By using this coding assistant, you acknowledge and agree the
              following: Any information you enter into the prompt, as well as
              the current code (being edited) and table headers, may be shared
              with OpenAI and KNIME in order to provide and improve this
              service.
            </p>

            <p>
              KNIME is not responsible for any content, input or output, or
              actions triggered by the generated code, and is not liable for any
              damages arising from or related to your use of the coding
              assistant.
            </p>

            <p>This is an experimental service, USE AT YOUR OWN RISK.</p>
          </div>
          <div class="disclaimer-button-container">
            <Button
              compact
              primary
              class="notification-button"
              @click="showDisclaimer = false"
              >Accept and close</Button
            >
          </div>
        </div>
      </Transition>
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
        :class="{ 'chat-controls-border-top': hasTopContent }"
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
            :disabled="status === 'waiting' || showDisclaimer"
            class="textarea"
            placeholder="Type your prompt"
            @keydown="handleKeyDown"
          />
          <div class="chat-controls-buttons">
            <FunctionButton
              v-if="status === 'error' || status === 'idle'"
              ref="sendButton"
              title="Send"
              :disabled="!input || showDisclaimer"
              class="textarea-button"
              @click="request"
            >
              <SendIcon class="icon" />
            </FunctionButton>
            <FunctionButton
              v-if="status === 'waiting'"
              ref="abortButton"
              title="Cancel"
              class="textarea-button"
              @click="abortRequest"
              @mouseover="mouseOverLoadingSpinner = true"
              @mouseleave="mouseOverLoadingSpinner = false"
            >
              <AbortIcon v-if="mouseOverLoadingSpinner" class="icon" />
              <LoadingIcon v-else class="icon" />
            </FunctionButton>
          </div>
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

.ai-bar-container {
  --ai-bar-margin: 13px;

  display: flex;
  flex-direction: column;
  position: absolute;
  bottom: 20px;
  background-color: var(--knime-gray-ultra-light);
  border: 1px solid var(--knime-silver-sand);
  font-size: 13px;
  line-height: 17px;
  z-index: 11; /* to display ai bar above the main code editor's scroll bar */
  overflow: visible;
  box-shadow: 0 -2px 8px 0 var(--knime-silver-sand-semi);
  margin-bottom: 12px; /* to hover above ai icon */
  transition: width 0.2s ease-in-out;

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

  & .chat-controls-border-top {
    border-top: 1px solid var(--knime-silver-sand);
  }

  & .chat-controls {
    display: flex;
    flex-direction: column;
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
        padding: 15px;
        padding-right: 30px;
        margin: var(--ai-bar-margin);
        color: var(--knime-masala);
        font-size: 13px;
        font-weight: lighter;
        font-family: Roboto, sans-serif;

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
          right: 20px;
          bottom: 20px;
        }
      }
    }
  }

  & :deep(.chat-controls::after) {
    --arrow-size: 18px;

    width: var(--arrow-size);
    height: var(--arrow-size);
    left: calc(var(--left-distance) + 40px);
    content: "";
    position: absolute;
    background-color: var(--knime-gray-ultra-light);
    bottom: 0;
    z-index: 1;
    border-right: 1px solid var(--knime-silver-sand);
    border-top: 1px solid var(--knime-silver-sand);
    transform: translate(-50%, 50%) rotate(135deg);
  }

  & .notification-bar {
    display: flex;
    justify-content: space-between;
    vertical-align: middle;
    border-top: 1px solid var(--knime-silver-sand);
    position: relative;
    height: 49px;
  }

  & :deep(.notification-bar::after) {
    --arrow-size: 18px;

    width: var(--arrow-size);
    height: var(--arrow-size);
    left: calc(var(--left-distance) + 40px);
    content: "";
    position: absolute;
    background-color: var(--knime-gray-ultra-light);
    bottom: 0;
    z-index: 1;
    border-right: 1px solid var(--knime-silver-sand);
    border-top: 1px solid var(--knime-silver-sand);
    transform: translate(-50%, 50%) rotate(135deg);
  }
}

.notification {
  line-height: 15.23px;
  margin: 15px;
}

.notification-button {
  height: 30px;
  margin: 9px;
  margin-right: 15px;
}

.disclaimer-container {
  display: flex;
  flex-direction: column;
}

.disclaimer-text {
  margin: 10px;
  line-height: 20px;
  padding: 5px;
  background-color: var(--knime-white);
}

.disclaimer-button-container {
  display: flex;
  justify-content: right;
}

.disclaimer-slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
}

.disclaimer-slide-fade-enter-from,
.disclaimer-slide-fade-leave-to {
  transform: translateY(30px);
  opacity: 0;
}
</style>
