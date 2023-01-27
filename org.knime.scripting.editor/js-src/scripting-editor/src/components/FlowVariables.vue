<script lang="ts">
import { defineComponent, type PropType } from 'vue';

export type FlowVariable = {
    name: string;
    value: string;
}

export default defineComponent({
    name: 'FlowVariables',
    components: {},
    props: {
        flowVariables: {
            type: Array as PropType<FlowVariable[]>,
            default() {
                return [];
            }
        }
    }
});

</script>

<template>
  <td>
    <tr
      v-for="variable in flowVariables"
      :key="variable.name"
    >
      {{ variable.name }} : <p>{{ variable.value }}</p>
    </tr>
  </td>
</template>
