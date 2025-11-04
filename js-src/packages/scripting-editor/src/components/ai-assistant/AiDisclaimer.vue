<script lang="ts">
/**
 * Component that contains the KAi disclaimer text and a button to accept it.
 * Saves quite a lot of lines of code in the AiPopupContent!
 */
</script>

<script setup lang="ts">
import { onMounted, ref } from "vue";

import { Button } from "@knime/components";

import { getScriptingService } from "@s/init";
import InfinityLoadingBar from "../InfinityLoadingBar.vue";

const disclaimerText = ref<string>();

onMounted(async () => {
  disclaimerText.value = await getScriptingService().getAiDisclaimer();
});

const emit = defineEmits(["accept-disclaimer"]);
</script>

<template>
  <div class="disclaimer-container">
    <template v-if="disclaimerText">
      <div class="disclaimer-box">
        <p style="font-weight: bold">Disclaimer</p>
        <p class="content">
          {{ disclaimerText }}
        </p>
      </div>
      <div class="disclaimer-button-container">
        <Button
          compact
          primary
          class="notification-button"
          data-testid="ai-disclaimer-accept-button"
          @click="emit('accept-disclaimer')"
        >
          Accept and continue
        </Button>
      </div>
    </template>
    <div v-else><InfinityLoadingBar /></div>
  </div>
</template>

<style lang="postcss" scoped>
.disclaimer-container {
  display: flex;
  flex-direction: column;

  & .disclaimer-box {
    margin: var(--space-8);
    line-height: 20px;
    padding: var(--space-4);
    background-color: var(--knime-white);
    border-radius: var(--ai-bar-corner-radius);

    & .content {
      overflow-wrap: break-word;
      white-space: pre-wrap;
    }
  }

  & .disclaimer-button-container {
    display: flex;
    justify-content: right;

    & > button {
      margin-top: 0;
    }
  }
}

.notification-button {
  height: 30px;
  margin: var(--space-8);
  margin-right: var(--space-16);
}

.disclaimer-slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
}

.disclaimer-slide-fade-enter-from,
.disclaimer-slide-fade-leave-to {
  transform: translateY(30px);
  opacity: 0;
}
</style>
