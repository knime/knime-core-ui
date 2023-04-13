<script lang="ts">
// TODO(AP-20076): Push functionanlity into knime-scripting-editor from PythonScriptingEditor

import { defineComponent } from 'vue';

export type ExecutionStatus = {
  status: string,
  isRunning: boolean,
  isSuccess: boolean,
  isIdle: boolean,
  isError: boolean
};

export default defineComponent<ExecutionStatus>({ name: 'HeaderBar',
    components: {},
    inject: ['executionStatus'],
    data() {
        return { status: this.executionStatus };
    },
    computed: {
        isRunning(): boolean {
            return this.status === 'running';
        },
        isIdle(): boolean {
            return this.status === 'idle';
        },
        isSuccess(): boolean {
            return this.status === 'success';
        },
        isError(): boolean {
            return this.status === 'error';
        },
    } });
</script>

<template>
  <div class="container">
    <div class="buttons">
      <slot name="buttons" />
    </div>
    <div class="info">
      <div
        class="circle"
        :class="{ success: isSuccess, idle: isIdle, running: isRunning, error: isError }"
      />
      <b>{{ status }}</b>
    </div>
  </div>
</template>


<style lang="postcss" scoped>

.info{
  margin: 10px;
  display: flex;
  flex: 0 1 50px;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: center;
}
.circle{
  margin-right: 10px;
  width: 15px;
  height: 15px;
  aspect-ratio: 1;
  border-radius: 50%;
  background-color: aqua;
}
.container{
  justify-content: space-between;
  height: var(--controls-height);
  background-color: var(--knime-porcelain);
  border-bottom: 2px solid var(--knime-silver-sand);
  display: flex;
  flex-wrap: nowrap;
  flex-grow: 1 0;
  align-content: center;
}

.idle{
  background-color: var(--knime-silver-sand);
}
.error{
  background-color: red;
}
.running{
  background-color: yellow;
}
.success{
  background-color: green;
}


:slotted(button){
  margin: 5px;
  flex: 0 1 150px;
  justify-content: center;

}

</style>
