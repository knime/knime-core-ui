<script lang="ts">
/**
 * Component that includes the button that pops up the AI, and also holds the AI popup container.
 */
export default {};
</script>

<script setup lang="ts">
import AiCodeIcon from "@knime/styles/img/icons/ai-general.svg";
import AiPopupContent from "./AiPopupContent.vue";
import { Button } from "@knime/components";
import { ref, defineProps, type Ref, onMounted, computed } from "vue";
import type { PaneSizes } from "@/components/utils/paneSizes";
import { getInitialDataService } from "@/initial-data-service";
import { computedAsync, onClickOutside, type MaybeElement } from "@vueuse/core";

const showAiPopup = ref(false);

const showAiButton = computedAsync<boolean>(
  async () =>
    (await getInitialDataService().getInitialData()).kAiConfig
      .codeAssistantEnabled,
  false,
);
const enableAiButton = computedAsync<boolean>(
  async () => (await getInitialDataService().getInitialData()).inputsAvailable,
  false,
);

type AiButtonPropType = {
  currentPaneSizes: PaneSizes;
  showButtonText: boolean;
};

const aiBarPopupRef = ref<Element | null>(null);
const aiButtonRef = ref<Element | null>(null);

const props = defineProps<AiButtonPropType>();

const setupOnClickOutside = () => {
  const splitters = [
    ...document.querySelectorAll(".splitpanes__splitter"),
  ] as HTMLElement[];

  onClickOutside(
    aiBarPopupRef as Ref<MaybeElement>,
    () => {
      showAiPopup.value = false;
    },
    { ignore: [aiButtonRef as Ref<MaybeElement>, ...splitters] },
  );
};

const leftSplitterPosition = computed(() =>
  Math.max(props.currentPaneSizes.left, 0),
);

onMounted(() => {
  setupOnClickOutside();
});
</script>

<template>
  <div class="button-container">
    <div class="popup-anchor">
      <div
        v-if="showAiPopup"
        ref="aiBarPopupRef"
        class="ai-popup"
        data-testid="ai-popup"
        :style="{
          '--left-splitter-position': `${leftSplitterPosition}vw`,
        }"
      >
        <AiPopupContent @request-close="showAiPopup = false" />
        <div class="arrow" />
      </div>
    </div>
    <Button
      v-if="showAiButton"
      ref="aiButtonRef"
      :disabled="!enableAiButton"
      compact
      :with-border="true"
      class="ai-button"
      :class="{
        'button-active': showAiPopup,
        'hide-button-text': !showButtonText,
      }"
      @click="showAiPopup = !showAiPopup"
    >
      <AiCodeIcon viewBox="0 0 32 32" /> {{ showButtonText ? "Ask K-AI" : "" }}
    </Button>
  </div>
</template>

<style lang="postcss" scoped>
.popup-anchor {
  position: relative;
  display: flex;
  width: 100%;

  & .ai-popup {
    --default-ai-bar-width: 65vw;
    --arrow-size: 18px;

    z-index: 10;

    /* Amount by which the left edge of prompt is left of the centre of AI button. */
    --left-hang: 150px;

    /* Correcting term for when the InputOutputPane is too small to allow the preferred left hang. */
    --left-hang-correction-for-left-pane: max(
      0px,
      calc(var(--left-hang) - var(--left-splitter-position))
    );

    /* Additional correcting term so that arrow never falls off the edge of the prompt.
    We shift the prompt left by this amount and then the arrow right by this amount. */
    --left-hang-correction-for-arrow: var(--arrow-size);

    width: var(--default-ai-bar-width);
    max-width: 1000px;
    position: absolute;
    left: calc(50%); /* put the left edge at the middle of the AI button */
    top: calc(-1 * (var(--arrow-size) + 2px));
    transform: translateY(-100%)
      translateX(
        calc(
          var(--left-hang-correction-for-left-pane) - var(--left-hang) -
            var(--left-hang-correction-for-arrow)
        )
      );

    & .arrow {
      width: var(--arrow-size);
      height: var(--arrow-size);
      content: "";
      position: absolute;
      background-color: var(--knime-gray-ultra-light);
      bottom: 0;
      z-index: 1;
      border-right: 1px solid var(--knime-porcelain);
      border-top: 1px solid var(--knime-porcelain);

      /* we clip the arrow to stop it casting a shadow on the rest of the popup.
      Note that at the time of writing, the CEF version doesn't support many of
      the shapes that would make this easier. */
      clip-path: polygon(
        0 calc(0px - 100vw),
        calc(100% + 100vw) calc(0px - 100vw),
        calc(100% + 100vw) 100%,
        0 100%
      );
      box-shadow: var(--shadow-elevation-2);
      transform: translateX(
          calc(
            var(--left-hang) - var(--arrow-size) / 2 -
              var(--left-hang-correction-for-left-pane) +
              var(--left-hang-correction-for-arrow)
          )
        )
        translateY(50%) rotate(135deg);
    }
  }
}
</style>
