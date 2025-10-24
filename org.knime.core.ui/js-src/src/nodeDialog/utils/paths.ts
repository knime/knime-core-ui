import type { ConfigPath } from "../composables/components/useFlowVariables";
import type { PersistSchema, PersistTreeSchema } from "../types/Persist";

export const composePaths = (path1: string, path2: string) => {
  if (path1 === "") {
    return path2;
  }
  return `${path1}.${path2}`;
};

const resolveIndexTemplate = (
  path: string,
  lastIndexSegment: string | null,
) => {
  if (lastIndexSegment === null) {
    return path;
  }
  return path.replace(/\$\{array_index\}/g, lastIndexSegment);
};

const pathToPersistSchema = new Map<string, PersistTreeSchema>();

export const addPersistSchemaForPath = (
  path: string,
  schema: PersistTreeSchema,
) => {
  pathToPersistSchema.set(path, schema);
};

const isDynamicParametersSchema = (
  schema: PersistSchema,
  parentPath: string,
): schema is PersistTreeSchema => {
  return pathToPersistSchema.has(parentPath);
};

/**
 * Composes multiple path segments into a single path.
 *
 * Also this method replaces occurrences of the templating syntax '${array_index}'
 * with the actual index value, if lastIndexSegment is provided. This does not work
 * for multiple array layouts in a single path.
 *
 * @param paths array of path segments possibly containing ".."
 * @param lastIndexSegment last segment if the path points to an array item, null otherwise
 * @returns composed path
 */
const resolveToDotSeparatedPath = (
  paths: string[],
  lastIndexSegment: string | null,
) => {
  const result: string[] = [];
  for (const path of paths) {
    if (path === "..") {
      if (result.length === 0) {
        throw new Error("Cannot go up from root path");
      }
      result.pop();
    } else {
      result.push(resolveIndexTemplate(path, lastIndexSegment));
    }
  }
  return result.join(".");
};

/**
 * E.g. [[1, 2], [3]] + [[4], [5, 6]] => [[1, 2, 4], [1, 2, 5, 6], [3, 4], [3, 5, 6]]
 */
const combinePaths = (paths1: string[][], paths2: string[][]): string[][] =>
  paths1.flatMap((p1) => paths2.map((p2) => [...p1, ...p2]));

const getNextConfigPathSegments = ({
  schema,
  segment,
}: {
  schema: PersistSchema;
  segment: string;
}): { configPaths: string[][]; continueTraversal: boolean } => {
  const configPaths = schema.configPaths;
  if (typeof configPaths === "undefined") {
    const configKey = schema.configKey ?? segment;
    return { configPaths: [[configKey]], continueTraversal: true };
  }
  return { configPaths, continueTraversal: false };
};

const getSubConfigKeysRecursive = (
  schema: PersistSchema,
  prefix: string[],
): string[][] => {
  if (!schema) {
    return [];
  }

  if (schema.type === "array") {
    return getSubConfigKeysRecursive(schema.items, prefix);
  } else if (schema.type === "object") {
    if (schema.propertiesConfigPaths) {
      return schema.propertiesConfigPaths;
    }
    const propertiesRoute = schema.propertiesRoute ?? [];
    const subConfigKeys: string[][] = [];
    Object.entries(schema.properties).forEach(([key, subschema]) => {
      const route = [...propertiesRoute, ...(subschema.route ?? [])];
      const { configPaths, continueTraversal } = getNextConfigPathSegments({
        schema: subschema,
        segment: key,
      });

      configPaths
        .map((configPath) => [...prefix, ...route, ...configPath])
        .forEach((newPrefix) =>
          continueTraversal
            ? subConfigKeys.push(
                ...getSubConfigKeysRecursive(schema.properties[key], newPrefix),
              )
            : subConfigKeys.push(newPrefix),
        );
    });
    return subConfigKeys;
  }
  return prefix.length ? [prefix] : [];
};

/**
 * Unless custom sub config keys are found in the given schema, sub config keys are inferred by traversing the schema
 * depth-first, replacing encountered segments with config keys, if custom config keys are found in the schema (as in
 * @see getConfigPaths). Further traversal at any segment ends prematurely if custom config keys are found in
 * the segment's schema.
 */
