<script setup lang="ts">
import { computed, ref, watch } from "vue";

import { InputField } from "@knime/components";

import { type FlowSettings } from "../../api/types";
import useProvidedState from "../../composables/components/useProvidedState";
import type { Control } from "../../types/Control";
import { mergeDeep } from "../../utils";

import type { Credentials } from "./types/Credentials";

const props = defineProps<{
  /**
   * jsonforms control of this component.
   * The data are not used but instead passed via the data prop.
   */
  control: Control;
  /**
   * The data prop
   */
  data: Credentials;
  /**
   * The associated flow settings
   */
  flowSettings: FlowSettings | null;
  disabled: boolean;
  labelForId?: string;
  isValid: boolean;
}>();

const emit = defineEmits<{
  change: [Credentials];
}>();

const getDefaultData = (): Credentials => ({
  password: "",
  secondFactor: "",
  username: "",
});

const data = computed(() => {
  return props.data ?? getDefaultData();
});

const onChangeUsername = (username: string) => {
  emit("change", mergeDeep(data.value, { username }));
};
const onChangePassword = (password: string) => {
  emit("change", mergeDeep(data.value, { password, isHiddenPassword: false }));
};
const onChangeSecondFactor = (secondFactor: string) => {
  emit(
    "change",
    mergeDeep(data.value, { secondFactor, isHiddenSecondFactor: false }),
  );
};

const hiddenPassword = "*****************";
const displayedPassword = computed(() => {
  return data.value.isHiddenPassword ? hiddenPassword : data.value.password;
});
const displayedSecondFactor = computed(() => {
  return data.value.isHiddenSecondFactor
    ? hiddenPassword
    : data.value.secondFactor;
});

const options = computed(() => props.control.uischema.options ?? {});

// Username visibility
const hasUsername = useProvidedState(
  computed(() => options.value.hasUsernameProvider),
  true,
);
const hideUsername = computed(() => options.value.hideUsername ?? false);
const showUsername = computed(() => hasUsername.value && !hideUsername.value);

// Password visibility
const hasPassword = useProvidedState(
  computed(() => options.value.hasPasswordProvider),
  true,
);
const hidePassword = computed(() => options.value.hidePassword ?? false);
const showPassword = computed(() => hasPassword.value && !hidePassword.value);

const showSecondFactor = computed(
  () => showPassword.value && (options.value.showSecondFactor ?? false),
);
const usernameLabel = computed(
  () => options.value?.usernameLabel ?? "Username",
);
const passwordLabel = computed(
  () => options.value?.passwordLabel ?? "Password",
);
const secondFactorLabel = computed(
  () => options.value?.secondFactorLabel ?? "Second authentication factor",
);

// Flow variables

const controllingFlowVariableName = computed(
  () => props.flowSettings?.controllingFlowVariableName,
);
watch(
  () => controllingFlowVariableName.value,
  (flowVariableName) => flowVariableName || emit("change", getDefaultData()),
);
const controlElement = ref<null | HTMLElement>(null);
</script>

<template>
  <div
    :id="labelForId ?? undefined"
    ref="controlElement"
    class="credentials-input-wrapper"
  >
    <InputField
      v-if="showUsername"
      :placeholder="usernameLabel"
      :model-value="data.username"
      :disabled="disabled"
      :is-valid
      compact
      type="text"
      @update:model-value="onChangeUsername"
    />
    <InputField
      v-if="showPassword"
      :class="{ margin: showUsername }"
      :placeholder="passwordLabel"
      :model-value="displayedPassword"
      :disabled="disabled"
      :is-valid
      compact
      type="password"
      @update:model-value="onChangePassword"
    />
    <InputField
      v-if="showSecondFactor"
      :class="{ margin: showUsername || showPassword }"
      :placeholder="secondFactorLabel"
      :model-value="displayedSecondFactor"
      :disabled="disabled"
      :is-valid
      compact
      type="password"
      @update:model-value="onChangeSecondFactor"
    />
  </div>
</template>

<style lang="postcss" scoped>
.margin {
  margin-top: 10px;
}
</style>
