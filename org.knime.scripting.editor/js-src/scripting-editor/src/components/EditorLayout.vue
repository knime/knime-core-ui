<script>
import { defineComponent } from 'vue';

// import SideDrawer from 'webapps-common/ui/components/SideDrawer.vue';
import HeaderBar from './HeaderBar.vue';
import FooterBar from './FooterBar.vue';

import Splitter from './Splitter.vue';
import LeftPane from './LeftPane.vue';

export default defineComponent({
    name: 'Layout',
    components: {
        LeftPane,
        HeaderBar,
        FooterBar,
        Splitter
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
      <LeftPane
        :expanded="isExpanded"
        class="left-pane"
        @toggle-expand="isExpanded = !isExpanded"
      >
        <template #conda_env>
          <slot name="conda_env" />
        </template>
        <template #inputs>
          <slot name="inputs" />
        </template>
      </LeftPane>
      <Splitter
        id="1"
        direction="row"
        secondary-size="10%"
      >
        <template #default>
          <Splitter
            id="2"
            direction="column"
            secondary-size="40%"
          >
            <template #default>
              <div class="editor">
                <slot name="editor" />
              </div>
            </template>
            <template #secondary>
              <slot name="bottom" />
            </template>
          </Splitter>
        </template>
        <template #secondary>
          <div class="right-pane">
            <slot name="right-pane" />
          </div>
        </template>
      </Splitter>
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

.left-pane{
  display: flex;
  flex: 1 1;
}

.slide{
  width: auto;
  flex: 0 1;
}

.center{
  display: flex;
  width: 100%;
  flex-wrap: nowrap;
  justify-content: stretch;
  flex: 1;
  height: 90%;
}

.splitter{
  height: 100%;
  width: 100%;
  display: flex;
  justify-content: flex-start;
  flex-direction: column;
  flex: 1;

}

.editor{
  height: 100%;
  width: 99%;
  display: flex;
  flex: 1;
}

.right-pane{
  min-width: 150px;
}

</style>
