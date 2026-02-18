import { beforeEach, describe, expect, it, vi } from "vitest";

import { useCopyPaste } from "../useCopyPaste";

// Polyfill Blob.text() for jsdom environments where it may be missing
const OriginalBlob = globalThis.Blob;
// @ts-ignore
globalThis.Blob = class extends OriginalBlob {
  private __parts: BlobPart[];
  constructor(parts?: BlobPart[], options?: BlobPropertyBag) {
    super(parts, options);
    this.__parts = parts ? [...parts] : [];
  }

  text(): Promise<string> {
    return Promise.resolve(
      this.__parts.map((p) => (typeof p === "string" ? p : "")).join(""),
    );
  }
};

// Mock ClipboardItem globally
class MockClipboardItem {
  readonly types: string[];
  private data: Record<string, Blob>;

  constructor(data: Record<string, Blob>) {
    this.data = data;
    this.types = Object.keys(data);
  }

  getType(type: string): Promise<Blob> {
    if (this.data[type]) {
      return Promise.resolve(this.data[type]);
    }
    return Promise.reject(new Error(`Type ${type} not found`));
  }
}

// @ts-ignore
globalThis.ClipboardItem = MockClipboardItem;

const mockClipboardWrite = vi.fn().mockResolvedValue(undefined);
const mockClipboardRead = vi.fn().mockResolvedValue([]);

Object.defineProperty(navigator, "clipboard", {
  value: {
    write: mockClipboardWrite,
    read: mockClipboardRead,
  },
  writable: true,
  configurable: true,
});

const createParams = (overrides: Record<string, unknown> = {}) => {
  const defaults = {
    getNumRows: vi.fn(() => 3),
    getNumColumns: vi.fn(() => 3),
    appendColumn: vi.fn(),
    appendRow: vi.fn(),
    setCellArea: vi.fn(),
    getCellValue: vi.fn(
      (col: number, row: number) =>
        ({ value: `r${row}c${col}`, isValid: true }) as {
          value: string;
          isValid: boolean;
        } | null,
    ),
    getColumnName: vi.fn((col: number) => `Col${col}`),
  };
  return { ...defaults, ...overrides };
};

