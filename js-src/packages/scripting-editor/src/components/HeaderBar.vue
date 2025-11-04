<script setup lang="ts">
import { computed } from "vue";

import { type MenuItem, SubMenu } from "@knime/components";
import MenuIcon from "@knime/styles/img/icons/menu-options.svg";

import type { SettingsMenuItem } from "./SettingsPage.vue";

interface Props {
  title?: string | null;
  menuItems: MenuItem[];
}

interface Emits {
  (
    e: "menu-item-click",
    payload: { event: Event; item: SettingsMenuItem },
  ): void;
}

const { title = null, menuItems } = defineProps<Props>();

const emit = defineEmits<Emits>();

const hasMenu = computed(() => menuItems !== null && menuItems.length > 0);

const menuItemClicked = (event: Event, item: SettingsMenuItem) => {
  emit("menu-item-click", { event, item });
};
</script>

<template>
  <div class="container">
    <div class="title">{{ title }}</div>
    <div v-if="hasMenu" class="menu">
      <SubMenu :items="menuItems" @item-click="menuItemClicked">
        <MenuIcon class="open-icon" />
      </SubMenu>
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.container {
  justify-content: space-between;
  background-color: var(--knime-porcelain);
  border-bottom: 1px solid var(--knime-silver-sand);
  display: flex;
  flex-wrap: nowrap;
  height: 38px;
}

.title {
  margin-left: 10px;
  display: flex;
  align-items: center;
}

.open-icon {
  width: 18px;
  height: 18px;
  margin: auto;
}

.menu {
  margin: var(--space-4);
  margin-right: var(--space-8);
  flex: 0 1 1px;
  justify-content: center;
}
</style>
