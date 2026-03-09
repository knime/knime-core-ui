/**
 * Pattern with group matching, for instance for a string
 * "Column Name (#5)" group 1 denotes "Column Name" and group 2 "5".
 */
const PATTERN = /^(.*) \(#(\d+)\)$/;

/**
 * Gets the first available default column name (Column 1, Column 2, etc.)
 * that doesn't collide with existing names.
 *
 * @param existingNames - Set or array of existing column names
 * @returns The first available default column name
 */
export const getNextDefaultColumnName = (
  existingNames: Set<string> | string[],
): string => {
  const nameSet =
    existingNames instanceof Set ? existingNames : new Set(existingNames);

  let index = 1;
  let name: string;

  do {
    name = `Column ${index++}`;
  } while (nameSet.has(name));

  return name;
};

/**
 * Generates a unique column name by checking against existing names.
 * If the suggested name exists, appends " (#N)" where N is the first available number.
 *
 * @param suggested - The suggested column name
 * @param existingNames - Set or array of existing column names
 * @returns A unique column name
 */
export const getUniqueColumnName = (
  suggested: string,
  existingNames: Set<string> | string[],
): string => {
  const nameSet =
    existingNames instanceof Set ? existingNames : new Set(existingNames);

  const trimmedName = suggested.trim();
  if (trimmedName.length === 0) {
    return getNextDefaultColumnName(nameSet);
  }
  if (!nameSet.has(trimmedName)) {
    return trimmedName;
  }
  let index = 1;
  let baseName = trimmedName;

  const match = PATTERN.exec(baseName);
  if (match) {
    baseName = match[1];
    try {
      index = parseInt(match[2], 10) + 1;
    } catch (e) {
      // If parsing fails (e.g., number out of range), keep index = 1
    }
  }

  let newName: string;
  do {
    newName = `${baseName} (#${index++})`;
  } while (nameSet.has(newName));

  return newName;
};

export const getOtherColumnNames = (
  columns: string[],
  index: number,
): Set<string> => {
  const nameSet = new Set<string>();
  for (let i = 0; i < columns.length; i++) {
    if (i !== index) {
      nameSet.add(columns[i]);
    }
  }
  return nameSet;
};

export const getUniqueColumnNameWithinArray = (
  columns: string[],
  index: number,
) => {
  const existingNames = getOtherColumnNames(columns, index);
  return getUniqueColumnName(columns[index], existingNames);
};
