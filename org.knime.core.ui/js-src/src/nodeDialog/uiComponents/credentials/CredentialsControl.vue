<script setup lang="ts">
import { computed } from "vue";

import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

import { useFlowSettings } from "../../composables/components/useFlowVariables";

import CredentialsControlBase from "./CredentialsControlBase.vue";
import type { Credentials } from "./types/Credentials";

const props = defineProps<VueControlPropsForLabelContent<Credentials>>();

const { flowSettings } = useFlowSettings({
  path: computed(() => props.control.path),
});
</script>

<template>
  <CredentialsControlBase
    :control="control"
    :data="control.data"
    :flow-settings="flowSettings"
    :disabled="disabled"
    :label-for-id="labelForId"
    @change="changeValue"
  />
</template>
