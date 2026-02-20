<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { v4 as uuidv4 } from "uuid";

import { FunctionButton, LoadingIcon } from "@knime/components";
import type { VueControlProps } from "@knime/jsonforms";

import type { Result as ResultOfType } from "../api/types/Result";
import inject from "../utils/inject";

interface RestCallButtonData {
  message: string;
}

interface ButtonChange {
  settingValue?: RestCallButtonData;
  setSettingValue?: boolean;
  buttonState: string;
}

type Result = ResultOfType<ButtonChange>;

const registerWatcher = inject("registerWatcher");
const getData = inject("getData") as (params: {
  method?: string;
  options?: unknown[];
}) => Promise<Result>;

const props = defineProps<VueControlProps<RestCallButtonData>>();

const errorMessage = ref(null as null | string);
const isLoading = ref(false);
const currentMessage = ref(props.control.data?.message || "");
const widgetId = uuidv4();
const currentSettings = ref({} as any);

const buttonText = computed(
  () => props.control.uischema.options?.buttonText || "Execute",
);

const showLoadingIndicator = computed(
  () => props.control.uischema.options?.showLoadingIndicator ?? true,
);

const updateMessage = (newMessage: string) => {
  currentMessage.value = newMessage;
  props.changeValue({ message: newMessage });
};

const saveCurrentSettings = (newSettings: any) => {
  // Flatten the settings to pass as dependencies
  const getFlattenedSettings = (settings: any): any => {
    if (!settings) {
      return {};
    }
    const flattened: any = {};
    for (const key in settings) {
      if (Object.prototype.hasOwnProperty.call(settings, key)) {
        const value = settings[key];
        if (value && typeof value === "object" && !Array.isArray(value)) {
          // Recursively flatten nested objects
          const nested = getFlattenedSettings(value);
          Object.assign(flattened, nested);
        } else {
          flattened[key] = value;
        }
      }
    }
    return flattened;
  };
  currentSettings.value = getFlattenedSettings(newSettings);
};

const onClick = async () => {
  errorMessage.value = null;
  isLoading.value = true;

  try {
    const receivedData = await getData({
      method: "settings.invokeButtonAction",
      options: [
        widgetId,
        props.control.uischema.options!.actionHandler,
        "READY", // Current button state
        currentSettings.value, // Pass dependencies
      ],
    });

    if (receivedData?.state === "SUCCESS") {
      // receivedData.result is a ButtonChange object
      const buttonChange = receivedData.result;
      if (buttonChange?.setSettingValue && buttonChange.settingValue?.message) {
        updateMessage(buttonChange.settingValue.message);
      }
    } else if (receivedData?.state === "FAIL") {
      errorMessage.value = receivedData.message?.[0] || "Action failed";
    }
  } catch (error) {
    errorMessage.value = `Error: ${error}`;
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  registerWatcher({
    init: (newSettings: any) => {
      // Initialize with current value and save dependencies
      if (props.control.data?.message) {
        currentMessage.value = props.control.data.message;
      }
      saveCurrentSettings(newSettings);
    },
    transformSettings: saveCurrentSettings,
    dependencies: props.control.uischema.options?.dependencies || [],
  });
});
</script>

<template>
  <div class="rest-call-button-widget">
    <div class="button-row">
      <FunctionButton
        :disabled="isLoading"
        class="rest-call-button"
        primary
        compact
        @click="onClick"
      >
        {{ buttonText }}
      </FunctionButton>
      <LoadingIcon
        v-if="showLoadingIndicator && isLoading"
        class="loading-icon"
      />
    </div>
    <div class="message-display">
      <textarea
        :value="currentMessage"
        :disabled="true"
        placeholder="Click the button above to execute the action"
        rows="6"
        class="rest-call-textarea"
      />
    </div>
    <span v-if="errorMessage" class="error-message">
      Error: {{ errorMessage }}
    </span>
  </div>
</template>

<style scoped>
.rest-call-button-widget {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.button-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rest-call-button {
  min-width: 100px;
}

.loading-icon {
  width: 20px;
  height: 20px;
}

.message-display {
  width: 100%;
}

.rest-call-textarea {
  width: 100%;
  resize: vertical;
  padding: 8px;
  border: 1px solid var(--knime-stone-gray);
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.875rem;
  background-color: var(--knime-white);
  color: var(--knime-masala);
}

.rest-call-textarea:disabled {
  background-color: var(--knime-silver);
  cursor: not-allowed;
}

.error-message {
  color: var(--theme-color-error);
  font-size: 0.875rem;
}
</style>
