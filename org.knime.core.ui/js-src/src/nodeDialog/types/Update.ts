export interface Scoped {}

export interface ValueTrigger extends Scoped {
  /**
   * The schema paths of the setting (including "/properties/" and possibly "/items/properties/")
   */
  scope: string;
}

export interface IdTrigger {
  id: string;
}

export type Trigger = ValueTrigger | IdTrigger;

export const isValueTrigger = (trigger: Trigger): trigger is ValueTrigger =>
  "scope" in trigger && Boolean(trigger.scope);

export interface Update {
  /**
   * The json schema scopes of dependency that the frontend needs to provide when requesting an update from the backend
   */
  dependencies: string[];
  /**
   * The trigger of this update
   */
  trigger: Trigger;
  /**
   * Whether the trigger is an initial trigger
   */
  triggerInitially?: boolean;
}

export type IndicesValuePairs = { indices: number[]; value: unknown }[]; // synchronous computeBeforeOpenDialog results
export type IndexIdsValuePairs = { indices: string[]; value: unknown }[]; // asynchronous results
export type Pairs = IndicesValuePairs | IndexIdsValuePairs;

// value updates
export interface ValueUpdateResult {
  scope: string;
  values: Pairs;
}

export interface LocationUiStateUpdateResult {
  scope: string;
  providedOptionName: string;
  values: Pairs;
}

// ui state updates
export interface IdUiStateUpdateResult {
  id: string;
  providedOptionName: string;
  values: Pairs;
}

export type UpdateResult =
  | ValueUpdateResult
  | LocationUiStateUpdateResult
  | IdUiStateUpdateResult;

export const isValueUpdateResult = (
  result: UpdateResult,
): result is ValueUpdateResult =>
  "scope" in result && !("providedOptionName" in result);

export const isLocationBased = (
  result: LocationUiStateUpdateResult | IdUiStateUpdateResult,
): result is LocationUiStateUpdateResult => "scope" in result;
