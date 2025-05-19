<script lang="ts">
import { type Ref, computed, defineComponent, ref, watch } from "vue";
import { type JsonSchema, composePaths, toDataPath } from "@jsonforms/core";
import {
  DispatchRenderer,
  rendererProps,
  useJsonFormsArrayControl,
  useJsonFormsControl,
} from "@jsonforms/vue";

import { Button } from "@knime/components";
import { useProvidedState } from "@knime/jsonforms";
import PlusIcon from "@knime/styles/img/icons/plus.svg";

import { useDirtySetting } from "../../composables/components/useDirtySetting";
import {
  createNewId,
  deleteId,
  getIndex,
  setIndex,
} from "../../composables/nodeDialog/useArrayIds";
import { editResetButtonFormat } from "../../renderers/editResetButtonRenderer";
import inject from "../../utils/inject";

import ArrayLayoutItem from "./ArrayLayoutItem.vue";
import ArrayLayoutItemControls from "./ArrayLayoutItemControls.vue";
import useIsEdited from "./composables/useIsEdited";

export interface ArrayLayoutControl {
  data: {
    _id?: string;
  }[];
  path: string;
  uischema: {
    type: "Control";
    scope: string;
    options: {
      elementDefaultValueProvider?: string;
      withEditAndReset: boolean;
      showSortButtons: boolean;
      hasFixedSize: boolean;
      addButtonText?: string;
      elementCheckboxScope?: string;
      arrayElementTitle?: string;
      detail?: Record<string, JsonSchema>;
      /**
       * Never actually present here but included in this type for provided options.
       */
      elementSubTitle?: string;
      elementDefaultValue?: unknown;
    };
    providedOptions?: (
      | "arrayElementTitle"
      | "elementSubTitle"
      | "elementDefaultValue"
    )[];
  };
  visible: boolean;
  schema: { properties: Record<string, JsonSchema> };
}

const ArrayLayout = defineComponent({
  name: "ArrayLayout",
  components: {
    DispatchRenderer,
    Button,
    PlusIcon,
    ArrayLayoutItemControls,
    ArrayLayoutItem,
  },
  props: {
    ...rendererProps(),
  },
  setup(props) {
    const { handleChange, control } = useJsonFormsControl(
      props as any,
    ) as unknown as {
      handleChange: any;
      control: Ref<ArrayLayoutControl>;
    };
    const numElements = computed(() => control.value.data?.length ?? 0);
    const cleanArrayLength = ref(numElements.value);
    const providedElementDefaultValue = useProvidedState(
      computed(() => control.value.uischema),
      "elementDefaultValue",
      null,
    );
    useDirtySetting({
      dataPath: computed(() => control.value.path),
      value: numElements,
      valueComparator: {
        setSettings: (length) => {
          cleanArrayLength.value = length!;
        },
        isModified: (length) => cleanArrayLength.value !== length,
      },
    });

    const idsRecord = inject("createArrayAtPath")(
      toDataPath(control.value.uischema.scope),
    );
    const getExistingId = (index: number) =>
      Object.keys(idsRecord).find((id) => getIndex(id) === index);
    const getOrCreateId = (index: number) =>
      getExistingId(index) ?? createNewId();

    watch(
      () => control.value.data,
      (data) => {
        data.forEach((item, index) => {
          if (!item._id) {
            handleChange(
              composePaths(composePaths(control.value.path, `${index}`), "_id"),
              getOrCreateId(index),
            );
          }
        });
      },
      { immediate: true },
    );
    const ids = computed(() => control.value.data.map(({ _id }) => _id));
    const hash = (ids: (string | undefined)[]) =>
      ids.reduce((x, y) => (x ?? "<noId>") + (y ?? "<noId>"), "");

    watch(
      () => ids.value,
      (newIds, oldIds) => {
        if (oldIds && hash(newIds) === hash(oldIds)) {
          return;
        }
        newIds.forEach((id, index) => id && setIndex(id, index));
        oldIds
          ?.filter((id) => id && !newIds.includes(id))
          .forEach((id) => deleteId(id!));
      },
      { immediate: true },
    );

    const { isEdited, isEditedIsLoading } = useIsEdited(
      control.value.uischema.options.withEditAndReset,
      ids,
    );

    const {
      addItem,
      moveDown,
      moveUp,
      removeItems,
      control: arrayControl,
    } = useJsonFormsArrayControl(props as any);

    return {
      addItem,
      moveDown,
      moveUp,
      removeItems,
      control: arrayControl as unknown as Ref<ArrayLayoutControl>,
      numElements,
      cleanArrayLength,
      idsRecord,
      providedElementDefaultValue,
      isEdited,
      isEditedIsLoading,
    };
  },
  data() {
    return {
      arrayElementTitleKey: "arrayElementTitle" as const,
      editResetButtonFormat,
      elementCountBeforeAddingOne: -1,
      clearStateMethods: new Map<string, () => void>(),
    };
  },
  computed: {
    showSortControls() {
      return this.control.uischema.options.showSortButtons;
    },
    showEditAndResetControls() {
      return this.control.uischema.options.withEditAndReset;
    },
    elementCheckboxScope() {
      return this.control.uischema.options.elementCheckboxScope;
    },
    showAddAndDeleteButtons() {
      return !this.control.uischema.options.hasFixedSize;
    },
    elements() {
      if (this.control.uischema.options.detail) {
        return Object.entries(this.control.uischema.options.detail);
      }
      return [];
    },
    arrayElementTitle() {
      if (
        this.control.uischema.providedOptions?.includes(
          this.arrayElementTitleKey,
        )
      ) {
        return {
          type: "provided",
        };
      }
      const elementTitle =
        this.control.uischema.options?.[this.arrayElementTitleKey];
      if (elementTitle) {
        return {
          type: "enumerated",
          title: elementTitle,
        };
      }
      return false;
    },
    useCardLayout() {
      return this.arrayElementTitle !== false;
    },
  },
  methods: {
    createDefaultValue(schema: { properties: Record<string, JsonSchema> }) {
      if (this.providedElementDefaultValue !== null) {
        return this.providedElementDefaultValue;
      }
      const defaultObject: Record<string, any> = {};
      Object.keys(schema.properties).forEach((ele) => {
        defaultObject[ele] = schema.properties[ele].default;
      });
      return defaultObject;
    },
    addDefaultItem() {
      this.elementCountBeforeAddingOne = this.numElements;
      this.addItem(this.control.path, {
        ...this.createDefaultValue(
          this.control.schema as { properties: Record<string, JsonSchema> },
        ),
        _id: createNewId(),
      })();
    },
    moveItemUp(index: number) {
      this.moveUp?.(this.control.path, index)();
    },
    moveItemDown(index: number) {
      this.moveDown?.(this.control.path, index)();
    },
    clearElement(id: string | undefined) {
      if (id) {
        this.clearStateMethods.get(id)?.();
        this.clearStateMethods.delete(id);
      }
    },
    registerElement(id: string | undefined) {
      return (instance: null | { clearState: () => void }) => {
        if (id) {
          this.clearStateMethods.set(id, () => instance?.clearState());
        }
      };
    },
    deleteItem(index: number) {
      const id = this.control.data[index]._id;
      this.clearElement(id);
      this.removeItems?.(composePaths(this.control.path, ""), [index])();
    },
  },
});
export default ArrayLayout;
</script>

