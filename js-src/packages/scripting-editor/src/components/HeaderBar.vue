<script lang="ts">
import { defineComponent } from "vue";
import type { PropType } from "vue";

import { type MenuItem, SubMenu } from "@knime/components";
import MenuIcon from "@knime/styles/img/icons/menu-options.svg";

import type { SettingsMenuItem } from "./SettingsPage.vue";

export default defineComponent({
  name: "HeaderBar",
  components: { SubMenu, MenuIcon },
  props: {
    title: {
      type: String,
      default: null,
    },
    menuItems: {
      type: Array as PropType<MenuItem[]>,
      required: true,
    },
  },
  emits: ["menu-item-click"],
  computed: {
    hasMenu() {
      return !(this.menuItems === null || this.menuItems.length === 0);
    },
  },
  methods: {
    menuItemClicked(event: Event, item: SettingsMenuItem) {
      this.$emit("menu-item-click", { event, item });
    },
  },
});
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
