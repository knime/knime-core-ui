<script setup lang="ts">
import "../common/main.css";
import { computed, inject, onMounted, ref } from "vue";

import {
  JsonDataService,
  type UIExtensionService,
} from "@knime/ui-extension-service";

import Tile from "./Tile.vue";
import type { TileCell } from "./Tile.vue";

const getKnimeService = (inject("getKnimeService") ??
  (() => null)) as () => UIExtensionService;

// ── Types ────────────────────────────────────────────────────────────────────

interface SpecialChoice<T extends string> {
  specialChoice: T;
}
interface ColumnChoice {
  value: string;
}
type TitleColumn =
  | SpecialChoice<"ROW_ID">
  | SpecialChoice<"NONE">
  | ColumnChoice;
type ColorColumn = SpecialChoice<"NONE"> | ColumnChoice;

interface TileViewSettings {
  title?: string;
  displayedColumns: object;
  titleColumn: TitleColumn;
  colorColumn: ColorColumn;
  tilesPerRow: number;
  displayColumnHeaders: boolean;
  textAlignment: "LEFT" | "CENTER" | "RIGHT";
  pageSize: number;
  selectionMode: string;
  showOnlySelectedRows: boolean;
  showOnlySelectedRowsConfigurable: boolean;
  showClearSelectionButton: boolean;
  showSelectAllButton: boolean;
}

interface TableData {
  totalSelected: number;
  columnContentTypes: string[];
  rowCount: number;
  rows: string[][];
  rowIndices: number[];
  displayedColumns: string[];
  columnCount: number;
}

interface InitialData {
  settings: TileViewSettings;
  table: TableData;
}

interface TileRow {
  rowKey: string;
  rowIndex: number;
  title: string;
  cells: TileCell[];
}

// ── State ─────────────────────────────────────────────────────────────────────

const initialData = ref<InitialData | null>(null);
const currentPage = ref(0);
const selectedRowIndices = ref(new Set<number>());
const showOnlySelected = ref(false);

onMounted(async () => {
  const knimeService = getKnimeService();
  const jsonDataService = new JsonDataService(knimeService);
  initialData.value = await jsonDataService.initialData();
  showOnlySelected.value =
    initialData.value?.settings.showOnlySelectedRows ?? false;
});

// ── Derived settings ──────────────────────────────────────────────────────────

const settings = computed(() => initialData.value?.settings);
const table = computed(() => initialData.value?.table);

// ── Data mapping ──────────────────────────────────────────────────────────────

/**
 * Rows arrive as string[]: [displayIndex, rowKey, col0, col1, ...].
 * displayedColumns lists the column names that correspond to col0, col1, …
 */
function resolveTileTitle(row: string[], cols: string[]): string {
  const tc = settings.value?.titleColumn;
  if (!tc) {
    return row[1];
  }
  if ("specialChoice" in tc) {
    return tc.specialChoice === "ROW_ID" ? row[1] : "";
  }
  // column choice
  const idx = cols.indexOf(tc.value);
  return idx >= 0 ? row[idx + 2] ?? "" : row[1];
}

function buildCells(row: string[], cols: string[]): TileCell[] {
  return cols.map((column, i) => ({ column, value: row[i + 2] ?? "" }));
}

const allRows = computed<TileRow[]>(() => {
  const t = table.value;
  const s = settings.value;
  if (!t || !s) {
    return [];
  }
  const cols = t.displayedColumns;
  return t.rows.map((row, i) => ({
    rowKey: row[1],
    rowIndex: t.rowIndices[i],
    title: resolveTileTitle(row, cols),
    cells: buildCells(row, cols),
  }));
});

const filteredRows = computed<TileRow[]>(() =>
  showOnlySelected.value
    ? allRows.value.filter((r) => selectedRowIndices.value.has(r.rowIndex))
    : allRows.value,
);

const pageSize = computed(() => settings.value?.pageSize ?? 10);

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value)),
);

