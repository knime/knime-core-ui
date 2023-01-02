<script>
import { defineComponent } from 'vue';

// import SideDrawer from 'webapps-common/ui/components/SideDrawer.vue';
import HeaderBar from './HeaderBar.vue';
import FooterBar from './FooterBar.vue';
import LeftCollapsiblePanel from './LeftCollapsiblePanel.vue';
import Splitter from './Splitter.vue';

export default defineComponent({
    name: 'Layout',
    components: {
        HeaderBar,
        FooterBar,
        Splitter,
        LeftCollapsiblePanel
    },
    data() {
        return {
            isExpanded: false
        };
    }
});
</script>

<template>
  <div class="layout">
    <HeaderBar
      :is-expanded="isExpanded"
      @switch="isExpanded = !isExpanded"
    />
    <div class="center">
      <LeftCollapsiblePanel
        class="slide"
        :expanded="isExpanded"
        @toggle-expand="isExpanded = !isExpanded"
      >
        <slot name="left-pane" />
      </LeftCollapsiblePanel>
      <div class="mid-pane">
        <Splitter>
          <template #default>
            <div class="editor">
              <slot name="editor" />
            </div>
          </template>
          <template #secondary>
            <slot name="bottom" />
          </template>
        </Splitter>
      </div>
      <slot name="right-pane" />
    </div>
    <FooterBar class="grid-item" />
  </div>
</template>

<style lang="postcss" scoped>
.layout{
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  height: 100vh;
  width: 100%;
}

.slide{
  position: relative;
  left:0;
  width: auto;
  margin-bottom: 5px;
  flex: 0 1 100px;
}

.center{
  display: flex;
  width: 100%;
  flex-wrap: nowrap;
  justify-content: space-between;
  height: 90%;
}
.editor{
  height: 100%;
  width: 100%;
  display: flex;
}
.mid-pane{
  height: 100%;
  width: 75%;
  display: flex;
  justify-content: flex-start;
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

</style>
