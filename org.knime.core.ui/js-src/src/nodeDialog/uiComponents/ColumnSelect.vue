<script>
import DropdownInput from "./DropdownInput.vue";

export default {
  name: "ColumnSelect",
  components: {
    DropdownInput,
  },
  inheritAttrs: false,
  methods: {
    toValue(data) {
      return data.selected;
    },
    toData(options, value) {
      const compatibleTypes = options.find(
        (item) => item.id === value,
      )?.compatibleTypes;
      return { selected: value, compatibleTypes };
    },
  },
};
</script>

<template>
  <DropdownInput
    v-bind="$attrs"
    :options-generator="optionsGenerator"
    :control-data-to-dropdown-value="toValue"
    :dropdown-value-to-control-data="toData"
  />
</template>
