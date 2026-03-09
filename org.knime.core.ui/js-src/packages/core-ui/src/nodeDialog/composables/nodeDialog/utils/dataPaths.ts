/**
 * E.g.
 * * dataPaths = ['foo', 'bar', 'baz'], indices = [1, 2] -->  ['foo.1.bar.2.baz']
 * * dataPaths = ['foo', 'bar', 'baz'], indices = [1] -->  ['foo.1.bar', 'baz']
 * * dataPaths = ['foo', 'bar', 'baz'], indices = [] -->  ['foo', 'bar', 'baz']
 */
export const combineDataPathsWithIndices = (
  dataPaths: string[],
  indices: number[],
) => {
  return dataPaths.reduce((segments, dataPath, i) => {
    if (i === 0) {
      segments[0] = dataPath;
    } else if (i <= indices.length) {
      segments[0] = `${segments[0]}.${indices[i - 1]}.${dataPath}`;
    } else {
      segments.push(dataPath);
    }
    return segments;
  }, [] as string[]);
};

/**
 * E.g.
 * * scope = '#/properties/foo/items/properties/bar/items/properties/baz'
 *   --> ['foo', 'bar', 'baz']
 * * scope = '#/properties/items/properties/foo/items/properties/bar' --> ['items.foo', 'bar']
 */
export const scopeToDataPaths = (scope: string): string[] => {
  const segments = scope.replace(/^#\//, "").split("/");

  const result: string[] = [];
  let currentPath: string[] = [];

  for (let i = 0; i < segments.length; i++) {
    const segment = segments[i];

    if (segment === "properties") {
      i++; // NOSONAR handle next segment right away
      if (segments.length === i) {
        throw new Error("Invalid scope: properties without a name");
      }
      currentPath.push(segments[i]);
    } else if (segment === "items") {
      result.push(currentPath.join("."));
      currentPath = [];
    } else {
      throw Error(`Unexpected segment in scope: ${segment}`);
    }
  }
  result.push(currentPath.join("."));
  return result;
};
/**
 * @returns an array of paths. If there are multiple, all but the first one lead to an array in
 * which every index is to be adjusted.
 */
export const combineScopeWithIndices = (scope: string, indices: number[]) => {
  return combineDataPathsWithIndices(scopeToDataPaths(scope), indices);
};

/**
 * E.g.
 *  removeFromStart(["A", "B"], ["A"]) === ["B"]
 *  removeFromStart(["A"], ["A", "B"]) === []
 *  removeFromStart(["C", "B"], ["A"]) === null
 */
const removeFromStart = (segments: string[], startingSegments: string[]) => {
  const lengthMin = Math.min(segments.length, startingSegments.length);
  for (let i = 0; i < lengthMin; i++) {
    if (segments[i] !== startingSegments[i]) {
      return null;
    }
  }
  return segments.slice(startingSegments.length);
};

/**
 * This method checks whether the given path segments can be obtained by combining the dataPathSegments with
 * intermediate numbers. If so, these numbers are returned. If not, the result is null.
 */
const testMatchesAndGetIndices = (
  segments: string[],
  dataPathSegments: string[][],
): null | number[] => {
  if (segments.length === 0) {
    return dataPathSegments.length > 1 ? null : [];
  }
  const segmentsWithoutFirstDataPath = removeFromStart(
    segments,
    dataPathSegments[0],
  );
  if (segmentsWithoutFirstDataPath === null) {
    return null;
  }
  if (dataPathSegments.length === 1) {
    return [];
  }
  if (segmentsWithoutFirstDataPath.length === 0) {
    return null;
  }
  const [needsToBeANumber, ...rest] = segmentsWithoutFirstDataPath;
  const nextNumber = parseInt(needsToBeANumber, 10);
  if (isNaN(nextNumber)) {
    return null;
  }
  const restMatchingIndices = testMatchesAndGetIndices(
    rest,
    dataPathSegments.slice(1),
  );
  return restMatchingIndices === null
    ? null
    : [nextNumber, ...restMatchingIndices];
};

const splitBy = (splitter: string) => (str: string) => str.split(splitter);

/**
 * Extracts the indices from the pathSegments so that the resulting gaps between these numbers match the given dataPaths.
 * E.g.
 *  * dataPaths: [["lorem.ipsum", "dolor"]], pathSegments: ["lorem", "ipsum", "123", "dolor"]
 *     --> [123]
 *  * dataPaths: [["lorem", "ipsum.dolor"]], pathSegments: ["lorem", "123", "ipsum"]
 *     --> [123]
 *  * dataPaths: [["lorem", "ipsum", "dolor"]], pathSegments: ["lorem", "123", "ipsum", "45"]
 *     --> [123, 45]
 * * dataPaths: [["lorem", "ipsum", "dolor"]], pathSegments: ["lorem", "123", "ipsum", "45", "dolor"]
 *     --> [123, 45]
 */
export const getIndicesFromDataPaths = (
  dataPaths: string[][],
  pathSegments: string[],
) => {
  let result: null | { dataPath: string[]; indices: number[] } = null;
  const isMoreSpecificMatch = (newIndices: number[]) => {
    return result === null || newIndices.length > result.indices.length;
  };
  for (const dependency of dataPaths) {
    const indices = testMatchesAndGetIndices(
      pathSegments,
      dependency.map(splitBy(".")),
    );
    if (indices && isMoreSpecificMatch(indices)) {
      result = { indices, dataPath: dependency };
    }
  }
  return result;
};
