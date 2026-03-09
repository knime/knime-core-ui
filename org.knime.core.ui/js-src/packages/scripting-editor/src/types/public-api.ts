/* eslint-disable @typescript-eslint/no-explicit-any */

/**
 * Utility type that extracts the public API from a class.
 * Automatically excludes private members and constructor.
 * Only includes callable methods (functions).
 *
 * @template T The class type to extract the public API from
 */
export type PublicAPI<T> = Pick<
  T,
  {
    [K in keyof T]: T[K] extends (...args: any[]) => any ? K : never;
  }[keyof T]
>;