export const getSubConfigKeys = (schema: PersistSchema): string[][] => {
  const subConfigKeys = getSubConfigKeysRecursive(schema, []);
  return subConfigKeys.length ? subConfigKeys : [[]];
};

/**
 * Config (persist) paths are assembled by traversing the persist schema along the given path, replacing any
 * segments along the traversal with custom config keys, if such custom config keys are found in any of the segments'
 * schemas. Potential sub config keys are then appended to to the determined config paths.
 *
 * Data (JsonForms schema) paths are assembled by concatenating the given path with any potential sub config keys of the
 * given control's schema.
 *
 * Note that there exists at least one data path in any case.
 *
 * @see getSubConfigKeys for details on how subConfigKeys are determined.
 */
export const getConfigPaths = ({
  path,
  persistSchema,
}: {
  persistSchema: PersistSchema;
  path: string;
}): ConfigPath[] => {
  const segments = path.split(".");
  let parentPath = "";
  let configPaths: string[][] = [[]];
  let schema = persistSchema;

  let traversalIsAborted = false;
  let lastIndexSegment: null | string = null;
  const deprecatedConfigPaths: string[][] = [];
  for (const segment of segments) {
    if (traversalIsAborted) {
      break;
    }
    if (schema.type === "array") {
      configPaths = configPaths.map((p) => [...p, segment]);
      lastIndexSegment = segment;
      schema = schema.items;
    } else if (
      schema.type === "object" ||
      isDynamicParametersSchema(schema, parentPath)
    ) {
      const parentPropertiesRoute = schema.propertiesRoute ?? [];
      const getRoutedParentPaths = () =>
        configPaths.map((parent) => [...parent, ...parentPropertiesRoute]);
      (schema.propertiesDeprecatedConfigKeys ?? []).forEach(({ deprecated }) =>
        deprecatedConfigPaths.push(
          ...combinePaths(getRoutedParentPaths(), deprecated),
        ),
      );
      if (schema.propertiesConfigPaths) {
        traversalIsAborted = true;
        const propertiesConfigPaths = schema.propertiesConfigPaths;
        configPaths = combinePaths(
          getRoutedParentPaths(),
          propertiesConfigPaths,
        );
        continue;
      }
      if (pathToPersistSchema.has(parentPath)) {
        schema = pathToPersistSchema.get(parentPath)!;
      }
      schema = schema.properties[segment] ?? {};

      const route = [...parentPropertiesRoute, ...(schema.route ?? [])];
      const routedConfigPaths = combinePaths(configPaths, [route]);

      (schema.deprecatedConfigKeys ?? []).forEach(({ deprecated }) =>
        deprecatedConfigPaths.push(
          ...combinePaths(routedConfigPaths, deprecated),
        ),
      );

      const { configPaths: nextPathSegments, continueTraversal } =
        getNextConfigPathSegments({ schema, segment });
      traversalIsAborted = !continueTraversal;

      configPaths = combinePaths(routedConfigPaths, nextPathSegments);
    } else {
      configPaths = configPaths.map((parent) => [...parent, segment]);
    }
    parentPath = composePaths(parentPath, segment);
  }

  let dataPaths = [segments];
  if (!traversalIsAborted) {
    const subConfigKeys = getSubConfigKeys(schema);
    configPaths = combinePaths(configPaths, subConfigKeys);
    dataPaths = combinePaths(dataPaths, subConfigKeys);
  }
  return configPaths.map((configPath, index) => ({
    configPath: resolveToDotSeparatedPath(configPath, lastIndexSegment),
    dataPath: resolveToDotSeparatedPath(
      dataPaths.length === 1 ? dataPaths[0] : dataPaths[index],
      lastIndexSegment,
    ),
    deprecatedConfigPaths: deprecatedConfigPaths.map((dep) =>
      resolveToDotSeparatedPath(dep, lastIndexSegment),
    ),
  }));
};

/**
 * Determines the longest common / shared prefix among a given array of paths.
 */
export const getLongestCommonPrefix = (paths: string[]) => {
  if (!paths.length) {
    return "";
  }
  const segments = paths[0].split(".").slice(0, -1);
  let prefix = "";
  for (const segment of segments) {
    for (let j = 1; j < paths.length; j++) {
      if (!paths[j].startsWith(prefix + segment)) {
        return paths[0].slice(0, prefix.length);
      }
    }
    prefix += segment.concat(".");
  }
  return prefix;
};
