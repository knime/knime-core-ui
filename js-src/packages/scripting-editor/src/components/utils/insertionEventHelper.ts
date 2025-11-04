export type InsertionEvent = {
  textToInsert: string;
  extraArgs?: { [name: string]: any };
};

type InsertionListener = (event: InsertionEvent) => void;

const createInsertionEventHelper = () => {
  const insertionEventHelpers: Record<string, InsertionListener[]> = {};

  const getInsertionEventHelper = (eventType: string) => {
    if (!insertionEventHelpers[eventType]) {
      insertionEventHelpers[eventType] = [];
    }

    return {
      registerInsertionListener: (listener: InsertionListener): void => {
        insertionEventHelpers[eventType].push(listener);
      },
      handleInsertion: (event: InsertionEvent): void => {
        insertionEventHelpers[eventType].forEach((listener) => listener(event));
      },
    };
  };

  return {
    getInsertionEventHelper,
  };
};

export const insertionEventHelper = createInsertionEventHelper();
