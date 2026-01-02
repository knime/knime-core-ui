import type { CellData } from "./useTableConfig";

const buildTsv = (
  withHeaders: boolean,
  headers: string[],
  rowValuesArray: (string | null)[][],
) => {
  let csvContent = "";
  if (withHeaders) {
    csvContent += headers.join("\t") + "\n";
  }
  for (let rowValues of rowValuesArray) {
    csvContent += rowValues.join("\t") + "\n";
  }
  return csvContent;
};

const parseTsv = (tsv: string): string[][] => {
  const rows = tsv.split("\n");
  if (rows.length > 0 && rows[rows.length - 1] === "") {
    rows.pop(); // Remove trailing empty line if present
  }
  return rows.map((row) => row.split("\t"));
};

const parseHtmlTable = (html: string): (string | null)[][] => {
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");
  const table = doc.querySelector("table");
  if (!table) {
    return [];
  }
  const rows = Array.from(table.querySelectorAll("tr"));
  return rows.map((row) =>
    Array.from(row.querySelectorAll("td, th")).map((cell) =>
      cell.hasAttribute("data-is-empty") ? null : cell.textContent || "",
    ),
  );
};

const buildHtmlTable = (
  withHeaders: boolean,
  headers: string[],
  rowValuesArray: (string | null)[][],
) => {
  let htmlContent = "<table>";
  if (withHeaders) {
    htmlContent += "<thead><tr>";
    headers.forEach((header) => {
      htmlContent += `<th>${header}</th>`;
    });
    htmlContent += "</tr></thead>";
  }
  htmlContent += "<tbody>";
  rowValuesArray.forEach((rowValues) => {
    htmlContent += "<tr>";
    rowValues.forEach((value) => {
      if (value === null) {
        htmlContent += `<td data-is-empty></td>`;
      } else {
        htmlContent += `<td>${value}</td>`;
      }
    });
    htmlContent += "</tr>";
  });
  htmlContent += "</tbody></table>";
  return htmlContent;
};

const getRowsFromClipboard = async (): Promise<(string | null)[][]> => {
  const clipboardItems = await navigator.clipboard.read();

  for (const item of clipboardItems) {
    // Try HTML first (preserves empty cell info)
    if (item.types.includes("text/html")) {
      const htmlBlob = await item.getType("text/html");
      const htmlContent = await htmlBlob.text();
      const rows = parseHtmlTable(htmlContent);
      if (rows.length > 0) {
        return rows;
      }
    }

    // Fall back to plain text (TSV)
    if (item.types.includes("text/plain")) {
      const textBlob = await item.getType("text/plain");
      const textContent = await textBlob.text();
      if (textContent) {
        return parseTsv(textContent);
      }
    }
  }

  return [];
};

export const useCopyPaste = ({
  getNumRows,
  getNumColumns,
  appendColumn,
  appendRow,
  setCellArea,
  getCellValue,
  getColumnName,
}: {
  getNumRows: () => number;
  getNumColumns: () => number;
  appendColumn: () => void;
  appendRow: () => void;
  setCellArea: (
    colIndex: number,
    rowIndex: number,
    values: CellData[][],
  ) => void;
  getCellValue: (colIndex: number, rowIndex: number) => CellData;
  getColumnName: (colIndex: number) => string;
}) => {
  const setCellAreaString = (
    colIndex: number,
    rowIndex: number,
    values: (string | null)[][],
  ) => {
    setCellArea(
      colIndex,
      rowIndex,
      values.map((row) =>
        row.map((cellValue) =>
          cellValue === null ? null : { value: cellValue, isValid: true },
        ),
      ),
    );
  };

  const createClipboardItem = (
    x: { min: number; max: number },
    y: { min: number; max: number },
    withHeaders: boolean,
  ) => {
    const headers = Array.from({ length: x.max - x.min + 1 }, (_, i) =>
      getColumnName(x.min + i),
    );
    const rowValuesArray = Array.from({ length: y.max - y.min + 1 }, (_, i) =>
      Array.from(
        { length: x.max - x.min + 1 },
        (_, j) => getCellValue(x.min + j, y.min + i)?.value ?? null,
      ),
    );
    const csvContent = buildTsv(withHeaders, headers, rowValuesArray);
    const htmlContent = buildHtmlTable(withHeaders, headers, rowValuesArray);
    const blobHTML = new Blob([htmlContent], { type: "text/html" });
    const blobCSV = new Blob([csvContent], { type: "text/plain" });
    return new ClipboardItem({
      [blobHTML.type]: blobHTML,
      [blobCSV.type]: blobCSV,
    });
  };

  const onCopySelection = async ({
    rect: { x, y },
    withHeaders,
  }: {
    rect: { x: { min: number; max: number }; y: { min: number; max: number } };
    withHeaders: boolean;
  }) => {
    document.body.style.cursor = "wait";
    try {
      const clipboardItem = createClipboardItem(x, y, withHeaders);
      await navigator.clipboard.write([clipboardItem]);
    } catch (error) {
      console.error("Failed to copy content to clipboard:", error);
    }
    document.body.style.cursor = "unset";
  };

  // Handle paste into table
  const onPasteSelection = async ({
    rect: { x, y },
    updateSelection,
  }: {
    rect: { x: { min: number; max: number }; y: { min: number; max: number } };
    id: null | number;
    updateSelection: (newRect: {
      minX: number;
      minY: number;
      maxX: number;
      maxY: number;
    }) => void;
  }) => {
    try {
      const rows = await getRowsFromClipboard();
      if (rows.length === 0) {
        return;
      }
      const numCols = rows[0].length;
      while (getNumColumns() < x.min + numCols) {
        appendColumn();
      }
      const numRows = rows.length;
      while (getNumRows() < y.min + numRows) {
        appendRow();
      }

      setCellAreaString(x.min, y.min, rows);

      // Update the selection to cover the pasted area
      const newMaxX = x.min + numCols - 1;
      const newMaxY = y.min + numRows - 1;

      updateSelection({
        minX: x.min,
        minY: y.min,
        maxX: newMaxX,
        maxY: newMaxY,
      });
    } catch (error) {
      console.error("Failed to paste content:", error);
    }
  };

  return {
    onCopySelection,
    onPasteSelection,
  };
};
