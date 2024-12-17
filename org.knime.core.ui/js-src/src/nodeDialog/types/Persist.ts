/* eslint-disable no-use-before-define */
export type PersistSchema = ConfigInfo &
  (PersistTreeSchema | PersistLeafSchema | PersistArrayParentSchema);

type DeprecatedConfigs = { deprecated: string[][] };

export interface ConfigInfo {
  configKey?: string;
  configPaths?: string[][];
  deprecatedConfigKeys?: DeprecatedConfigs[];
}

export interface PersistTreeSchema {
  type: "object";
  propertiesConfigPaths?: string[][];
  propertiesDeprecatedConfigKeys?: DeprecatedConfigs[];
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
