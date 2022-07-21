<script lang="ts">
import Vue, { PropType } from 'vue';
import TabBar from '~/webapps-common/ui/components/TabBar.vue';

export type TabOption = { value: string; label: string; title?: string; icon?: string; disabled?: boolean };

export default Vue.extend({
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
      :name="name"
      :value.sync="activeTab"
      :possible-values="tabs"
    />
    <slot :active-tab="activeTab" />
  </div>
</template>
