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

export const setUpApplyButton = (key?: string) => {
  const applyButton = ref(null);
  const applyText = ref("Apply");
  const applyButtonShown = ref(false);
  const applyDisabled = ref(false);
  const onClick = ref<undefined | (() => Promise<void>)>();
  const provided: ProvidedType = {
    element: applyButton,
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
