<script lang="ts">
import type { PropType } from 'vue';
import { defineComponent } from 'vue';
import TabBar from 'webapps-common/ui/components/TabBar.vue';

export type TabOption = { value: string; label: string; title?: string; icon?: string; disabled?: boolean };

export default defineComponent({
    name: 'TabPane',
    components: { TabBar },
    props: {
        name: {
            type: String,
            default: ''
        },
        initialTab: {
            type: String,
            default: ''
        },
        tabs: {
            type: Array as PropType<TabOption[]>,
            default() {
                return [];
            }
        }
    },
    data() {
        return {
            activeTab: this.initialTab
        };
    }
});
</script>

<template>
  <div class="tab-pane">
    <TabBar
      v-model:modelValue="activeTab"
      :name="name"
      :possible-values="tabs"
    />

    <slot :active-tab="activeTab" />
  </div>
</template>

<style lang="postcss" scoped>
.tab-pane{
    overflow: hidden;
    height: 100%;
    flex: 1 2 200px;
}
</style>
