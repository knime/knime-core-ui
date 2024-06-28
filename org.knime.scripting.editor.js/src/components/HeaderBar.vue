<script lang="ts">
import { defineComponent } from "vue";
import type { PropType } from "vue";

import MenuIcon from "webapps-common/ui/assets/img/icons/menu-options.svg";
import type { MenuItem } from "webapps-common/ui/components/MenuItems.vue";
import SubMenu from "webapps-common/ui/components/SubMenu.vue";

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
    menuItemClicked(event: Event, item: any) {
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
  height: var(--controls-height);
  background-color: var(--knime-porcelain);
  border-bottom: 1px solid var(--knime-silver-sand);
  display: flex;
  flex-wrap: nowrap;
}

.title {
  margin-left: 10px;
  height: var(--controls-height);
  line-height: var(--controls-height);
  display: flex;
  align-items: center;
}

.open-icon {
  width: 25px;
  height: 50px;
  margin: auto;
}

.menu {
  margin: var(--space-4);
  margin-right: var(--space-8);
  flex: 0 1 1px;
  justify-content: center;
}
</style>
