<script lang="ts">
import { type Ref, computed, defineComponent, nextTick, ref, watch } from "vue";
import { type JsonSchema, composePaths, toDataPath } from "@jsonforms/core";
import {
  DispatchRenderer,
  rendererProps,
  useJsonFormsArrayControl,
  useJsonFormsControl,
} from "@jsonforms/vue";

import { Button } from "@knime/components";
import PlusIcon from "@knime/styles/img/icons/plus.svg";

import { useDirtySetting } from "./../../composables/components/useDirtySetting";
import useProvidedState from "./../../composables/components/useProvidedState";
import {
  createNewId,
  deleteId,
  setIndex,
} from "./../../composables/nodeDialog/useArrayIds";
import { editResetButtonFormat } from "./../../renderers/editResetButtonRenderer";
import DialogComponentWrapper from "./../../uiComponents/DialogComponentWrapper.vue";
import inject from "./../../utils/inject";
import ArrayLayoutItem from "./ArrayLayoutItem.vue";
import ArrayLayoutItemControls from "./ArrayLayoutItemControls.vue";
import useIsEdited from "./composables/useIsEdited";

interface ArrayLayoutControl {
  data: {
    _id?: string;
  }[];
  path: string;
  uischema: {
    scope: string;
    options: {
      elementDefaultValueProvider?: string;
      withEditAndReset: boolean;
      showSortButtons: boolean;
      hasFixedSize: boolean;
      addButtonText?: string;
      elementCheckboxScope?: string;
      elementTitleProvider?: string;
      elementSubTitleProvider?: string;
      arrayElementTitle?: string;
      detail?: Record<string, JsonSchema>;
    };
  };
  schema: { properties: Record<string, JsonSchema> };
}

const ArrayLayout = defineComponent({
  name: "ArrayLayout",
  components: {
    DispatchRenderer,
    Button,
    PlusIcon,
    DialogComponentWrapper,
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
      control.value.uischema.options?.elementDefaultValueProvider,
      null,
    );
    useDirtySetting({
      dataPath: control.value.path,
      value: numElements,
      valueComparator: {
        setSettings: (length) => {
          cleanArrayLength.value = length!;
        },
        isModified: (length) => cleanArrayLength.value !== length,
      },
    });

    /**
     * We need to ids in the data for setting correct keys in the template and for handling updates correctly.
     */
    const dataWithId = computed(() =>
      control.value.data.map((item) =>
        item._id
          ? (item as { _id: string })
          : {
              ...item,
              _id: createNewId(),
            },
      ),
    );
    const idsRecord = inject("createArrayAtPath")(
      toDataPath(control.value.uischema.scope),
    );
    const ids = computed(() => dataWithId.value.map(({ _id }) => _id));
    const hash = (ids: string[]) => ids.reduce((x, y) => x + y, "");

    watch(
      () => ids.value,
      (newIds, oldIds) => {
        if (oldIds && hash(newIds) === hash(oldIds)) {
          return;
        }
        newIds.forEach((id, index) => setIndex(id, index));
        oldIds
          ?.filter((id) => !newIds.includes(id))
          .forEach((id) => deleteId(id));
      },
      { immediate: true },
    );

    const { isEdited, isEditedIsLoading } = useIsEdited(
      control.value.uischema.options.withEditAndReset,
      ids,
    );

    watch(
      () => hash(ids.value),
      () => handleChange(control.value.path, dataWithId.value),
      { immediate: true },
    );

    const {
      addItem,
      moveDown,
      moveUp,
      removeItems,
      control: arrayControl,
    } = useJsonFormsArrayControl(props as any);

    const updateData = inject("updateData");
    const andUpdate =
      <T extends (...params: any[]) => () => void>(fn: T) =>
      async (...params: Parameters<T>) => {
        fn(...params)();
        await nextTick();
        updateData(control.value.path);
      };

    return {
      addItem: andUpdate(addItem),
      moveDown: andUpdate(moveDown!),
      moveUp: andUpdate(moveUp!),
      removeItems: andUpdate(removeItems!),
      control: arrayControl as unknown as Ref<ArrayLayoutControl>,
      numElements,
      cleanArrayLength,
      signedData: dataWithId,
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
      const elementTitleProvider =
        this.control.uischema.options?.elementTitleProvider;
      if (elementTitleProvider) {
        return {
          type: "provided",
          provider: elementTitleProvider,
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
    subTitleProvider() {
      return this.control.uischema.options?.elementSubTitleProvider;
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
      this.addItem(
        this.control.path,
        this.createDefaultValue(
          this.control.schema as { properties: Record<string, JsonSchema> },
        ),
      );
    },
    moveItemUp(index: number) {
      this.moveUp(this.control.path, index);
    },
    moveItemDown(index: number) {
      this.moveDown(this.control.path, index);
    },
    deleteItem(index: number) {
      this.removeItems(composePaths(this.control.path, ""), [index]);
    },
  },
});
export default ArrayLayout;
</script>

<template>
  <DialogComponentWrapper :control="control">
    <div class="array">
      <div
        v-for="(obj, objIndex) in signedData"
        :key="`${control.path}-${obj._id}`"
        :class="['item', { card: useCardLayout }]"
      >
        <ArrayLayoutItem
          :id="obj._id"
          :ids-record="idsRecord"
          :elements="elements"
          :array-element-title="arrayElementTitle"
          :sub-title-provider="subTitleProvider"
          :path="control.path"
          :index="objIndex"
          :has-been-added="objIndex === elementCountBeforeAddingOne"
          :element-checkbox-scope="elementCheckboxScope"
        >
          <template #renderer="{ element, path }">
            <DispatchRenderer
              v-bind="control"
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
  </DialogComponentWrapper>
</template>

<style lang="postcss" scoped>
.array {
  display: flex;
  flex-direction: column;
  gap: var(--space-16);
  margin-bottom: 10px;

  & .item {
    display: flex;
    flex-direction: column;

    &.card {
      padding: 5px 10px 10px;
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
