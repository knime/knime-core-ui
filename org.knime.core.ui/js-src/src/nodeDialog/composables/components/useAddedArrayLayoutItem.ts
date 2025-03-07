import { type InjectionKey, inject, provide } from "vue";

/**
 * Exported for testing only
 */
export const injectionKey: InjectionKey<
  | {
      isAdded: true;
      registerDirtyStateReset: (reset: () => void) => void;
    }
  | { isAdded: false }
> = Symbol("isChildOfAddedArrayLayoutElement");

/**
 * The initialization of settings states depends on the initial values.
 * With the here provided boolean, it becomes determinable whether the
 * initial value is given by the child controls data or should be undefined instead.
 */

export const provideForAddedArrayLayoutElements = () => {
  const elementDirtyStateResetters = new Set<() => void>();

  provide(injectionKey, {
    isAdded: true,
    registerDirtyStateReset: (reset) => {
      elementDirtyStateResetters.add(reset);
    },
  });

  return {
    resetElementDirtyState: () =>
      elementDirtyStateResetters.forEach((reset) => reset()),
  };
};

export const injectIsChildOfAddedArrayLayoutElement = () =>
  inject(injectionKey, {
    isAdded: false,
  });
