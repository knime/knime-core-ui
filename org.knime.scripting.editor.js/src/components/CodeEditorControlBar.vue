<script setup lang="ts">
import { nextTick, onMounted, ref, type PropType } from "vue";
import Button from "webapps-common/ui/components/Button.vue";
import AiButton from "webapps-common/ui/assets/img/icons/ai-brain.svg";

import AiBar from "./AiBar.vue";
import { getScriptingService } from "@/scripting-service";
import { onClickOutside } from "@vueuse/core";
import type { PaneSizes } from "./ScriptingEditor.vue";

const showBar = ref<boolean>(false);
const aiBar = ref(null);
const aiButton = ref(null);

const codeAssistantSupported = ref<boolean>(false);
const inputsAvailable = ref<boolean>(false);

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
  codeAssistantSupported.value =
    await getScriptingService().supportsCodeAssistant();
  inputsAvailable.value = await getScriptingService().inputsAvailable();
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
    <Button
      ref="aiButton"
      :disabled="!codeAssistantSupported || !inputsAvailable"
      compact
      on-dark
      class="ai-button"
      @click="showBar = !showBar"
    >
      <AiButton />
    </Button>
    <slot name="controls" class="button-controls" />
  </div>
</template>

<style lang="postcss" scoped>
.ai-bar {
  flex: 0 0 auto; /* Let the element ignore its size and not grow or shrink */
}

.controls {
  display: flex;
  justify-content: space-between;
  align-content: center;
  min-height: var(--controls-height);
  max-height: var(--controls-height);
  padding: 9px 20px;
  background-color: var(--knime-gray-light-semi);
  border-top: 1px solid var(--knime-silver-sand);
}

.button-controls {
  display: flex;
  justify-content: right;
}

.button {
  & :slotted(svg) {
    stroke: var(--knime-porcelain);
  }
}

.ai-button {
  border: none;
  border-radius: 0;
  margin: -9px -20px;
  height: 49px;
  width: 49px;

  &.compact {
    background-color: var(--knime-masala);
  }

  & svg {
    stroke: var(--knime-porcelain);
    transform: scale(1.3);
    margin: 0;
  }

  &.on-dark {
    &:active,
    &:hover,
    &:focus {
      background-color: var(--knime-masala);

      & svg {
        stroke: var(--knime-porcelain);
        transform: scale(1.5);
      }
    }
  }
}
</style>
