import { type MaybeRef, type Ref, onMounted, unref, watch } from "vue";

import type { SettingComparator } from "@knime/ui-extension-service";

import inject from "../../utils/inject";
import useDirtySettings from "../nodeDialog/useDirtySettings";

import { JsonSettingsComparator } from "./JsonSettingsComparator";
import { injectIsChildOfAddedArrayLayoutElement } from "./useAddedArrayLayoutItem";

export const useDirtySetting = <ValueType>({
  dataPath,
  value,
  valueComparator: valueComparatorProp,
}: {
  dataPath: MaybeRef<string>;
  value: Ref<ValueType>;
  valueComparator?: SettingComparator<ValueType | undefined>;
  configPaths?: string[];
}) => {
  const valueComparator =
    valueComparatorProp ??
    (new JsonSettingsComparator() as SettingComparator<ValueType | undefined>);
  const { constructSettingState, getSettingState } = useDirtySettings();

  const isInsideAnAddedArrayItem = injectIsChildOfAddedArrayLayoutElement();
  const updateData = inject("updateData");
  const initialValue = value.value;
  const constructNewSettingState = () => {
    const setValue = constructSettingState<ValueType | undefined>(
      unref(dataPath),
      {
        initialValue: isInsideAnAddedArrayItem.isAdded
          ? // eslint-disable-next-line no-undefined
            undefined
          : initialValue,
        valueComparator,
      },
    );
    if (isInsideAnAddedArrayItem.isAdded) {
      setValue(initialValue);
      updateData(unref(dataPath));
    }
    return setValue;
  };

  const getExistingSettingStateAndSetCurrentValue = () => {
    const setValue = getSettingState<ValueType | undefined>(unref(dataPath));
    setValue?.(initialValue);
    if (isInsideAnAddedArrayItem.isAdded) {
      updateData(unref(dataPath));
    }
    return setValue;
  };

  const getOrConstructSettingState = () =>
    getExistingSettingStateAndSetCurrentValue() ?? constructNewSettingState();

  let setValue: null | ((value: ValueType | undefined) => void) = null;

  onMounted(() => {
    setValue = getOrConstructSettingState();
  });

  watch(
    () => value.value,
    (newValue) => setValue?.(newValue),
  );
  if (isInsideAnAddedArrayItem.isAdded) {
    isInsideAnAddedArrayItem.registerDirtyStateReset(() => {
      // eslint-disable-next-line no-undefined
      setValue?.(undefined);
    });
  }
};
