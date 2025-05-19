import { type InjectionKey, provide, inject as vueInject } from "vue";

import type { Provided } from "../../../types/provided";
import inject from "../../../utils/inject";
import { topLevelElementScope } from "../ArrayLayoutItemLabel.vue";

const determineElementScope = (currentScope: string, parentScope: string) => {
  if (!currentScope) {
    return currentScope;
  }
  if (currentScope === topLevelElementScope) {
    return parentScope;
  }
  return `${parentScope}/items${currentScope.replace(/^#/, "")}`;
};

export const addIndexToStateProviders = (
  indexId: string,
  index: number,
  arrayScope: string,
) => {
  const injectionKey = "addStateProviderListener";
  const addStateProviderListener = inject(injectionKey);
  const injectionKeyParentScope: InjectionKey<string> =
    Symbol("arrayParentScope");
  /**
   * For nested array layouts, the parent scope might differ from the current array layouts scope.
   */
  const parentScope = vueInject(injectionKeyParentScope) ?? arrayScope;

  const wrapperWithIndex: Provided[typeof injectionKey] = (
    {
      indexIds = [],
      indices = [],
      ...location
    }: {
      indexIds?: string[];
      indices?: number[];
      [key: string]: any;
    } & Parameters<Provided[typeof injectionKey]>[0],
    callback,
  ) =>
    addStateProviderListener(
      {
        ...location,
        scope: determineElementScope(location.scope, parentScope),
        indexIds: [indexId, ...indexIds],
        indices: [index, ...indices],
      },
      callback,
    );

  provide(injectionKeyParentScope, parentScope);
  provide(injectionKey, wrapperWithIndex);
};

export const addIndexToTriggers = (indexId: string) => {
  const injectionKeyTrigger = "trigger";
  const trigger = inject(injectionKeyTrigger);

  const wrapperWithIndexTrigger: Provided[typeof injectionKeyTrigger] = ({
    id,
    indexIds = [],
  }) =>
    trigger({
      id,
      indexIds: [indexId, ...indexIds],
    });

  provide(injectionKeyTrigger, wrapperWithIndexTrigger);

  const injectionKeyIsTriggerActive = "isTriggerActive";
  const isTriggerActive = inject(injectionKeyIsTriggerActive);

  const wrapperWithIndexTriggerIsActive: Provided[typeof injectionKeyIsTriggerActive] =
    ({ id, indexIds = [] }) =>
      isTriggerActive({
        id,
        indexIds: [indexId, ...indexIds],
      });

  provide(injectionKeyIsTriggerActive, wrapperWithIndexTriggerIsActive);
};
