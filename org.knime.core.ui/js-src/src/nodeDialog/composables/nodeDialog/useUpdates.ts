/* eslint-disable max-lines */
import { nextTick } from "vue";
import { composePaths } from "@jsonforms/core";
import { get } from "lodash-es";

import { type AlertParams } from "@knime/ui-extension-service";

import type { Result } from "../../api/types/Result";
import {
  type IndexIdsValuePairs,
  type Pairs,
  type Trigger,
  type Update,
  type UpdateResult,
  isLocationBased,
  isValueTrigger,
  isValueUpdateResult,
} from "../../types/Update";

import { type ArrayRecord, getIndex } from "./useArrayIds";
import type { StateProviderLocation } from "./useStateProviders";
import type {
  IndexedIsActive,
  IsActiveCallback,
  TriggerCallback,
} from "./useTriggers";
import { combineScopeWithIndices } from "./utils/dataPaths";
import { getDependencyValues } from "./utils/dependencyExtraction";
import {
  isIndexIdsAndValuePairs,
  toIndicesValuePairs,
} from "./utils/updateResults";

const resolveToIndices = (ids: string[] | undefined) =>
  (ids ?? []).map((id) => getIndex(id));

export type DialogSettings = { view?: any; model?: any };

export type SettingsIdContext = {
  settingsId: string;
  currentParentPathSegments: string[];
};

const settingsIdToRegisteredWatchersRemoval = new Map<string, (() => void)[]>();

const addRegisteredWatcher = (
  settingsId: string,
  unregisterCallback: () => void,
): void => {
  if (!settingsIdToRegisteredWatchersRemoval.has(settingsId)) {
    settingsIdToRegisteredWatchersRemoval.set(settingsId, []);
  }
  settingsIdToRegisteredWatchersRemoval
    .get(settingsId)!
    .push(unregisterCallback);
};

export const clearRegisteredWatchersForSettingsId = (
  settingsId: string,
): void => {
  const removals = settingsIdToRegisteredWatchersRemoval.get(settingsId);
  if (removals) {
    for (const remove of removals) {
      remove();
    }
    settingsIdToRegisteredWatchersRemoval.delete(settingsId);
  }
};

export type SettingsUpdate2 = (params: {
  method: "settings.update2";
  options: [
    /**
     * Reserved for future use (currently null)
     */
    null,
    /**
     * The trigger that caused the update
     */
    Trigger,
    /**
     * The current dependencies extracted from the settings
     */
    Record<string, Pairs>,
  ];
}) => Promise<Result<UpdateResult[]>>;

export type SettingsUpdate2WithSettingsId = (params: {
  method: "settings.update2WithSettingsId";
  options: [
    /**
     * The settings ID for scoped updates
     */
    string,
    /**
     * Reserved for future use (currently null)
     */
    null,
    /**
     * The trigger that caused the update
     */
    Trigger,
    /**
     * The current dependencies extracted from the settings
     */
    Record<string, Pairs>,
  ];
}) => Promise<Result<UpdateResult[]>>;

const indicesAreDefined = (indices: (number | null)[]): indices is number[] =>
  !indices.includes(null);

const getToBeAdjustedSegments = (
  scope: string,
  /**
   * Indices starting from the root applied to all targets
   */
  indices: number[] | undefined,
  /**
   * Mapping further indices to the respective values
   */
  values: [number[], unknown][],
  newSettings: DialogSettings,
  currentParentPathSegments: string[],
) => {
  const pathSegments = combineScopeWithIndices(scope, indices ?? []);
  if (currentParentPathSegments.length > 0) {
    pathSegments[0] = [...currentParentPathSegments, pathSegments[0]].join(".");
  }

  type SettingsWithPath = {
    settings: object;
    /**
     * @param subPath a sub path within the settings object
     * @returns the total absolute path
     */
    path: string;
    /**
     * The values that are to be adjusted at the remaining paths
     * once the algorithm reaches the last path segment, this is a one-element array with empty numbers
     */
    values: [number[], unknown][];
  };

  const toBeAdjustedByLastPathSegment = pathSegments
    .slice(0, pathSegments.length - 1)
    .reduce(
      (arrayOfSettings, dataPath) =>
        arrayOfSettings.flatMap(({ settings, path, values }) => {
          // accessing the settings at the current non-leaf dataPath, i.e. this has to be an array
          const arraySettings = get(settings, dataPath) as object[];
          const firstKey = values[0][0];
          if (!firstKey) {
            throw Error("Should not happen");
          }
          // no further specific indices are provided, so we have to adjust all indices
          if (firstKey.length === 0) {
            return arraySettings.map(
              (subSettings, index): SettingsWithPath => ({
                settings: subSettings,
                path: composePaths(composePaths(path, dataPath), `${index}`),
                values,
              }),
            );
          } else {
            // group by first index of keys
            const groupedValues = values.reduce((grouped, [key, value]) => {
              const firstIndex = key[0];
              if (!grouped.has(firstIndex)) {
                grouped.set(firstIndex, []);
              }
              grouped.get(firstIndex)!.push([key.slice(1), value]);
              return grouped;
            }, new Map<number, [number[], unknown][]>());
            return [...groupedValues.entries()].map(([index, valueMap]) => {
              const subSettings = arraySettings[index];
              return {
                settings: subSettings,
                path: composePaths(composePaths(path, dataPath), `${index}`),
                values: valueMap,
              };
            });
          }
        }),
      [{ settings: newSettings, path: "", values }] as SettingsWithPath[],
    );
  return {
    toBeAdjustedByLastPathSegment,
    lastPathSegment: pathSegments[pathSegments.length - 1],
  };
};

