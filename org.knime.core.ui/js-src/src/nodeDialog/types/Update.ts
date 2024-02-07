export interface ValueReference {
  /**
   * The schema path of the setting
   */
  scope: string;
  /**
   * A unique identifyer
   */
  id: string;
}

export interface Update {
  /**
   * The dependencies that the frontend needs to provide when requesting an update from the backend
   */
  dependencies: ValueReference[];
  /**
   * The trigger of this update
   */
  trigger: ValueReference;
}

export interface PathAndValue {
  path: string;
  value: unknown;
}
