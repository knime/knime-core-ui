import { type MaybeRef, type Ref, onMounted, unref, watch } from "vue";

import type { SettingComparator } from "@knime/ui-extension-service";

import inject from "../../utils/inject";
import useDirtySettings from "../nodeDialog/useDirtySettings";

import { JsonSettingsComparator } from "./JsonSettingsComparator";
import { injectIsChildOfAddedArrayLayoutElement } from "./useAddedArrayLayoutItem";

export const useDirtySetting = <ValueType>({
  dataPath,
  value,
  initialValue: customInitialValue,
  valueComparator: valueComparatorProp,
}: {
  dataPath: MaybeRef<string>;
  value: Ref<ValueType>;
  /**
   * If not provided, we try to get the initial value from getInitialValue(dataPath)
   */
  initialValue?: ValueType | undefined;
  valueComparator?: SettingComparator<ValueType | undefined>;
  configPaths?: string[];
}) => {
  const valueComparator =
    valueComparatorProp ??
    (new JsonSettingsComparator() as SettingComparator<ValueType | undefined>);
  const { constructSettingState, getSettingState } = useDirtySettings();

  const isInsideAnAddedArrayItem = injectIsChildOfAddedArrayLayoutElement();
  const updateData = inject("updateData");
  const getInitialValue = inject("getInitialValue");
  const constructNewSettingState = () => {
    const initialValue =
      typeof customInitialValue === "undefined"
        ? getInitialValue<ValueType>(unref(dataPath))
        : customInitialValue;
    const setValue = constructSettingState<ValueType | undefined>(
      unref(dataPath),
      {
        initialValue,
        valueComparator,
      },
    );
    setValue(value.value);
    if (isInsideAnAddedArrayItem.isAdded) {
      updateData(unref(dataPath));
    }
    return setValue;
  };

  const getExistingSettingStateAndSetCurrentValue = () => {
    const setValue = getSettingState<ValueType | undefined>(unref(dataPath));
    setValue?.(value.value);
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