export default ({
  callStateProviderListener,
  callStateProviderListenerByIndices,
  registerWatcher,
  registerTrigger,
  updateData,
  sendAlert,
  pathIsControlledByFlowVariable,
  setValueAtPath,
  globalArrayIdsRecord,
  callDataService,
}: {
  callStateProviderListener: (
    location: { indexIds?: string[] } & StateProviderLocation,
    value: unknown,
  ) => void;
  callStateProviderListenerByIndices: (
    location: { indices: number[] } & StateProviderLocation,
    value: unknown,
  ) => void;
  registerWatcher: (params: {
    dependencies: string[];
    transformSettings: TriggerCallback;
  }) => () => void;
  /**
   * Used to trigger a new update depending on value update results (i.e. transitive updates)
   */
  updateData: (paths: string[]) => void;
  registerTrigger: (
    id: string,
    isActive: IsActiveCallback,
    callback: TriggerCallback,
    settingsId?: SettingsIdContext,
  ) => void;
  sendAlert: (params: AlertParams) => void;
  pathIsControlledByFlowVariable: (path: string) => boolean;
  setValueAtPath: (path: string, value: unknown) => void;
  globalArrayIdsRecord: ArrayRecord;
  callDataService: SettingsUpdate2 & SettingsUpdate2WithSettingsId;
}) => {
  const resolveUpdateResult =
    (
      updateResult: UpdateResult,
      onValueUpdate: (path: string) => void,
      indexIds: string[],
      settingsIdContext?: SettingsIdContext,
    ) =>
    (newSettings: DialogSettings) => {
      const indices = resolveToIndices(indexIds);

      if (!indicesAreDefined(indices)) {
        return;
      }
      if (isValueUpdateResult(updateResult)) {
        const { scope, values } = updateResult;
        const indicesValuePairs = isIndexIdsAndValuePairs(values)
          ? toIndicesValuePairs(values, getIndex)
          : values;
        const { toBeAdjustedByLastPathSegment, lastPathSegment } =
          getToBeAdjustedSegments(
            scope,
            indices,
            indicesValuePairs.map(({ indices, value }) => [indices, value]),
            newSettings,
            settingsIdContext
              ? settingsIdContext.currentParentPathSegments
              : [],
          );
        toBeAdjustedByLastPathSegment.forEach(
          ({ path, values: [[, value]] }) => {
            const fullToBeUpdatedPath = composePaths(path, lastPathSegment);
            if (!pathIsControlledByFlowVariable(fullToBeUpdatedPath)) {
              setValueAtPath(fullToBeUpdatedPath, value);
              onValueUpdate(fullToBeUpdatedPath);
            }
          },
        );
      } else {
        const toLocation = <T>(indexLocation: T): T & StateProviderLocation => {
          if (isLocationBased(updateResult)) {
            return {
              ...indexLocation,
              scope: updateResult.scope,
              providedOptionName: updateResult.providedOptionName,
              settingsId: settingsIdContext,
            };
          }
          return {
            ...indexLocation,
            id: updateResult.id,
            providedOptionName: updateResult.providedOptionName,
            settingsId: settingsIdContext,
          };
        };
        const { values } = updateResult;
        if (isIndexIdsAndValuePairs(values)) {
          values.forEach(({ indices: valueIndexIds, value }) =>
            callStateProviderListener(
              toLocation({ indexIds: [...indexIds, ...valueIndexIds] }),
              value,
            ),
          );
        } else {
          values.forEach(({ indices: valueIndices, value }) =>
            callStateProviderListenerByIndices(
              toLocation({ indices: [...indices, ...valueIndices] }),
              value,
            ),
          );
        }
      }
    };

  const resolveUpdateResults = (
    initialUpdates: UpdateResult[],
    currentSettings: DialogSettings,
    indexIds: string[] = [],
    settingsIdContext?: SettingsIdContext,
  ) => {
    const updatedPaths: string[] = [];
    initialUpdates
      .map((updateResult) =>
        resolveUpdateResult(
          updateResult,
          (path) => updatedPaths.push(path),
          indexIds,
          settingsIdContext,
        ),
      )
      .forEach((transform) => {
        transform(currentSettings);
      });
    // we have to wait one tick to ensure that array element ids are set correctly
    nextTick(() => updateData(updatedPaths));
  };

  const toScope = (scope: string, settingsIdContext?: SettingsIdContext) => {
    if (settingsIdContext) {
      return scope.replace(
        /^#/,
        `#/properties/${settingsIdContext.currentParentPathSegments.join(
          "/properties/",
        )}`,
      );
    }
    return scope;
  };

  const setValueTrigger = (
    scope: string,
    callback: TriggerCallback,
    settingsIdContext?: SettingsIdContext,
  ) => {
    const registeredWatcherRemoval = registerWatcher({
      dependencies: [toScope(scope, settingsIdContext)],
      transformSettings: callback,
    });
    if (settingsIdContext) {
      addRegisteredWatcher(
        settingsIdContext.settingsId,
        registeredWatcherRemoval,
      );
    }
  };

  const setTrigger = (
    trigger: Trigger,
    isActive: IsActiveCallback,
    triggerCallback: TriggerCallback,
    triggerInitially?: boolean,
    settingsIdContext?: SettingsIdContext,
  ): null | ReturnType<TriggerCallback> => {
    if (isValueTrigger(trigger)) {
      setValueTrigger(trigger.scope, triggerCallback, settingsIdContext);
      return null;
    }
    if (triggerInitially) {
      return triggerCallback([]);
    }
    registerTrigger(trigger.id, isActive, triggerCallback, settingsIdContext);
    return null;
  };

  const extractCurrentDependencies = (
    dependencies: string[],
    newSettings: object,
    indices: number[],
  ) =>
    Object.fromEntries<Pairs>(
      dependencies.map((scope) => [
        scope,
        getDependencyValues(newSettings, scope, indices, globalArrayIdsRecord),
      ]),
    );

  const callDataServiceUpdate2 = ({
    trigger,
    currentDependencies,
  }: {
    trigger: Trigger;
    currentDependencies: Record<string, Pairs>;
  }): Promise<Result<UpdateResult[]>> =>
    callDataService({
      method: "settings.update2",
      options: [null, trigger, currentDependencies],
    });

  const callDataServiceUpdate2WithSettingsId = ({
    trigger,
    currentDependencies,
    settingsId,
  }: {
    trigger: Trigger;
    currentDependencies: Record<string, Pairs>;
    settingsId: string;
  }): Promise<Result<UpdateResult[]>> =>
    callDataService({
      method: "settings.update2WithSettingsId",
      options: [settingsId, null, trigger, currentDependencies],
    });

  const sendAlerts = (messages: string[] | undefined) => {
    messages?.forEach((message) =>
      sendAlert({
        message,
        type: "error",
      }),
    );
  };

  const getUpdateResults =
    (
      { dependencies, trigger }: Update,
      settingsIdContext?: SettingsIdContext,
    ) =>
    (indexIds: string[]) =>
    (dependencySettings: DialogSettings): Promise<Result<UpdateResult[]>> => {
      const indicesBeforeUpdate = resolveToIndices(indexIds);
      if (!indicesAreDefined(indicesBeforeUpdate)) {
        throw Error("Trigger called with wrong ids: No indices found.");
      }
      if (settingsIdContext) {
        const parentDependencySettings = get(
          dependencySettings,
          settingsIdContext.currentParentPathSegments.join("."),
        );
        const currentDependencies = extractCurrentDependencies(
          dependencies,
          parentDependencySettings,
          indicesBeforeUpdate,
        );
        return callDataServiceUpdate2WithSettingsId({
          trigger,
          currentDependencies,
          settingsId: settingsIdContext.settingsId,
        });
      } else {
        const currentDependencies = extractCurrentDependencies(
          dependencies,
          dependencySettings,
          indicesBeforeUpdate,
        );
        return callDataServiceUpdate2({
          trigger,
          currentDependencies,
        });
      }
    };

  const getOrCreateIndicesEntry = (
    indexIds: string[],
    acc: IndexedIsActive[],
  ) => {
    let existing = acc.find(
      ({ indices }) => JSON.stringify(indices) === JSON.stringify(indexIds),
    );
    if (!existing) {
      existing = { indices: indexIds, isActive: false };
      acc.push(existing);
    }
    return existing;
  };

  const isUpdateAtIndicesNecessary = ({
    scope,
    currentIndices,
    indexIds,
    value,
    settings,
    settingsIdContext,
  }: {
    scope: string;
    currentIndices: number[];
    indexIds: string[];
    value: unknown;
    settings: object;
    settingsIdContext?: SettingsIdContext;
  }) => {
    const indices = indexIds.map(getIndex);
    if (!indicesAreDefined(indices)) {
      return false;
    }
    const { toBeAdjustedByLastPathSegment, lastPathSegment } =
      getToBeAdjustedSegments(
        scope,
        currentIndices,
        [[indices, value]],
        settings,
        settingsIdContext ? settingsIdContext.currentParentPathSegments : [],
      );
    return Boolean(
      toBeAdjustedByLastPathSegment.find(({ settings: s, values: [[, v]] }) => {
        return get(s, lastPathSegment) !== v;
      }),
    );
  };

  const isUpdateNecessary = ({
    updateResult,
    settings,
    indexIds,
  }: {
    updateResult: UpdateResult[];
    settings: DialogSettings;
    indexIds: string[];
  }) =>
    updateResult.reduce((acc: IndexedIsActive[], updateResult) => {
      const currentIndices = resolveToIndices(indexIds);
      if (!indicesAreDefined(currentIndices)) {
        return acc;
      }
      (updateResult.values as IndexIdsValuePairs).forEach(
        ({ indices: indexIds, value }) => {
          const existing = getOrCreateIndicesEntry(indexIds, acc);
          if (isValueUpdateResult(updateResult)) {
            if (
              isUpdateAtIndicesNecessary({
                scope: updateResult.scope,
                currentIndices,
                indexIds,
                value,
                settings,
              })
            ) {
              existing.isActive = true;
            }
          } else {
            // for simplicity, if an ui state is provided, the update is seen as necessary
            existing.isActive = true;
          }
        },
      );
      return acc;
    }, [] satisfies IndexedIsActive[]);

  const getIsUpdateNecessary =
    (
      { dependencies, trigger }: Update,
      settingsIdContext?: SettingsIdContext,
    ) =>
    (indexIds: string[]) =>
    async (
      dependencySettings: DialogSettings,
    ): Promise<Result<IndexedIsActive[]>> => {
      const response = await getUpdateResults(
        { dependencies, trigger },
        settingsIdContext,
      )(indexIds)(dependencySettings);
      if (response.state === "SUCCESS") {
        const isNecessary = isUpdateNecessary({
          updateResult: response.result,
          settings: dependencySettings,
          indexIds,
        });
        return {
          state: "SUCCESS",
          result: isNecessary,
          message: response.message,
        };
      }
      return response;
    };

  const getTriggerCallback =
    (
      { dependencies, trigger }: Update,
      settingsIdContext?: SettingsIdContext,
    ): TriggerCallback =>
    (indexIds) =>
    async (dependencySettings) => {
      const response = await getUpdateResults(
        { dependencies, trigger },
        settingsIdContext,
      )(indexIds)(dependencySettings);
      return (newSettings) => {
        if (response.state === "FAIL" || response.state === "SUCCESS") {
          sendAlerts(response.message);
        }
        if (response.state === "SUCCESS") {
          resolveUpdateResults(
            response.result ?? [],
            newSettings,
            indexIds,
            settingsIdContext,
          );
        }
      };
    };

  /**
   * @param globalUpdates from the uischema
   * @returns possibly an immediate induced initial transformation
   */
  const registerUpdates = (
    globalUpdates: Update[],
    settingsIdContext?: SettingsIdContext,
  ): null | ReturnType<TriggerCallback> => {
    let initialTransformation: null | ReturnType<TriggerCallback> = null;
    globalUpdates.forEach((update) => {
      const isActive = getIsUpdateNecessary(update);
      const inducedInitialTransformation = setTrigger(
        update.trigger,
        isActive,
        getTriggerCallback(update, settingsIdContext),
        update.triggerInitially,
        settingsIdContext,
      );
      initialTransformation =
        inducedInitialTransformation ?? initialTransformation;
    });
    return initialTransformation;
  };

  return { registerUpdates, resolveUpdateResults };
};
