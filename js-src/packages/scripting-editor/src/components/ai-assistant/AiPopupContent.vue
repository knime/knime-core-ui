<script lang="ts">
/**
 * Content for the popup that contains the prompt for the KAi, as well as holding the components
 * that show the diff and disclaimer.
 */
export default {};
</script>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import { useTextareaAutosize } from "@vueuse/core";

import { Button, FunctionButton, InlineMessage } from "@knime/components";
import AbortIcon from "@knime/styles/img/icons/cancel-execution.svg";
import WarningIcon from "@knime/styles/img/icons/circle-warning.svg";
import SendIcon from "@knime/styles/img/icons/paper-flier.svg";

import InfinityLoadingBar from "@/components/InfinityLoadingBar.vue";
import {
  getInitialData,
  getScriptingService,
  getSettingsService,
} from "@/init";
import { type UsageData } from "@/scripting-service";
import {
  type Message,
  type PromptResponseStore,
  activeEditorStore,
  clearPromptResponseStore,
  currentInputOutputItems,
  showDisclaimer,
  usageData,
  usePromptResponseStore,
} from "@/store/ai-bar";

import AiDisclaimer from "./AiDisclaimer.vue";
import AiSuggestion from "./AiSuggestion.vue";

type Status =
  | "idle"
  | "error"
  | "waiting"
  | "disabledOrUninstalled" // K-AI-related UI elements shouldn't be rendered for both of these cases: K-AI is uninstalled / K-AI is disabled
  | "newlyDisabled"
  | "unauthorized"
  | "readonly";

const { textarea, input } = useTextareaAutosize({
  // Note that useTextareaAutosize does not set the size correctly if the
  // textarea has a border and "box-sizing: border-box". This is a workaround
  // to take the border into account
  // See https://github.com/vueuse/vueuse/issues/3133
  onResize() {
    if (!textarea.value) {
      return;
    }
    const style = getComputedStyle(textarea.value);
    const heightWithBorder =
      parseFloat(textarea.value.style.height) +
      parseFloat(style.borderTopWidth) +
      parseFloat(style.borderBottomWidth);
    textarea.value.style.height = `${heightWithBorder}px`;
  },
});

const emit = defineEmits(["request-close"]);

const promptResponseStore: PromptResponseStore = usePromptResponseStore();
const status = ref<Status>("idle" as Status);
let message: Message | null =
  promptResponseStore.promptResponse?.message ?? null;
const history: Array<Message | null> = [];
const scriptingService = getScriptingService();

const abortRequest = () => {
  scriptingService?.sendToService("abortSuggestCodeRequest");
  // this should be handled in the response of the eventhandler
  status.value = "waiting";
};

type CodeSuggestion = {
  code: string;
  status: "SUCCESS" | "ERROR" | "CANCELLED";
  error: string | undefined;
};

type AIResponseData = {
  code: string;
  interactionId: string;
  usage?: UsageData;
};

const errorText = ref<string | null>(null);

const handleCodeSuggestion = (codeSuggestion: CodeSuggestion) => {
  errorText.value = null;

  if (codeSuggestion.status === "ERROR") {
    errorText.value = codeSuggestion.error ?? null;
    status.value = "error";
  } else if (codeSuggestion.status === "CANCELLED") {
    status.value = "idle";
  } else {
    const responseData: AIResponseData = JSON.parse(codeSuggestion.code);
    const suggestedCode = responseData.code;

    // Update usage data if provided
    if (responseData.usage) {
      usageData.value = responseData.usage;
    }

    message = { role: "request", content: input.value };
    promptResponseStore.promptResponse = { suggestedCode, message };
    status.value = "idle";
    input.value = "";
  }
  nextTick(() => {
    textarea.value?.focus();
  });
};

scriptingService.registerEventHandler("codeSuggestion", handleCodeSuggestion);

