/* eslint-disable no-use-before-define */
export type PersistSchema = ConfigInfo &
  (PersistTreeSchema | PersistLeafSchema | PersistArrayParentSchema);

type DeprecatedConfigs = { deprecated: string[][] };

export interface ConfigInfo {
  configKey?: string;
  configPaths?: string[][];
  deprecatedConfigKeys?: DeprecatedConfigs[];
  /**
   * Route to the parent config path relative to the current parent.
   * Applied before all other config path manipulations (like configKey or configPaths).
   * If the route contains "..", it is resolved against the current path.
   * E.g. if the current path is ["a","b","c"] and the route is ["..","d"], the resulting
   * path will be ["a","b","d"].
   */
  route?: string[];
}

export interface PersistTreeSchema {
  type: "object";
  propertiesConfigPaths?: string[][];
  /**
   * Same effect as if the respective `route` was applied to all child properties.
   * Overwritten by child's own `route` if present.
   */
  propertiesDeprecatedConfigKeys?: DeprecatedConfigs[];
  propertiesRoute?: string[];
  properties: {
    [key: string]: PersistSchema;
  };
}

export interface PersistLeafSchema {
  type?: "leaf";
}

export interface PersistArrayParentSchema {
  type: "array";
  items: PersistSchema;
}
