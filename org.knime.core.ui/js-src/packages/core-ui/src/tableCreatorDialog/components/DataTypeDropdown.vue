<script setup lang="ts">
import { computed } from "vue";

import { Dropdown } from "@knime/components";
import { KdsDataType } from "@knime/kds-components";

export type DataTypeChoice = {
  id: string;
  text: string;
  type: {
    id: string;
    text: string;
  };
};

export type DataType = {};

export type DataTypeDropdownProps = {
  possibleValues: DataTypeChoice[];
  id: string;
  ariaLabel: string;
};

const modelValue = defineModel<string>();

const props = defineProps<DataTypeDropdownProps>();

const possibleValues = computed(() => {
  if (props.possibleValues === null) {
    return [];
  }
  return props.possibleValues.map((value) => ({
    ...value,
    slotData: {
      text: value.text,
      ...(value.type && { typeId: value.type.id, typeText: value.type.text }),
    },
  }));
});
</script>

<template>
  <Dropdown
    v-bind="{ ariaLabel, id }"
    v-model="modelValue"
    :possible-values="possibleValues ?? []"
    compact
  >
    <template
      #option="{ slotData, selectedValue, isMissing, expanded } = {
        slotData: {},
      }"
    >
      <template v-if="expanded || selectedValue !== '' || isMissing">
        <div
          :class="[
            'data-type-entry',
            { missing: isMissing, 'with-type': isMissing || slotData.typeId },
          ]"
        >
          <template v-if="isMissing">
            <KdsDataType size="small" />
            <span>(MISSING) {{ selectedValue }}</span>
          </template>
          <template v-else>
            <template v-if="slotData.typeId">
              <KdsDataType
                :icon-name="slotData.typeId"
                :icon-title="slotData.typeText"
                size="small"
              />
            </template>
            <span>{{ slotData.text }}</span>
          </template>
        </div>
      </template>
    </template>
  </Dropdown>
</template>

<style lang="postcss" scoped>
.data-type-entry.with-type {
  display: flex;
  gap: var(--space-4);
  align-items: center;

  & > span {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
