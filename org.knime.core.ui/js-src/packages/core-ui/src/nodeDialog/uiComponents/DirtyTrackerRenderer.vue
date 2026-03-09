<script setup lang="ts">
import { computed, watch } from "vue";

import {
  type UiSchemaWithProvidedOptions,
  type VueControlProps,
  useProvidedState,
} from "@knime/jsonforms";

import useDirtySettings, {
  getModelOrView,
} from "../composables/nodeDialog/useDirtySettings";

type DirtyTrackerOptions = {
  makeDirty?: boolean;
};

const props = defineProps<VueControlProps<undefined>>();

const uischema = computed(
  () =>
    props.control.uischema as UiSchemaWithProvidedOptions<DirtyTrackerOptions>,
);

const makeDirty = useProvidedState(uischema, "makeDirty", false);

const { registerNonControlState } = useDirtySettings();

const dirtyState = registerNonControlState(getModelOrView(props.control.path))({
  initialValue: false,
  valueComparator: {
    isModified: (currentState: boolean) => currentState,
    setSettings: () => {
      /**
       * Instead of updating the clean state, it remains false
       * and we instead change the current state.
       */
      makeDirty.value = false;
    },
  },
});

watch(makeDirty, dirtyState.setValue, { immediate: true });
</script>
