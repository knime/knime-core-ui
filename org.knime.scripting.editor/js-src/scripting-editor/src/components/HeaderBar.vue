<script>
// TODO(AP-20076): Push functionanlity into knime-scripting-editor from PythonScriptingEditor

import { defineComponent } from 'vue';

export default defineComponent({ name: 'HeaderBar',
    components: {} });
</script>

<template>
  <div class="container">
    <slot name="buttons" />
  </div>
</template>


<style lang="postcss" scoped>

.container{
  height: var(--controls-height);
  background-color: var(--knime-porcelain);
  border-bottom: 2px solid var(--knime-silver-sand);
  display: flex;
  flex-wrap: wrap;
  flex-grow: 1 1;
}

:slotted(button){
  margin: 5px;
  flex: 0 1 150px;
  justify-content: center;

}

</style>
