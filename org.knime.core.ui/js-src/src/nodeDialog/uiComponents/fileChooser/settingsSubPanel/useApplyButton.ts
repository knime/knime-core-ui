import { type InjectionKey, type Ref, inject, provide, ref } from "vue";

type ProvidedType = {
  element: Ref<null | HTMLElement>;
  disabled: Ref<boolean>;
  text: Ref<string>;
  shown: Ref<boolean>;
  /**
   * Triggered whenever the apply button is pressed.
   */
  onApply: Ref<undefined | (() => Promise<void>)>;
};

type InitialValue = {
  text: string;
  disabled?: true;
  shown?: false;
};

const otherKeys: {
  [key: string]: InjectionKey<ProvidedType>;
} = {};

/**
 * Exported for tests only.
 */
export const createOrGetInjectionKey = (
  key: string,
): InjectionKey<ProvidedType> => {
  if (!(key in otherKeys)) {
    const newKey = Symbol(key);
    otherKeys[key] = newKey;
  }

  return otherKeys[key];
};

// exported for tests only
export const applyButtonInjectionKey = createOrGetInjectionKey(
  "applyButtonInjectionKey",
);

export const setUpApplyButton = ({
  key,
  initialValue,
  element,
}: {
  key?: string;
  element: Ref<HTMLElement | null>;
  initialValue: InitialValue;
}) => {
  const applyText = ref(initialValue.text);
  const applyButtonShown = ref(initialValue.shown ?? true);
  const applyDisabled = ref(initialValue.disabled ?? false);
  const onClick = ref<undefined | (() => Promise<void>)>();
  const provided: ProvidedType = {
    element,
    disabled: applyDisabled,
    shown: applyButtonShown,
    text: applyText,
    onApply: onClick,
  };

  provide(
    key ? createOrGetInjectionKey(key) : applyButtonInjectionKey,
    provided,
  );
  return provided;
};

export const useApplyButton = (key?: string) =>
  inject(key ? createOrGetInjectionKey(key) : applyButtonInjectionKey)!;
