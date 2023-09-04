<script lang="ts">
export type InputOutputModel = {
  name: string;
  codeAlias?: string;
  subItems?: {
    name: string;
    type: string;
    codeAlias?: string;
  }[];
};
</script>

<script setup lang="ts">
import Collapser from "webapps-common/ui/components/Collapser.vue";
import { getScriptingService } from "@/scripting-service";
const props = defineProps<{
  inputOutputItem: InputOutputModel;
}>();

const handleClick = (event: any, codeAlias?: string) => {
  if (codeAlias) {
    getScriptingService().pasteToEditor(codeAlias);
    event.stopPropagation();
  }
};
</script>

<template>
  <Collapser v-if="inputOutputItem.subItems" class="collapser bottom-border">
    <template #title>
      <div class="top-card has-collapser">
        <div class="title">
          {{ inputOutputItem.name }}
        </div>
        <div
          v-if="inputOutputItem.codeAlias"
          class="code-alias"
          @click="($event) => handleClick($event, inputOutputItem.codeAlias)"
        >
          {{ inputOutputItem.codeAlias }}
        </div>
      </div>
    </template>
    <div v-if="props.inputOutputItem.subItems" class="collapser-content">
      <div
        v-for="subItem in props.inputOutputItem.subItems"
        :key="subItem.name"
        class="sub-item"
        :class="{ 'clickable-sub-item': subItem.codeAlias }"
        @click="($event) => handleClick($event, subItem.codeAlias)"
      >
        <div class="cell">{{ subItem.name }}</div>
        <div class="cell">{{ subItem.type }}</div>
      </div>
    </div>
  </Collapser>
  <div v-else class="top-card bottom-border">
    <div class="title">
      {{ inputOutputItem.name }}
    </div>
    <div
      v-if="inputOutputItem.codeAlias"
      class="code-alias"
      @click="($event) => handleClick($event, inputOutputItem.codeAlias)"
    >
      {{ inputOutputItem.codeAlias }}
    </div>
  </div>
</template>

<style scoped lang="postcss">
.top-card {
  min-height: 42px;
  background-color: var(--knime-porcelain);
  padding-left: 8px;
  position: relative;
  margin: 0;
  font-size: 13px;
  font-weight: bold;
  display: flex;
  flex-direction: row;
  justify-content: left;
  align-items: center;
  line-height: 26px;
  overflow: hidden;
}

.has-collapser {
  padding-right: 32px;
}

.title {
  flex-basis: 100px;
  min-width: 60px;
}

.collapser-content {
  font-size: 11px;
  width: 100%;
}

.sub-item {
  width: 100%;
  border-bottom: 1px solid var(--knime-porcelain);
  display: flex;
  flex-direction: row;
}

.clickable-sub-item {
  &:hover {
    background-color: var(--knime-cornflower-semi);
    cursor: pointer;
  }
}

.cell {
  padding: 10px;
  flex: 50%;
}

.code-alias {
  font-family: monospace;
  font-weight: normal;
  font-size: 12px;
  padding: 8px;
  text-overflow: ellipsis;
  overflow: hidden;
  flex-shrink: 10;

  &:hover {
    background-color: var(--knime-cornflower-semi);
    cursor: pointer;
  }
}

.collapser {
  & :deep(.dropdown) {
    width: 20px;
    height: 20px;
    top: 10px;

    & .dropdown-icon {
      width: 12px;
      height: 12px;
    }
  }
}

.bottom-border {
  border-bottom: 1px solid var(--knime-white);
}
</style>
