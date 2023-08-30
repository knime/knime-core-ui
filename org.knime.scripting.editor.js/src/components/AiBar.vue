<script setup lang="ts">
import { ref } from "vue";
import { useTextareaAutosize } from "@vueuse/core";
import SendIcon from "webapps-common/ui/assets/img/icons/paper-flier.svg";
import AbortIcon from "webapps-common/ui/assets/img/icons/circle-close.svg";
import Button from "webapps-common/ui/components/Button.vue";
import { getScriptingService } from "@/scripting-service";

export type KAIStatus = "idle" | "error" | "waiting";

interface Message {
  role: "reply" | "request";
  content: string;
}

const { textarea, input } = useTextareaAutosize();

const emit = defineEmits<{
  (e: "accept-suggestion", value: boolean): void;
}>();

const status = ref<KAIStatus>("idle" as KAIStatus);
let message: Message | null = null;
let history: Array<Message | null> = []; // maybe define as props
let showButtons = false;
const scriptingService = getScriptingService();
let responseHandlerRegistered = false;

const abortRequest = () => {
  scriptingService?.sendToService("abortRequest");
  // this should be handled in the response of the eventhandler
  status.value = "error";
};

const acceptSuggestion = () => {
  history.push(message);
  status.value = "idle";
  emit("accept-suggestion", true);
};
const discardSuggestion = () => {
  // don't push to history
  status.value = "idle";
  emit("accept-suggestion", false);
};

export type CodeSuggestion = {
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
    let newCode = JSON.parse(codeSuggestion.code).code;
    scriptingService.setScript(newCode);
    // here pend for discard/accept
    status.value = "idle";
    history.push({ role: "request", content: input.value });
    input.value = "";
  }
};

const registerResponseHandler = () => {
  if (responseHandlerRegistered) {
    return;
  }

  getScriptingService().registerEventHandler(
    "codeSuggestion",
    handleCodeSuggestion,
  );
  responseHandlerRegistered = true;
};

const request = async () => {
  status.value = "waiting";

  registerResponseHandler();

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
</script>

<template>
  <div class="chat-controls">
    <textarea
      ref="textarea"
      v-model="input"
      :readonly="status === 'waiting'"
      class="textarea"
      placeholder="Ask KAi here."
      @keydown="handleKeyDown"
    />
    <Button
      v-show="status === 'error' || status === 'idle'"
      ref="sendButton"
      title="Send"
      :disabled="!input"
      @click="request"
    >
      <SendIcon class="icon" />
    </Button>
    <Button
      v-show="status === 'waiting'"
      ref="abortButton"
      title="Cancel"
      @click="abortRequest"
    >
      <AbortIcon class="icon" />
    </Button>

    <div class="subtitle">
      <div class="text">{{ status }}</div>
      <div v-if="status === 'waiting'" class="dot" />
      <div v-if="status === 'waiting'" class="dot" />
      <div v-if="status === 'waiting'" class="dot" />
      <Button
        v-show="showButtons"
        with-border
        compact
        :disabled="status === 'error' || status === 'waiting'"
        @click="discardSuggestion"
      >
        Discard
      </Button>
      <Button
        v-show="showButtons"
        primary
        with-border
        compact
        :disabled="status === 'error' || status === 'waiting'"
        @click="acceptSuggestion"
      >
        Accept
      </Button>
    </div>
    <div class="triangle" />
  </div>
</template>

<style lang="postcss" scoped>
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

.button {
  height: auto;
  width: auto;
  position: absolute;
  right: 10px;
  bottom: 44px;
  padding: 0;
  stroke-width: 5px;
  color: var(--knime-yellow);
  background-color: transparent;

  /* best way to ensure pill shaped buttons with flexible 1/4 corners */
  border-radius: var(--theme-button-border-radius, 9999px);

  & :slotted(svg) {
    align-content: center;
    width: 22px;
    height: 22px;
    stroke: var(--knime-yellow);
    stroke-width: 2px;
    position: relative;
    top: 0;
    vertical-align: middle;
  }
}

.chat-controls {
  display: flex;
  flex-direction: column;
  position: absolute;
  left: 0;
  bottom: 0;
  background-color: var(--knime-gray-light-semi);
  border: 1px solid var(--knime-gray-dark);
  font-weight: 299;
  line-height: 17px;
  padding: 9px;
  width: calc(100% - 38px);
  box-shadow: var(--shadow-elevation-3);
  margin-left: 19px;
  margin-right: 19px;
  margin-bottom: 35px;

  & .textarea {
    position: relative;
    overflow: hidden;
    border: 0 solid var(--knime-gray-dark);
    resize: none;
    border-radius: 0;
    bottom: -1;
    padding: 9px;

    &:focus {
      outline: none;
    }
  }

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
}

.triangle {
  position: absolute;
  width: 0;
  height: 0;
  left: 10px;
  bottom: -35px;
  border-left: 35px solid transparent; /* Half the width of the base */
  border-right: 0 solid transparent; /* Half the width of the base */
  border-bottom: 35px solid var(--knime-gray-light-semi);
  transform: rotate(180deg);
  box-shadow: var(--shadow-elevation-3);
}
</style>
