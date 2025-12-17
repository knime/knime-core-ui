import { type InjectionKey, provide, inject as vueInject } from "vue";

import type { Provided } from "../../../types/provided";
import inject from "../../../utils/inject";
import { topLevelElementScope } from "../ArrayLayoutItemLabel.vue";

const determineElementIdOrScope = (
  location: { id: string } | { scope: string },
  parentScope: string,
) => {
  if ("id" in location) {
    return { id: `${parentScope}/items${location.id.replace(/^#/, "")}` };
  }
  const currentScope = location.scope;
  if (!currentScope) {
    return { scope: currentScope };
  }
  if (currentScope === topLevelElementScope) {
    return { scope: parentScope };
  }
  return { scope: `${parentScope}/items${currentScope.replace(/^#/, "")}` };
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
      id?: string;
      scope?: string;
      [key: string]: any;
    } & Parameters<Provided[typeof injectionKey]>[0],
    callback,
  ) =>
    location.settingsId
      ? /**
         * Keep untouched in this case since the array index is part of the path of the dynamic settings.
         */
        addStateProviderListener({ indexIds, indices, ...location }, callback)
      : addStateProviderListener(
          {
            ...location,
            ...determineElementIdOrScope(location, parentScope),
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

  const wrapperWithIndexTrigger: Provided[typeof injectionKeyTrigger] = (
    triggerId: unknown,
  ) => {
    const {
      id,
      indexIds = [],
      settingsId,
    } = triggerId as {
      id: string;
      indexIds?: string[];
      settingsId?: string;
    };
    return settingsId
      ? trigger({ id, indexIds, settingsId })
      : trigger({
          id,
          indexIds: [indexId, ...indexIds],
        });
  };

  provide(injectionKeyTrigger, wrapperWithIndexTrigger);

  const injectionKeyIsTriggerActive = "isTriggerActive";
  const isTriggerActive = inject(injectionKeyIsTriggerActive);

  const wrapperWithIndexTriggerIsActive: Provided[typeof injectionKeyIsTriggerActive] =
    ({ id, indexIds = [], settingsId }) =>
      settingsId
        ? isTriggerActive({ id, indexIds, settingsId })
        : isTriggerActive({
            id,
            indexIds: [indexId, ...indexIds],
          });

  provide(injectionKeyIsTriggerActive, wrapperWithIndexTriggerIsActive);
};
