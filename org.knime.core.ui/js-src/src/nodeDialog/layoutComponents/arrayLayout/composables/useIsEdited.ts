import { type Ref, nextTick, onMounted, ref, watch } from "vue";

import inject from "../../../utils/inject";
import { ELEMENT_RESET_BUTTON_ID } from "../EditResetButton.vue";

const MILLISECONDS_UNTIL_LOADING = 200;

const isStringArray = (ids: (string | undefined)[]): ids is string[] =>
  ids.every((id) => typeof id === "string");
const hash = (ids: (string | undefined)[]) =>
  isStringArray(ids) ? ids.reduce((x, y) => x + y, "") : false;

export default (
  withEditAndReset: boolean,
  ids: Ref<(string | undefined)[]>,
) => {
  // mapping  element ids to whether the element is edited
  const isEdited = ref(new Map());
  const isEditedIsLoading = ref(false);
  const isTriggerActive = inject("isTriggerActive");
  const setIsEdited = () => {
    let resultAvailable = false;
    isTriggerActive({ id: ELEMENT_RESET_BUTTON_ID }).then((response) => {
      resultAvailable = true;
      if (response.state !== "SUCCESS") {
        return;
      }
      isEdited.value = response.result.reduce((acc, { indices, isActive }) => {
        acc.set(indices[0], isActive);
        return acc;
      }, new Map());
      isEditedIsLoading.value = false;
    });
    setTimeout(() => {
      if (!resultAvailable) {
        isEditedIsLoading.value = true;
      }
    }, MILLISECONDS_UNTIL_LOADING);
    isEditedIsLoading.value = false;
  };

  const hasIds = ref(false);

  onMounted(() => {
    setTimeout(() => {
      if (!hasIds.value) {
        isEditedIsLoading.value = true;
      }
    }, MILLISECONDS_UNTIL_LOADING);
  });

  watch(
    () => hash(ids.value),
    (idsAvailable) => {
      if (withEditAndReset && idsAvailable) {
        hasIds.value = true;
        const alreadyComputedForIds =
          ids.value.every((id) => isEdited.value.has(id)) &&
          isEdited.value.size === ids.value.length;
        if (!alreadyComputedForIds) {
          nextTick(setIsEdited);
        }
      }
    },
    { immediate: true },
  );
  return { isEdited, isEditedIsLoading };
};
