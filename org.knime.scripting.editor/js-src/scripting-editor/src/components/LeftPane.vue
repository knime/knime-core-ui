<script>

/*
  I need some type of Icon as key with KeepAlive component.
*/

import CubeIcon from 'webapps-common/ui/assets/img/icons/cube.svg';
import PlusIcon from 'webapps-common/ui/assets/img/icons/circle-plus.svg';
import LeftCollapsiblePanel from './LeftCollapsiblePanel.vue';
import Button from 'webapps-common/ui/components/Button.vue';

import { defineComponent } from 'vue';

const TABS = {
    INPUT: 'inputs',
    CONDA: 'conda_env',
    FLOWVARS: 'flow_vars'
};

/*
TODO: Make this component with dynamic slots so we can implement it from the parent's project.


  <transition-group
    tag="ul"
    :css="false"
    @before-enter="onBeforeEnter"
    @enter="onEnter"
    @leave="onLeave"
  >
    <PlusIcon class="icons" />
    <CubeIcon class="icons" />
  </transition-group>


like.
interface UiSlot

interface ComponentPublicInstance {
  $slots: { [name: string]: UiSlot }
}
*/

export default defineComponent({
    name: 'LeftPane',
    components: {
        LeftCollapsiblePanel,
        Button
    },
    props: {
        initialTab: {
            type: String,
            default: 'conda_env'
        }
    },
    data(props) {
        return {
            isExpanded: false,
            activeTab: props.initialTab || null
        };
    },
    computed: {
        sidebarSections() {
            return [
                { title: 'Input',
                    icon: CubeIcon,
                    isActive: this.isTabActive(TABS.INPUT),
                    isExpanded: this.isExpanded,
                    onClick: () => this.clickItem(TABS.INPUT) },
                { title: 'Conda Environment',
                    icon: PlusIcon,
                    isActive: this.isTabActive(TABS.CONDA),
                    isExpanded: this.isExpanded,
                    onClick: () => this.clickItem(TABS.CONDA) },

                { title: 'Flow Variables',
                    icon: PlusIcon,
                    isActive: this.isTabActive(TABS.FLOWVARS),
                    isExpanded: this.isExpanded,
                    onClick: () => this.clickItem(TABS.FLOWVARS) }
            ];
        }
    },
    methods: {
        isTabActive(tabName) {
            const activeTab = this.activeTab;
            return activeTab === tabName;
        },
        clickItem(tabName) {
            const isAlreadyActive = this.isTabActive(tabName);
            if (isAlreadyActive && this.isExpanded) {
                this.isExpanded = false;
                this.activeTab = null;
            } else {
                this.isExpanded = true;
                this.activeTab = tabName;
            }
        }
    }
});
</script>

<template>
  <nav class="nav-bar">
    <ul>
      <li
        v-for="section in sidebarSections"
        :key="section.title"
        @click="section.onClick"
      >
        <Button
          :key="section.title"
          :title="section.title"
          :class="{ active: section.isActive, expanded: section.isExpanded }"
        >
          <Component
            :is="section.icon"
            class="icon"
          />
        </Button>
      </li>
    </ul>
  </nav>
  <LeftCollapsiblePanel
    class="slide"
    :expanded="isExpanded"
    @toggle-expand="isExpanded = !isExpanded"
  >
    <div v-show="activeTab === 'conda_env'">
      <slot name="conda_env" />
    </div>
    <div v-show="activeTab === 'inputs'">
      <slot name="inputs" />
    </div>
    <div v-show="activeTab === 'flow_vars'">
      <slot name="flow_vars" />
    </div>
  </LeftCollapsiblePanel>
</template>

<style lang="postcss" scoped>


.slide{
  display: flex;
  flex: 0 0;
  justify-items: flex-start;
  justify-content: space-evenly;
}

.icon{
  width: auto;
  aspect-ratio: 1;
  left: 0;
  min-height: 50px;
  min-width: 100%;
}

.nav-bar{
  width: var(--app-side-bar-width);
  background-color: var(--knime-black);

  & ul {
    display: contents;

    & li {
      height: 50px;
      width: 50px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      background-color: var(--knime-silver-sand);
      border-bottom: 1px solid var(--knime-black);
      transition: background-color 150ms ease-out;

      &.active {
        background-color: var(--knime-porcelain);

        &.expanded {
          background-color: var(--knime-gray-ultra-light);
        }
      }

      &:hover {
        background-color: var(--knime-gray-ultra-light);
        cursor: pointer;

        & svg {
          stroke: var(--knime-masala);
        }
      }
    }
  }

  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex-direction: column;
}

.slide-leave-to {
  transition: transform 0.55s ease-in-out;
  transform: translateX(calc(50% + 10px)); /* extra 10px to hide box shadow when collapsed */
}
.slide-leave-to {
  transition: transform 0.55s ease-in-out;
  transform: translateX(calc(-100% + 10px)); /* extra 10px to hide box shadow when collapsed */
}

.list-move, /* apply transition to moving elements */
.list-enter-active,
.list-leave-active {
  transition: all 0.5s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* ensure leaving items are taken out of layout flow so that moving
   animations can be calculated correctly. */
.list-leave-active {
  position: absolute;
}

</style>
