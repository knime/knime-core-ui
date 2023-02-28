<script lang="ts">
import { defineComponent, type PropType } from 'vue';
import type { FlowVariableInput } from '../utils/scripting-service';

export default defineComponent({
    name: 'FlowVariables',
    components: {},
    props: {
        flowVariables: {
            type: Array as PropType<FlowVariableInput[]>,
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
