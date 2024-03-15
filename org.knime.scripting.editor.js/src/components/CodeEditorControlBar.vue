<script setup lang="ts">
import { nextTick, onMounted, ref, type PropType } from "vue";
import Button from "webapps-common/ui/components/Button.vue";
import AiCode from "webapps-common/ui/assets/img/icons/ai-general.svg";
import AiBar from "./ai-assistant/AiBar.vue";
import { getScriptingService } from "@/scripting-service";
import { onClickOutside } from "@vueuse/core";

import type { PaneSizes } from "@/components/utils/paneSizes";

const showBar = ref<boolean>(false);
const aiBar = ref(null);
const aiButton = ref(null);

const showAiButton = ref<boolean>(false);
const enableAiButton = ref<boolean>(false);

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

const setupOnClickOutside = () => {
  const splitters = [...document.querySelectorAll(".splitpanes__splitter")].map(
    (splitter) => splitter as HTMLElement,
  );
  onClickOutside(
    aiBar,
    () => {
      if (showBar.value) {
        showBar.value = !showBar.value;
      }
    },
    { ignore: [aiButton, ...splitters] },
  );
};

onMounted(async () => {
  showAiButton.value = await getScriptingService().isCodeAssistantEnabled();
  enableAiButton.value = await getScriptingService().inputsAvailable();
  nextTick(() => {
    setupOnClickOutside();
  });
});
</script>

<template>
  <div v-if="showBar" class="ai-bar">
    <AiBar
      ref="aiBar"
      :current-pane-sizes="props.currentPaneSizes"
      :language="language"
      @accept-suggestion="showBar = false"
      @close-ai-bar="showBar = false"
    />
  </div>
  <div class="controls">
    <div>
      <Button
        v-if="showAiButton"
        ref="aiButton"
        class="ai-button"
        :disabled="!enableAiButton"
        compact
        :with-border="true"
        :class="{ 'button-active': showBar }"
        @click="showBar = !showBar"
      >
        <AiCode viewBox="0 0 32 32" /> Ask K-AI
      </Button>
    </div>
    <div class="button-controls">
      <slot name="controls" />
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.ai-bar {
  flex: 0 0 auto; /* Let the element ignore its size and not grow or shrink */
}

.controls {
  display: flex;
  place-content: center space-between;
  align-items: center;
  padding: 0 10px;
  background-color: var(--knime-gray-light-semi);
  border-top: 1px solid var(--knime-silver-sand);
  height: var(--controls-height);
  width: 100%;
}

.button-controls {
  display: flex;
  justify-content: right;
}

.button-active {
  &.compact {
    color: var(--knime-white);
    background-color: var(--knime-masala);

    & svg {
      stroke: var(--knime-white);
    }
  }
}
</style>
