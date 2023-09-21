<script setup lang="ts">
import { nextTick, onMounted, ref, type PropType } from "vue";
import Button from "webapps-common/ui/components/Button.vue";
import AiCode from "webapps-common/ui/assets/img/icons/ai-code.svg";
import AiBar from "./AiBar.vue";
import { getScriptingService } from "@/scripting-service";
import { onClickOutside } from "@vueuse/core";
import type { PaneSizes } from "./ScriptingEditor.vue";

const showBar = ref<boolean>(false);
const aiBar = ref(null);
const aiButton = ref(null);

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
      class="ai-button"
      :disabled="!inputsAvailable"
      compact
      on-dark
      @click="showBar = !showBar"
    >
      <AiCode viewBox="0 0 32 32" /> Ask AI
    </Button>
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
  justify-content: space-between;
  align-content: center;
  align-items: center;
  padding: 0 10px;
  background-color: var(--knime-gray-light-semi);
  border-top: 1px solid var(--knime-silver-sand);
  height: var(--controls-height);
}

.button-controls {
  display: flex;
  justify-content: right;
}

.button {
  font-weight: 500;

  &.compact {
    background-color: var(--knime-masala);
    color: var(--knime-white);

    &:hover,
    &:visited,
    &:focus,
    &:active {
      background-color: var(--knime-masala);
      color: var(--knime-white);
    }

    & svg {
      stroke: var(--knime-white);
      stroke-width: 1.5px;
      width: 18px;
      height: 18px;
    }
  }
}
</style>