const visibleRows = computed<TileRow[]>(() => {
  const start = currentPage.value * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

// ── Grid ───────────────────────────────────────────────────────────────────────

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${settings.value?.tilesPerRow ?? 1}, 1fr)`,
}));

// ── Selection ─────────────────────────────────────────────────────────────────

function toggleSelection(rowIndex: number) {
  const next = new Set(selectedRowIndices.value);
  if (next.has(rowIndex)) {
    next.delete(rowIndex);
  } else {
    next.add(rowIndex);
  }
  selectedRowIndices.value = next;
}

function clearSelection() {
  selectedRowIndices.value = new Set();
}

function selectAll() {
  selectedRowIndices.value = new Set(filteredRows.value.map((r) => r.rowIndex));
}

// ── Pagination ────────────────────────────────────────────────────────────────

function prevPage() {
  if (currentPage.value > 0) {
    currentPage.value--;
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++;
  }
}

function onToggleShowOnlySelected(event: Event) {
  showOnlySelected.value = (event.target as HTMLInputElement).checked;
  currentPage.value = 0;
}
</script>

<template>
  <div v-if="initialData" class="tile-view">
    <!-- View title -->
    <h1 v-if="settings?.title" class="view-title">{{ settings.title }}</h1>

    <!-- Toolbar -->
    <div
      v-if="
        settings?.showClearSelectionButton ||
        settings?.showSelectAllButton ||
        settings?.showOnlySelectedRowsConfigurable
      "
      class="toolbar"
    >
      <button
        v-if="settings?.showClearSelectionButton"
        class="btn"
        @click="clearSelection"
      >
        Clear selection
      </button>
      <button
        v-if="settings?.showSelectAllButton"
        class="btn"
        @click="selectAll"
      >
        Select all
      </button>
      <label
        v-if="settings?.showOnlySelectedRowsConfigurable"
        class="toggle-label"
      >
        <input
          type="checkbox"
          :checked="showOnlySelected"
          @change="onToggleShowOnlySelected"
        />
        <span>Show only selected rows</span>
      </label>
    </div>

    <!-- Tile grid -->
    <div class="tile-grid" :style="gridStyle">
      <Tile
        v-for="row in visibleRows"
        :key="row.rowIndex"
        :row-index="row.rowIndex"
        :cells="row.cells"
        :title="row.title"
        :selected="selectedRowIndices.has(row.rowIndex)"
        :text-alignment="settings?.textAlignment ?? 'LEFT'"
        :display-column-headers="settings?.displayColumnHeaders ?? true"
        @toggle-selection="toggleSelection(row.rowIndex)"
      />
    </div>

    <!-- Empty state -->
    <div v-if="visibleRows.length === 0" class="empty-state">
      No rows to display.
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="btn" :disabled="currentPage === 0" @click="prevPage">
        ‹ Previous
      </button>
      <span>Page {{ currentPage + 1 }} of {{ totalPages }}</span>
      <button
        class="btn"
        :disabled="currentPage >= totalPages - 1"
        @click="nextPage"
      >
        Next ›
      </button>
    </div>
  </div>

  <!-- Loading state -->
  <div v-else class="loading">Loading…</div>
</template>

<style scoped>
.tile-view {
  font-family: Roboto, sans-serif;
  padding: 16px;
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 16px;

  /* background: var(--knime-porcelain); */
  overflow: auto;
}

.view-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--knime-masala);
  margin: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.btn {
  padding: 4px 14px;
  border: 1px solid var(--knime-masala);
  border-radius: 20px;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  font-family: Roboto, sans-serif;
  color: var(--knime-masala);

  &:disabled {
    opacity: 0.4;
    cursor: default;
  }

  &:hover:not(:disabled) {
    background: var(--knime-masala);
    color: var(--knime-white);
  }
}

.toggle-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  cursor: pointer;
  color: var(--knime-masala);
  user-select: none;
}

.tile-grid {
  display: grid;
  gap: 16px;
  align-items: start;
}

.empty-state {
  text-align: center;
  padding: 32px 0;
  font-size: 14px;
  color: var(--knime-masala);
  opacity: 0.6;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
  font-size: 13px;
  color: var(--knime-masala);
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 14px;
  color: var(--knime-masala);
}
</style>
