import type { ColumnType } from "@knime/knime-ui-table";

import type { NodeDialogCoreInitialData } from "../nodeDialog/types/InitialData";

export type ColumnParameters = {
  name: string;
  type: ColumnType;
  values: (string | null)[];
  isInvalidAt?: (boolean | undefined)[];
};

export type TableCreatorParameters = {
  numRows: number;
  columns: ColumnParameters[];
};

export type InitialData = NodeDialogCoreInitialData & {
  data: {
    model: TableCreatorParameters;
  };
  schema: {
    properties: {
      model: {
        properties: {
          columns: {
            items: {
              properties: {
                type: {
                  default: ColumnType;
                };
              };
            };
          };
        };
      };
    };
  };
  initialUpdates: {
    values: {
      id: string;
      type: {
        id: string;
        text: string;
      };
    }[];
  }[];
  flowVariableSettings?: Record<string, any>;
};