const request = async () => {
  status.value = "waiting";

  // code suggestions will be returned via events, see the response handler above
  await scriptingService.sendToService("suggestCode", [
    input.value,
    activeEditorStore.value?.text.value,
    currentInputOutputItems.value,
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

const acceptSuggestion = (acceptedCode: string) => {
  history.push(message);
  status.value = "idle";
  activeEditorStore.value!.text.value = acceptedCode;
  clearPromptResponseStore();

  emit("request-close");
};

onMounted(async () => {
  const [currentIsKaiEnabledStatus] = await Promise.all([
    getScriptingService().isKaiEnabled(),
  ]);
  const settings = getSettingsService().getSettings();
  const initialData = getInitialData();

  if (!initialData.kAiConfig.isKaiEnabled) {
    // K-AI is disabled on launch of the scripting editor
    status.value = "disabledOrUninstalled";
  } else if (!currentIsKaiEnabledStatus) {
    // K-AI is enabled on launch, but got disabled while the scripting editor was open
    status.value = "newlyDisabled";
  } else if (settings.settingsAreOverriddenByFlowVariable) {
    status.value = "readonly";
  } else if (await getScriptingService().isLoggedIntoHub()) {
    // Fetch initial usage data if K-AI is available and user is logged in
    fetchUsageData();
  } else {
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
    fetchUsageData();
  }
};

scriptingService.registerEventHandler("hubLogin", handleLoginStatus);

const tryLogin = () => {
  scriptingService.sendToService("loginToHub");
};

// used to add a divider between notification bar / chat controls and above
const hasTopContent = computed(() => {
  return (
    status.value === "disabledOrUninstalled" ||
    status.value === "unauthorized" ||
    showDisclaimer.value ||
    promptResponseStore.promptResponse
  );
});

// Used to disable the send button if the prompt is too long, so as not to break the AI service.
// 3000 characters is enough to paste in a non-trivial code snippet or error message, which is
// hopefully enough for users.
const MAX_PROMPT_LENGTH = 3000;

const promptTooLong = computed(() => {
  return input.value ? input.value.length > MAX_PROMPT_LENGTH : false;
});

// The id of the hub is used to display the name of the hub in the login button
// Updated with the actual hub id once the scripting service is ready
const hubId = ref<string>("KNIME Hub");
const id = getInitialData().kAiConfig.hubId;
if (id !== null) {
  hubId.value = id;
}

/** Fetch usage data in background and update store */
const fetchUsageData = () => {
  getScriptingService()
    .getAiUsage()
    .then((usage) => {
      usageData.value = usage;
    })
    .catch((error) => {
      consola.warn("Failed to fetch AI usage data:", error);
      usageData.value = null;
    });
};

/** false if the usage limit is exceeded and the message prompt should not be shown */
const isWithinLimit = computed(() => {
  // Pro users (null limit) or users without data loaded are within limit
  if (usageData.value === null || usageData.value.limit === null) {
    return true;
  }
  // Check if used is less than limit
  return usageData.value.used < usageData.value.limit;
});

/** @returns the days left in the current month */
const getDaysLeftInMonth = (date: Date = new Date()): number => {
  const endOfMonth = new Date(date.getFullYear(), date.getMonth() + 1, 0);
  return endOfMonth.getDate() - date.getDate();
};
</script>

<template>
  <div class="popup-content">
    <!-- Edge case: If K-AI gets disabled while a scripting editor is open, the "Ask K-AI" button will still be rendered and clickable. -->
    <div v-show="status === 'newlyDisabled'" class="notification-bar">
      <span class="notification"> K-AI has been disabled. </span>
    </div>

    <div v-show="status === 'unauthorized'" class="notification-bar">
      <span class="notification">
        To start generating code with our AI assistant, please log into your
        <i>KNIME Hub</i> account
      </span>
      <Button compact primary class="notification-button" @click="tryLogin()">
        Login to {{ hubId }}
      </Button>
    </div>
    <div v-show="status === 'readonly'" class="notification-bar">
      <span class="notification">
        Script is overwritten by a flow variable.
      </span>
    </div>
    <div
      v-show="
        status !== 'disabledOrUninstalled' &&
        status !== 'newlyDisabled' &&
        status !== 'unauthorized' &&
        status !== 'readonly'
      "
    >
      <Transition name="disclaimer-slide-fade">
        <AiDisclaimer
          v-if="showDisclaimer && isWithinLimit"
          @accept-disclaimer="showDisclaimer = false"
        />
      </Transition>
      <Transition name="slide-fade">
        <AiSuggestion
          v-if="promptResponseStore.promptResponse"
          class="ai-suggestion"
          @accept-suggestion="acceptSuggestion"
        />
      </Transition>
      <div
        class="chat-controls"
        :class="{ 'chat-controls-border-top': hasTopContent }"
      >
        <Transition name="slide-fade">
          <div v-if="promptResponseStore.promptResponse" class="prompt-bar">
            {{ promptResponseStore.promptResponse.message.content }}
          </div>
        </Transition>
        <InfinityLoadingBar v-if="status === 'waiting'" />
        <InlineMessage
          v-if="!isWithinLimit"
          variant="info"
          title="All free monthly AI interactions used"
          class="limit-exceeded-message"
        >
          <a href="https://www.knime.com/knime-hub-pricing">Upgrade</a> to
          continue building with AI or wait
          {{ getDaysLeftInMonth() }}
          days to use it again.
        </InlineMessage>
        <div v-else class="chat-controls-text-input">
          <textarea
            ref="textarea"
            v-model="input"
            :disabled="status === 'waiting' || showDisclaimer"
            class="textarea"
            placeholder="Describe your task"
            wrap="soft"
            @keydown="handleKeyDown"
          />
          <div class="chat-controls-buttons">
            <FunctionButton
              v-if="status === 'error' || status === 'idle'"
              ref="sendButton"
              primary
              title="Send"
              :disabled="!input || showDisclaimer || promptTooLong"
              class="send-button"
              @click="request"
            >
              <SendIcon class="icon" />
            </FunctionButton>
            <FunctionButton
              v-if="status === 'waiting'"
              ref="abortButton"
              title="Cancel"
              class="send-button abort-button"
              @click="abortRequest"
            >
              <AbortIcon class="icon" />
            </FunctionButton>
          </div>
        </div>
        <div v-if="status === 'error'" class="error-message">
          <WarningIcon class="error-icon" />
          <span class="error-text">
            {{ errorText }}
          </span>
        </div>
        <div v-if="promptTooLong" class="error-message">
          <WarningIcon class="error-icon" />
          <span class="error-text">
            "The prompt is too long. Please shorten it to
            {{ MAX_PROMPT_LENGTH }} characters or less."
          </span>
        </div>
        <div class="usage-limit">
          <span
            v-if="usageData && usageData.limit !== null && isWithinLimit"
            class="usage-counter"
          >
            {{ usageData?.used ?? "−" }}/{{ usageData?.limit ?? "−" }} monthly
            interactions
          </span>
          <span class="usage-disclaimer"> K-AI can make mistakes </span>
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

.popup-content {
  --ai-bar-margin: var(--space-8);
  --ai-bar-corner-radius: 8px;

  display: flex;
  flex-direction: column;
  background-color: var(--knime-gray-ultra-light);
  border: 1px solid var(--knime-porcelain);
  border-radius: var(--ai-bar-corner-radius);
  font-size: 13px;
  line-height: 27px;
  z-index: 11; /* to display ai bar above the main code editor's scroll bar */
  box-shadow: var(--shadow-elevation-2);
  transition: width 0.2s ease-in-out;
  overflow: hidden;

  & .subtitle {
    color: var(--knime-black);
    margin-top: var(--space-8);
    font-style: italic;
    display: flex;
    justify-content: flex-start;
    align-items: baseline;
    flex-direction: row;

    & .text {
      margin-right: var(--space-4);
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

  & .ai-suggestion {
    margin: var(--ai-bar-margin);
    min-height: 200px;
    height: 40vh;
  }

  & .chat-controls-border-top {
    border-top: 1px solid var(--knime-porcelain);
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
      overflow-wrap: break-word;
      max-height: 100px;
      text-overflow: ellipsis;
      overflow: hidden;
      line-clamp: 5; /* This adds an ellipsis at the end of the 5th line. Is the new standard but not well supported yet */
      display: -webkit-box; /* For older browsers including our CEF we need to use these -webkit properties */
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 5;
    }

    & .error-message {
      color: var(--knime-coral-dark);
      z-index: 12;
      margin: 0 var(--ai-bar-margin) var(--ai-bar-margin) var(--ai-bar-margin);
      line-height: 15.23px;
      display: flex;
      align-items: center;
      flex-wrap: nowrap;

      & .error-icon {
        stroke: var(--knime-coral-dark);
        width: 13px;
        min-width: 13px;
        height: 13px;
        margin-right: var(--space-8);
      }

      & .error-text {
        white-space: normal;
        overflow-wrap: break-word;
        font-size: 12px;
      }
    }

    & .usage-limit {
      display: flex;
      flex-direction: row;
      align-items: center;
      height: 10px;
      margin: 0 var(--ai-bar-margin) var(--ai-bar-margin) var(--ai-bar-margin);
      font-weight: 500;
      font-size: 10px;
      flex-grow: 0;

      /* Ensure usage text appears above the arrow (z-index: 1 in AiButton.vue) */
      position: relative;
      z-index: 2;

      & .usage-disclaimer {
        margin-left: auto;
      }
    }

    & .limit-exceeded-message {
      margin: var(--ai-bar-margin);
    }

    & .chat-controls-text-input {
      display: flex;
      flex-flow: row nowrap;
      align-items: center;
      z-index: 12;

      & .textarea {
        flex-grow: 1;
        border: 2px solid var(--knime-porcelain);
        resize: none;
        border-radius: 0;
        padding: var(--space-8);
        margin: var(--ai-bar-margin);
        color: var(--knime-masala);
        font-size: 13px;
        font-weight: lighter;
        font-family: Roboto, sans-serif;
        overflow: hidden auto;
        max-height: 100px;

        &::placeholder {
          color: var(--knime-dove-gray);
        }

        &:disabled {
          opacity: 0.5;
        }

        &:focus {
          outline: none;
        }
      }

      & .chat-controls-buttons {
        margin: var(--ai-bar-margin);
        margin-left: 0;
      }
    }
  }

  & .notification-bar {
    display: flex;
    justify-content: space-between;
    vertical-align: middle;
    border-top: 1px solid var(--knime-porcelain);
    position: relative;
    height: 49px;
  }
}

.notification {
  line-height: 15.23px;
  margin: var(--space-16);
}

.notification-button {
  height: 30px;
  margin: var(--space-8);
  margin-right: var(--space-16);
}

.abort-button {
  border: 1px solid var(--knime-silver-sand);
}
</style>
