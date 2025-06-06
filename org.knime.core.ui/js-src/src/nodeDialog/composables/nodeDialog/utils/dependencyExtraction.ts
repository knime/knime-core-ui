import { get } from "lodash-es";

import type { IndexIdsValuePairs } from "../../../types/Update";
import {
  type ArrayRecord,
  getOrCreateIdForIndex,
  getOrCreateNestedArrayRecord,
} from "../useArrayIds";

import { combineDataPathsWithIndices, scopeToDataPaths } from "./dataPaths";

/**
 * If there is more than one path, we know that the settings at the first are an array
 */
const hasFurtherPaths = (
  dataPaths: string[],
  settings: object,
): settings is { _id: string | undefined }[] => dataPaths.length > 0;

const getDependencyValuesFromDataPaths = (
  settings: object,
  dataPaths: string[],
  indices: number[],
  arrayRecord: ArrayRecord,
): IndexIdsValuePairs => {
  // eslint-disable-next-line no-use-before-define -- the function is hoisted
  return getDependencyValuesWithoutFixedIndices(
    settings,
    combineDataPathsWithIndices(dataPaths, indices),
    getOrCreateNestedArrayRecord(dataPaths, indices, arrayRecord),
  );
};

export const getDependencyValues = (
  settings: object,
  scope: string,
  indices: number[],
  arrayRecord: ArrayRecord,
): IndexIdsValuePairs => {
  const dataPaths = scopeToDataPaths(scope);
  return getDependencyValuesFromDataPaths(
    settings,
    dataPaths,
    indices,
    arrayRecord,
  );
};

const getOrCreateIdFromSettings = (
  settings: { _id: string | undefined },
  index: number,
  dataPath: string,
  arrayRecord: ArrayRecord,
) => {
  const id = settings._id;
  if (typeof id === "undefined") {
    const { indexId: newId } = getOrCreateIdForIndex(
      arrayRecord,
      dataPath,
      index,
    );
    settings._id = newId;
    return newId;
  }
  return id;
};

// eslint-disable-next-line func-style -- we need to hoist this function
function getDependencyValuesWithoutFixedIndices(
  settings: any,
  dataPaths: string[],
  arrayRecord: ArrayRecord,
): IndexIdsValuePairs {
  const [firstDataPath, ...restDataPaths] = dataPaths;
  const atFirstPath = get(settings, firstDataPath);
  if (hasFurtherPaths(restDataPaths, atFirstPath)) {
    return atFirstPath.flatMap((elementSettings, index) => {
      const id = getOrCreateIdFromSettings(
        elementSettings,
        index,
        firstDataPath,
        arrayRecord,
      );
      return getDependencyValuesFromDataPaths(
        elementSettings,
        restDataPaths,
        [index],
        arrayRecord,
      ).map(({ indices: indexIds, value }) => ({
        indices: [id, ...indexIds],
        value,
      }));
    });
  }
  return [{ indices: [], value: atFirstPath }];
}
