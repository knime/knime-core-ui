<script lang="ts">
/**
 * Component that contains the KAi disclaimer text and a button to accept it.
 * Saves quite a lot of lines of code in the AiPopupContent!
 */
</script>

<script setup lang="ts">
import { Button } from "@knime/components";

const emit = defineEmits(["accept-disclaimer"]);
</script>

<template>
  <div class="disclaimer-container">
    <div class="disclaimer-text">
      <p style="font-weight: bold">Disclaimer</p>
      <p>
        By using this coding assistant, you acknowledge and agree the following:
        Any information you enter into the prompt, as well as the current code
        (being edited) and table headers, may be shared with OpenAI and KNIME in
        order to provide and improve this service.
      </p>

      <p>
        KNIME is not responsible for any content, input or output, or actions
        triggered by the generated code, and is not liable for any damages
        arising from or related to your use of the coding assistant.
      </p>

      <p>This is an experimental service, USE AT YOUR OWN RISK.</p>
    </div>
    <div class="disclaimer-button-container">
      <Button
        compact
        primary
        class="notification-button"
        data-testid="ai-disclaimer-accept-button"
        @click="emit('accept-disclaimer')"
      >
        Accept and close
      </Button>
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.disclaimer-container {
  display: flex;
  flex-direction: column;

  & .disclaimer-text {
    margin: var(--space-8);
    line-height: 20px;
    padding: var(--space-4);
    background-color: var(--knime-white);
    border-radius: var(--ai-bar-corner-radius);
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
