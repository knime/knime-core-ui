<script setup lang="ts" generic="SettingValue extends Stringifyable">
import { computed, onMounted, ref } from "vue";
import { set } from "lodash-es";
import { v4 as uuidv4 } from "uuid";

import { FunctionButton, LoadingIcon } from "@knime/components";
import type { VueControlProps } from "@knime/jsonforms";

import type { Result as ResultOfType } from "../api/types/Result";
import { type Stringifyable } from "../composables/components/JsonSettingsComparator";
import type { SettingsData } from "../types/SettingsData";
import getFlattenedSettings from "../utils/getFlattenedSettings";
import inject from "../utils/inject";

type Id = string; // NOSONAR intended type alias
interface State {
  id: Id;
  nextState: Id;
  disabled: boolean;
  primary: boolean;
  text: string;
}

interface ButtonChange {
  settingValue: SettingValue;
  setSettingValue: boolean;
  buttonState: Id;
}

type Result = ResultOfType<ButtonChange>;

const registerWatcher = inject("registerWatcher");
const getData = inject("getData") as (params: {
  method?: string;
  options?: unknown[];
}) => Promise<Result>;

const applyData = inject("applyData") as
  | undefined
  | ((dataTransformer: (data: any) => void) => Promise<void>);

const props = defineProps<VueControlProps<any>>();

const errorMessage = ref(null as null | string);
const clearError = () => {
  errorMessage.value = null;
};

const currentSettings = ref({});
const saveCurrentSettings = (newSettings: SettingsData) => {
  currentSettings.value = getFlattenedSettings(newSettings);
};

const currentState = ref({} as State);
const states = computed(
  () => (props.control.uischema.options?.states as State[]) ?? [],
);

const setButtonState = (newButtonStateId: Id) => {
  clearError();
  currentState.value = states.value.find(({ id }) => id === newButtonStateId)!;
};

const saveResult = (newVal: SettingValue) => {
  // without setTimeout, the value is not updated when triggered via onUpdate
  setTimeout(() => props.changeValue(newVal));
};

const setNextState = (dataServiceResult: ButtonChange) => {
  if (dataServiceResult === null) {
    return;
  }
  const { settingValue, setSettingValue, buttonState } = dataServiceResult;
  if (setSettingValue) {
    saveResult(settingValue);
  }
  setButtonState(buttonState);
};

const handleDataServiceResult = (
  receivedData: Result,
  resetCallback = () => {},
) => {
  const { state } = receivedData;
  if (state === "SUCCESS") {
    setNextState(receivedData.result);
    return;
  }
  if (state === "FAIL") {
    errorMessage.value = receivedData.message?.[0];
  }
  resetCallback();
};

const numPendingRequests = ref(0);
const widgetId = uuidv4();
const performRequest = async (
  {
    method,
    options,
    handler,
  }: {
    method:
      | "settings.initializeButton"
      | "settings.update"
      | "settings.invokeButtonAction";
    options: any[];
    handler: string;
  },
  resetCallback = () => {},
) => {
  numPendingRequests.value += 1;
  const receivedData = await getData({
    method,
    options: [widgetId, handler, ...options],
  });
  numPendingRequests.value -= 1;
  handleDataServiceResult(receivedData, resetCallback);
};

const initialize = async (newSettings: SettingsData) => {
  saveCurrentSettings(newSettings);
  await performRequest({
    method: "settings.initializeButton",
    options: [props.control.data],
    handler: props.control.uischema.options!.actionHandler,
  });
};

const onUpdate = (dependencySettings: SettingsData) => {
  performRequest({
    method: "settings.update",
    options: [getFlattenedSettings(dependencySettings)],
    handler: props.control.uischema.options!.updateOptions.updateHandler,
  });
};

const onClick = async () => {
  if (applyData && props.control.uischema.options?.incrementAndApplyOnClick) {
    applyData((data) => set(data, props.control.path, props.control.data + 1));
    return;
  }

  const { id, nextState } = currentState.value;
  const lastSuccessfulState = currentState.value;
  const resetCallback = () => {
    if (nextState === currentState.value.id) {
      currentState.value = lastSuccessfulState;
    }
  };
  if (typeof nextState !== "undefined") {
    setButtonState(nextState);
  }
  await performRequest(
    {
      method: "settings.invokeButtonAction",
      options: [id, currentSettings.value],
      handler: props.control.uischema.options!.actionHandler,
    },
    resetCallback,
  );
};

onMounted(() => {
  registerWatcher({
    init: initialize,
    transformSettings: saveCurrentSettings,
    dependencies: props.control.uischema.options?.dependencies || [],
  });
  const updateOptions = props.control.uischema?.options?.updateOptions;
  if (typeof updateOptions !== "undefined") {
    registerWatcher({
      transformSettings: onUpdate,
      dependencies: updateOptions.dependencies,
    });
  }
});

const displayErrorMessage = computed(
  () => props.control.uischema.options?.displayErrorMessage ?? true,
);
</script>

<template>
  <div class="button-wrapper">
    <FunctionButton
      :disabled="currentState.disabled"
      class="button-input"
      :primary="currentState.primary"
      compact
      @click="onClick"
    >
      <div class="button-input-text">{{ currentState.text }}</div>
    </FunctionButton>
    <LoadingIcon v-if="numPendingRequests > 0" />
    <span v-if="errorMessage && displayErrorMessage" class="error-message">
      Error: {{ errorMessage }}
    </span>
  </div>
</template>

<style scoped>
.button-input {
  min-width: 100px;
  text-align: center;
}

.button-wrapper {
  display: flex;
  align-items: center;
  justify-content: left;
  gap: 10px;
}

.button-input-text {
  display: flex;
  justify-content: center;
  width: 100%;
}

.error-message {
  font-size: 13px;
  color: var(--knime-coral-dark);
}

svg {
  height: 18px;
}
</style>
