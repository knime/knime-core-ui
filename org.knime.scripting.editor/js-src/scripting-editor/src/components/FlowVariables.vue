<script lang="ts">
import { defineComponent, type PropType } from 'vue';
import type { FlowVariable } from '../utils/scripting-service';

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
