import { ref } from "vue";

type Id = string; // NOSONAR marker type

/**
 * Maintains a list of stable identifiers id-row-0, id-row-1, ... for rows and id-column-0, id-column-1, ... for columns.
 * Hereby the index does not necessarily match the index of the row/column in the table, as rows/columns can be deleted.
 * This allows to keep track of validation results for specific cells even when rows/columns are deleted.
 */
const useIdentifiers = (prefix: "row" | "column") => {
  const identifiers = ref<Id[]>([]);
  let nextIndex = 0;

  const createNewIdentifier = (): Id => {
    return `id-${prefix}-${nextIndex++}`;
  };

  const deleteIdentifier = (index: number): Id | undefined =>
    identifiers.value.splice(index, 1)[0];
  const appendIdentifier = () => identifiers.value.push(createNewIdentifier());
  const getIdentifierIndex = (id: Id): number | null => {
    const index = identifiers.value.findIndex(
      (existingId) => existingId === id,
    );
    if (index === -1) {
      return null;
    }
    return index;
  };
  const getIdentifier = (index: number): Id | null => {
    if (index < 0 || index >= identifiers.value.length) {
      return null;
    }
    return identifiers.value[index];
  };

  return {
    identifiers,
    deleteIdentifier,
    appendIdentifier,
    getIdentifierIndex,
    getIdentifier,
  };
};

export const useCellIdentifiers = () => {
  const {
    identifiers: columnIds,
    deleteIdentifier: deleteColumnId,
    appendIdentifier: appendColumnId,
    getIdentifierIndex: getColumnIdIndex,
    getIdentifier: getColumnId,
  } = useIdentifiers("column");
  const {
    identifiers: rowIds,
    deleteIdentifier: deleteRowId,
    appendIdentifier: appendRowId,
    getIdentifierIndex: getRowIdIndex,
    getIdentifier: getRowId,
  } = useIdentifiers("row");
  return {
    columnIds,
    rowIds,
    deleteColumnId,
    deleteRowId,
    appendColumnId,
    appendRowId,
    getColumnIdIndex,
    getRowIdIndex,
    getColumnId,
    getRowId,
  };
};