<template>
  <div v-if="control.visible" :class="['array', { cards: useCardLayout }]">
    <div
      v-for="(obj, objIndex) in control.data"
      :key="`${control.path}-${obj._id}`"
      :class="['item', { card: useCardLayout }]"
    >
      <ArrayLayoutItem
        :id="obj._id"
        :ref="registerElement(obj._id)"
        :ids-record="idsRecord"
        :array-ui-schema="control.uischema"
        :elements="elements"
        :array-element-title="arrayElementTitle"
        :path="control.path"
        :index="objIndex"
        :has-been-added="objIndex === elementCountBeforeAddingOne"
        :element-checkbox-scope="elementCheckboxScope"
      >
        <template #renderer="{ element, path }">
          <DispatchRenderer
            :schema="control.schema"
            :uischema="element"
            :path="path"
          />
        </template>
        <template #controls>
          <ArrayLayoutItemControls
            :is-first="objIndex === 0"
            :is-last="objIndex === control.data.length - 1"
            :show-sort-controls="showSortControls"
            :show-delete-button="showAddAndDeleteButtons"
            @move-up="moveItemUp(objIndex)"
            @move-down="moveItemDown(objIndex)"
            @delete="deleteItem(objIndex)"
          >
            <template #before>
              <DispatchRenderer
                v-if="showEditAndResetControls"
                v-bind="control"
                :schema="{
                  type: 'object',
                  properties: {
                    _edit: {
                      type: 'boolean',
                    },
                  },
                }"
                :uischema="{
                  scope: '#/properties/_edit',
                  options: {
                    format: editResetButtonFormat,
                  },
                }"
                :path="`${control.path}.${objIndex}`"
                :initial-is-edited="isEdited.get(obj._id) ? '' : undefined"
                :is-loading="isEditedIsLoading ? '' : undefined"
              />
            </template>
          </ArrayLayoutItemControls>
        </template>
      </ArrayLayoutItem>
    </div>
    <Button
      v-if="showAddAndDeleteButtons"
      class="add-item-button"
      with-border
      compact
      @click="addDefaultItem"
    >
      <PlusIcon />
      {{ control.uischema.options.addButtonText || "New" }}
    </Button>
  </div>
</template>

<style lang="postcss" scoped>
.array {
  display: flex;
  flex-direction: column;
  gap: var(--error-message-min-reserved-space);
  margin-bottom: 10px;

  &.cards {
    gap: var(--space-12);
  }

  & .item {
    display: flex;
    flex-direction: column;

    &.card {
      padding: 4px 8px var(--error-message-min-reserved-space);
      background-color: white;
      box-shadow: 0 1px 4px 0 var(--knime-gray-dark-semi);
    }
  }

  & .add-item-button {
    width: fit-content;

    &:not(:first-child) {
      margin-top: 10px;
    }
  }
}
</style>