describe("useCopyPaste", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    document.body.style.cursor = "";
  });

  describe("onCopySelection", () => {
    it("builds TSV and HTML, writes to clipboard, sets cursor", async () => {
      const params = createParams();
      const { onCopySelection } = useCopyPaste(params);

      await onCopySelection({
        rect: { x: { min: 0, max: 1 }, y: { min: 0, max: 1 } },
        withHeaders: false,
      });

      expect(mockClipboardWrite).toHaveBeenCalledTimes(1);
      const clipboardItem = mockClipboardWrite.mock.calls[0][0][0];

      const htmlBlob = await clipboardItem.getType("text/html");
      const htmlContent = await htmlBlob.text();
      expect(htmlContent).toContain("<table>");
      expect(htmlContent).not.toContain("<thead>");
      // Verify correct cell order in HTML
      const r0c0Pos = htmlContent.indexOf("<td>r0c0</td>");
      const r0c1Pos = htmlContent.indexOf("<td>r0c1</td>");
      const r1c0Pos = htmlContent.indexOf("<td>r1c0</td>");
      const r1c1Pos = htmlContent.indexOf("<td>r1c1</td>");
      expect(r0c0Pos).toBeGreaterThan(-1);
      expect(r0c0Pos).toBeLessThan(r0c1Pos);
      expect(r0c1Pos).toBeLessThan(r1c0Pos);
      expect(r1c0Pos).toBeLessThan(r1c1Pos);

      const textBlob = await clipboardItem.getType("text/plain");
      const textContent = await textBlob.text();
      expect(textContent).toBe("r0c0\tr0c1\nr1c0\tr1c1\n");

      expect(document.body.style.cursor).toBe("unset");
    });

    it("includes headers when withHeaders is true", async () => {
      const params = createParams();
      const { onCopySelection } = useCopyPaste(params);

      await onCopySelection({
        rect: { x: { min: 0, max: 1 }, y: { min: 0, max: 0 } },
        withHeaders: true,
      });

      const clipboardItem = mockClipboardWrite.mock.calls[0][0][0];

      const htmlBlob = await clipboardItem.getType("text/html");
      const htmlContent = await htmlBlob.text();
      expect(htmlContent).toContain("<thead>");
      expect(htmlContent).toContain("<th>Col0</th>");
      expect(htmlContent).toContain("<th>Col1</th>");
    });

    it("escapes HTML special characters in cell values and headers", async () => {
      const params = createParams({
        getCellValue: vi.fn(() => ({
          value: '<script>alert("xss")</script>&',
          isValid: true,
        })),
        getColumnName: vi.fn(() => 'Header<img src="x">'),
      });
      const { onCopySelection } = useCopyPaste(params);

      await onCopySelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        withHeaders: true,
      });

      const clipboardItem = mockClipboardWrite.mock.calls[0][0][0];
      const htmlBlob = await clipboardItem.getType("text/html");
      const htmlContent = await htmlBlob.text();

      expect(htmlContent).not.toContain("<script>");
      expect(htmlContent).not.toContain('<img src="x">');
      expect(htmlContent).toContain("&lt;script&gt;");
      expect(htmlContent).toContain("&amp;");
      expect(htmlContent).toContain('&lt;img src="x"&gt;');
    });

    it("handles null cell values (data-is-empty)", async () => {
      const params = createParams({
        getCellValue: vi.fn(() => null),
      });
      const { onCopySelection } = useCopyPaste(params);

      await onCopySelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        withHeaders: false,
      });

      const clipboardItem = mockClipboardWrite.mock.calls[0][0][0];

      const htmlBlob = await clipboardItem.getType("text/html");
      const htmlContent = await htmlBlob.text();
      expect(htmlContent).toContain("data-is-empty");

      const textBlob = await clipboardItem.getType("text/plain");
      const textContent = await textBlob.text();
      expect(textContent).toBe("\n");
    });
  });

  describe("onPasteSelection", () => {
    const makeUpdateSelection = () => vi.fn();

    it("reads HTML from clipboard and pastes into table", async () => {
      const html =
        "<table><tbody><tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></tbody></table>";
      const clipboardItem = new MockClipboardItem({
        "text/html": new Blob([html], { type: "text/html" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = makeUpdateSelection();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 1, max: 1 }, y: { min: 1, max: 1 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).toHaveBeenCalledWith(1, 1, [
        [
          { value: "a", isValid: true },
          { value: "b", isValid: true },
        ],
        [
          { value: "c", isValid: true },
          { value: "d", isValid: true },
        ],
      ]);
      expect(updateSelection).toHaveBeenCalledWith({
        minX: 1,
        minY: 1,
        maxX: 2,
        maxY: 2,
      });
    });

    it("falls back to plain text TSV when HTML has no table", async () => {
      const clipboardItem = new MockClipboardItem({
        "text/html": new Blob(["<p>not a table</p>"], { type: "text/html" }),
        "text/plain": new Blob(["x\ty\nz\tw\n"], { type: "text/plain" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = makeUpdateSelection();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).toHaveBeenCalledWith(0, 0, [
        [
          { value: "x", isValid: true },
          { value: "y", isValid: true },
        ],
        [
          { value: "z", isValid: true },
          { value: "w", isValid: true },
        ],
      ]);
    });

    it("falls back to plain text when HTML type is not available", async () => {
      const clipboardItem = new MockClipboardItem({
        "text/plain": new Blob(["val1\tval2\n"], { type: "text/plain" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = makeUpdateSelection();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).toHaveBeenCalledWith(0, 0, [
        [
          { value: "val1", isValid: true },
          { value: "val2", isValid: true },
        ],
      ]);
    });

    it("appends columns and rows when paste area exceeds table dimensions", async () => {
      let numCols = 2;
      let numRows = 2;
      const params = createParams({
        getNumColumns: vi.fn(() => numCols),
        getNumRows: vi.fn(() => numRows),
        appendColumn: vi.fn(() => {
          numCols++;
        }),
        appendRow: vi.fn(() => {
          numRows++;
        }),
      });

      const html =
        "<table><tbody><tr><td>a</td><td>b</td><td>c</td></tr><tr><td>d</td><td>e</td><td>f</td></tr></tbody></table>";
      const clipboardItem = new MockClipboardItem({
        "text/html": new Blob([html], { type: "text/html" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = makeUpdateSelection();
      const { onPasteSelection } = useCopyPaste(params);

      // Paste starting at col 1, row 1. Need cols up to 1+3=4, rows up to 1+2=3
      await onPasteSelection({
        rect: { x: { min: 1, max: 1 }, y: { min: 1, max: 1 } },
        id: null,
        updateSelection,
      });

      expect(params.appendColumn).toHaveBeenCalledTimes(2);
      expect(params.appendRow).toHaveBeenCalledTimes(1);
    });

    it("does nothing when clipboard is empty", async () => {
      mockClipboardRead.mockResolvedValueOnce([]);

      const updateSelection = makeUpdateSelection();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).not.toHaveBeenCalled();
      expect(updateSelection).not.toHaveBeenCalled();
    });

    it("does nothing when clipboard item has no usable types", async () => {
      const clipboardItem = new MockClipboardItem({});
      // types array is empty, so neither text/html nor text/plain is available
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = makeUpdateSelection();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).not.toHaveBeenCalled();
      expect(updateSelection).not.toHaveBeenCalled();
    });

    it("handles data-is-empty attribute for null cells in HTML paste", async () => {
      const html =
        "<table><tbody><tr><td>a</td><td data-is-empty></td></tr></tbody></table>";
      const clipboardItem = new MockClipboardItem({
        "text/html": new Blob([html], { type: "text/html" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = makeUpdateSelection();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).toHaveBeenCalledWith(0, 0, [
        [{ value: "a", isValid: true }, null],
      ]);
    });

    it("handles empty text content in plain text fallback", async () => {
      const clipboardItem = new MockClipboardItem({
        "text/plain": new Blob([""], { type: "text/plain" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = makeUpdateSelection();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).not.toHaveBeenCalled();
      expect(updateSelection).not.toHaveBeenCalled();
    });
  });

  describe("round-trip copy/paste", () => {
    it("preserves null cells through copy and paste via HTML", async () => {
      // Copy phase: cell at (0,0) is null
      const params = createParams({
        getCellValue: vi.fn((col: number) =>
          col === 0 ? null : { value: "val", isValid: true },
        ),
      });
      const { onCopySelection, onPasteSelection } = useCopyPaste(params);

      // Capture what gets written to clipboard
      let capturedItem: any;
      mockClipboardWrite.mockImplementationOnce((items: any[]) => {
        capturedItem = items[0];
        return Promise.resolve();
      });

      await onCopySelection({
        rect: { x: { min: 0, max: 1 }, y: { min: 0, max: 0 } },
        withHeaders: false,
      });

      // Use captured item for paste
      mockClipboardRead.mockResolvedValueOnce([capturedItem]);
      const updateSelection = vi.fn();

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).toHaveBeenCalledWith(0, 0, [
        [null, { value: "val", isValid: true }],
      ]);
    });
  });

  describe("parseTsv edge cases (via paste)", () => {
    it("handles TSV without trailing newline", async () => {
      const clipboardItem = new MockClipboardItem({
        "text/plain": new Blob(["a\tb"], { type: "text/plain" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = vi.fn();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).toHaveBeenCalledWith(0, 0, [
        [
          { value: "a", isValid: true },
          { value: "b", isValid: true },
        ],
      ]);
    });

    it("handles single-cell TSV", async () => {
      const clipboardItem = new MockClipboardItem({
        "text/plain": new Blob(["hello\n"], { type: "text/plain" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = vi.fn();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).toHaveBeenCalledWith(0, 0, [
        [{ value: "hello", isValid: true }],
      ]);
      expect(updateSelection).toHaveBeenCalledWith({
        minX: 0,
        minY: 0,
        maxX: 0,
        maxY: 0,
      });
    });
  });

  describe("parseHtmlTable edge cases (via paste)", () => {
    it("handles th elements in HTML table", async () => {
      const html =
        "<table><thead><tr><th>H1</th><th>H2</th></tr></thead><tbody><tr><td>a</td><td>b</td></tr></tbody></table>";
      const clipboardItem = new MockClipboardItem({
        "text/html": new Blob([html], { type: "text/html" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = vi.fn();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      // Both th and td rows are parsed
      expect(params.setCellArea).toHaveBeenCalledWith(0, 0, [
        [
          { value: "H1", isValid: true },
          { value: "H2", isValid: true },
        ],
        [
          { value: "a", isValid: true },
          { value: "b", isValid: true },
        ],
      ]);
    });

    it("parses HTML-escaped content back to original values", async () => {
      const html =
        '<table><thead><tr><th>&lt;b&gt;Header&lt;/b&gt;</th></tr></thead><tbody><tr><td>&lt;script&gt;alert("xss")&lt;/script&gt;&amp;</td></tr></tbody></table>';
      const clipboardItem = new MockClipboardItem({
        "text/html": new Blob([html], { type: "text/html" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = vi.fn();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).toHaveBeenCalledWith(0, 0, [
        [{ value: "<b>Header</b>", isValid: true }],
        [{ value: '<script>alert("xss")</script>&', isValid: true }],
      ]);
    });

    it("handles cell with no textContent as empty string", async () => {
      const html = "<table><tbody><tr><td></td></tr></tbody></table>";
      const clipboardItem = new MockClipboardItem({
        "text/html": new Blob([html], { type: "text/html" }),
      });
      mockClipboardRead.mockResolvedValueOnce([clipboardItem]);

      const updateSelection = vi.fn();
      const params = createParams();
      const { onPasteSelection } = useCopyPaste(params);

      await onPasteSelection({
        rect: { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } },
        id: null,
        updateSelection,
      });

      expect(params.setCellArea).toHaveBeenCalledWith(0, 0, [
        [{ value: "", isValid: true }],
      ]);
    });
  });
});
