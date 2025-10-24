import type { SettingsIdContext } from "./useUpdates";

export type StateProviderLocation = (
  | {
      id: string;
    }
  | {
      scope: string;
    }
) & {
  providedOptionName: string;
  settingsId?: SettingsIdContext;
};

type Key = {
  indexIds: string[];
} & StateProviderLocation;

const keySpecifier = ({ indexIds }: Key) => indexIds;

type IndicesKey = {
  indices: number[];
} & StateProviderLocation;

const indicesKeySpecifier = ({ indices }: IndicesKey) => indices;

const isInvokedBy =
  <K extends StateProviderLocation>(getSpecifier: (key: K) => any[]) =>
  (invocationKey: K) =>
  (otherKey: K) => {
    if (
      ("id" in invocationKey && "scope" in otherKey) ||
      ("scope" in invocationKey && "id" in otherKey)
    ) {
      return false;
    }
    if (invocationKey.providedOptionName !== otherKey.providedOptionName) {
      return false;
    }
    if (
      JSON.stringify(invocationKey.settingsId) !==
      JSON.stringify(otherKey.settingsId)
    ) {
      return false;
    }
    if (
      "id" in invocationKey &&
      "id" in otherKey &&
      invocationKey.id !== otherKey.id
    ) {
      return false;
    }
    if (
      "scope" in invocationKey &&
      "scope" in otherKey &&
      invocationKey.scope !== otherKey.scope
    ) {
      return false;
    }
    if (getSpecifier(invocationKey).length > getSpecifier(otherKey).length) {
      return false;
    }
    return getSpecifier(invocationKey).every(
      (value, index) => getSpecifier(otherKey)[index] === value,
    );
  };

const invokes =
  <K extends StateProviderLocation>(getSpecifier: (key: K) => any[]) =>
  (otherKey: K) =>
  (invocationKey: K) =>
    isInvokedBy(getSpecifier)(invocationKey)(otherKey);

const getValuesFromPredicate =
  <K, T>(map: Map<K, T>) =>
  (predicate: (key: K) => boolean) =>
    Array.from(map.entries())
      .filter(([key]) => predicate(key))
      .map(([, value]) => value);

const toMapKey = (
  location: { indexIds?: string[] } & StateProviderLocation,
) => ({
  ...location,
  indexIds: location.indexIds ?? [],
});

const toByIndicesKey = (
  location: { indices?: number[] } & StateProviderLocation,
) => ({
  ...location,
  indices: location.indices ?? [],
});

export default () => {
  const stateProviderListeners = new Map<Key, ((value: unknown) => void)[]>();

  const stateProviderListenersByIndices = new Map<
    IndicesKey,
    ((value: unknown) => void)[]
  >();

  /**
   * States remembered for yet to be registered listeners
   */
  const states = new Map<Key, unknown>();

  const statesByIndices = new Map<IndicesKey, unknown>();

  const getListenersInvokedBy = (key: Key) =>
    getValuesFromPredicate(stateProviderListeners)(
      isInvokedBy(keySpecifier)(key),
    );

  const getListenersByIndicesInvokedBy = (key: IndicesKey) =>
    getValuesFromPredicate(stateProviderListenersByIndices)(
      isInvokedBy(indicesKeySpecifier)(key),
    );

  const getStatesInvoking = (key: Key) =>
    getValuesFromPredicate(states)(invokes(keySpecifier)(key));

  const getStatesByIndicesInvoking = (key: IndicesKey) =>
    getValuesFromPredicate(statesByIndices)(invokes(indicesKeySpecifier)(key));

  const register = <K, V>(map: Map<K, V[]>, key: K, callback: V) => {
    if (map.has(key)) {
      map.get(key)!.push(callback);
    } else {
      map.set(key, [callback]);
    }
  };

  /**
   * We prefer id-based state over index-based state since index-based state is only used for initial updates when no ids are present yet.
   */
  const getCachedStates = (key: Key, byIndicesKey: IndicesKey) => {
    const cachedStates = getStatesInvoking(key);
    if (cachedStates.length > 0) {
      return cachedStates;
    }
    return getStatesByIndicesInvoking(byIndicesKey);
  };

  const addStateProviderListener = (
    location: {
      indexIds?: string[];
      indices?: number[];
    } & StateProviderLocation,
    callback: (value: any) => void,
  ) => {
    const key = toMapKey(location);
    register(stateProviderListeners, key, callback);
    const byIndicesKey = toByIndicesKey(location);
    register(stateProviderListenersByIndices, byIndicesKey, callback);
    getCachedStates(key, byIndicesKey).forEach(callback);
  };

  const callStateProviderListener = (
    location: { indexIds?: string[] } & StateProviderLocation,
    value: unknown,
  ) => {
    const key = toMapKey(location);
    states.set(key, value);
    getListenersInvokedBy(key)
      .flatMap((callbacks) => callbacks)
      .forEach((callback) => callback(value));
  };

  const callStateProviderListenerByIndices = (
    location: { indices: number[] } & StateProviderLocation,
    value: unknown,
  ) => {
    statesByIndices.set(location, value);
    getListenersByIndicesInvokedBy(location)
      .flatMap((callbacks) => callbacks)
      .forEach((callback) => callback(value));
  };

  return {
    addStateProviderListener,
    callStateProviderListener,
    callStateProviderListenerByIndices,
  };
};
