<script setup lang="ts">
import { type Ref, computed } from "vue";
import { DispatchRenderer } from "@jsonforms/vue";

import { FunctionButton } from "@knime/components";
import {
  SettingsSubPanel,
  VerticalLayoutBase,
  type VueLayoutProps,
  useProvidedState,
} from "@knime/jsonforms";
import SettingsIcon from "@knime/styles/img/icons/settings.svg";

const props = defineProps<VueLayoutProps>();
const control = computed(() => props.layout.uischema.control);
const subParameters = computed(() => props.layout.uischema.elements);

const showSubParameters = useProvidedState(
  control as Ref<
    {
      options: {
        showSubParameters?: boolean;
      };
      providedOptions: "showSubParameters"[];
    } & ({ scope: string } | { id: string })
  >,
  "showSubParameters",
  true,
);
</script>

<template>
  <div class="control-with-sub-parameters">
    <DispatchRenderer
      :key="`${layout.path}-control`"
      :schema="layout.schema"
      :uischema="control"
      :path="layout.path"
      :enabled="layout.enabled"
      :renderers="layout.renderers"
      :cells="layout.cells"
    />
    <SettingsSubPanel
      v-if="showSubParameters"
      background-color-override="var(--knime-gray-ultra-light)"
    >
      <template #expand-button="{ expand }">
        <FunctionButton
          class="settings-button"
          :disabled="!layout.enabled"
          @click="expand"
        >
          <SettingsIcon />
        </FunctionButton>
      </template>
      <template #default>
        <VerticalLayoutBase
          #default="{ element, index }"
          class="sub-parameters"
          :elements="subParameters"
        >
          <DispatchRenderer
            :key="`${layout.path}-${index}`"
            :schema="layout.schema"
            :uischema="element"
            :path="layout.path"
            :enabled="layout.enabled"
            :renderers="layout.renderers"
            :cells="layout.cells"
          />
        </VerticalLayoutBase>
      </template>
    </SettingsSubPanel>
  </div>
</template>

<style scoped lang="postcss">
.control-with-sub-parameters {
  display: flex;
  align-items: flex-end;
  gap: var(--space-8);

  & > :first-child {
    flex-grow: 1;
  }

  & > .settings-button {
    align-self: flex-end;
  }
}
</style>
