<script>
import { defineComponent } from 'vue';

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
    <HeaderBar>
      <template #buttons>
        <slot name="buttons" />
      </template>
    </HeaderBar>
    <div class="center">
      <LeftPane
        :expanded="isExpanded"
        @toggle-expand="isExpanded = !isExpanded"
      >
        <template #conda_env>
          <slot name="conda_env" />
        </template>
        <template #inputs>
          <slot name="inputs" />
        </template>
        <template #flow_vars>
          <slot name="flow_vars" />
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
              <slot name="editor" />
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
    <FooterBar />
  </div>
</template>

<style lang="postcss" scoped>

.layout{
  --controls-height: 49px;
  --description-button-size: 15px;

  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background-color: var(--knime-gray-ultra-light);
  border-left: 1px solid var(--knime-silver-sand);
  & .center{
    display: flex;
    height: calc(100vh - 2*var(--controls-height));
    width: 100%;
  }
}

</style>
