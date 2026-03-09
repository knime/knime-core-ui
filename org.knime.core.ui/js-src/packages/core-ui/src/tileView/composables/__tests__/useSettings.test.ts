import { beforeEach, describe, expect, it } from "vitest";

import { SelectionMode } from "@/tableView/types/ViewSettings";
import {
  DEFAULT_SETTINGS,
  type TileViewSettings,
  useSettings,
} from "../useSettings";

describe("useSettings", () => {
  let comp: ReturnType<typeof useSettings>;

  beforeEach(() => {
    comp = useSettings();
  });

  it("initialises settings with DEFAULT_SETTINGS", () => {
    expect(comp.settings.value).toStrictEqual(DEFAULT_SETTINGS);
  });

  describe("updateSettings", () => {
    const makeSettings = (
      overrides: Partial<TileViewSettings> = {},
    ): TileViewSettings => ({ ...DEFAULT_SETTINGS, ...overrides });

    it("applies the new settings", () => {
      const next = makeSettings({ title: "New Title" });
      comp.updateSettings(comp.settings.value, next);
      expect(comp.settings.value.title).toBe("New Title");
    });

    it("returns needsRefetch=false when only non-refetch settings change", () => {
      const next = makeSettings({
        title: "Changed",
        tilesPerRow: 5,
        displayColumnHeaders: false,
        textAlignment: "RIGHT",
        selectionMode: SelectionMode.SHOW,
        showOnlySelectedRowsConfigurable: true,
      });
      const diff = comp.updateSettings(comp.settings.value, next);
      expect(diff.needsRefetch).toBeFalsy();
      expect(diff.needsPageReset).toBeFalsy();
      expect(diff.displayedColumnsChanged).toBeFalsy();
    });

    it("detects displayedColumns change", () => {
      const prev = makeSettings({
        displayedColumns: { selected: ["A", "B"] },
      });
      comp.settings.value = prev;
      const next = makeSettings({ displayedColumns: { selected: ["A"] } });
      const diff = comp.updateSettings(prev, next);
      expect(diff.needsRefetch).toBeTruthy();
      expect(diff.displayedColumnsChanged).toBeTruthy();
      expect(diff.needsPageReset).toBeFalsy();
    });

    it("does not flag displayedColumns changed when arrays are equal", () => {
      const prev = makeSettings({
        displayedColumns: { selected: ["A", "B"] },
      });
      comp.settings.value = prev;
      const next = makeSettings({ displayedColumns: { selected: ["A", "B"] } });
      const diff = comp.updateSettings(prev, next);
      expect(diff.displayedColumnsChanged).toBeFalsy();
    });

    it("detects titleColumn change (specialChoice)", () => {
      const prev = makeSettings({
        titleColumn: { specialChoice: "NONE" },
      });
      comp.settings.value = prev;
      const next = makeSettings({
        titleColumn: { specialChoice: "ROW_ID" },
      });
      const diff = comp.updateSettings(prev, next);
      expect(diff.needsRefetch).toBeTruthy();
      expect(diff.needsPageReset).toBeFalsy();
    });

    it("does not flag titleColumn changed when specialChoice is identical", () => {
      const prev = makeSettings({ titleColumn: { specialChoice: "NONE" } });
      comp.settings.value = prev;
      const next = makeSettings({ titleColumn: { specialChoice: "NONE" } });
      const diff = comp.updateSettings(prev, next);
      expect(diff.needsRefetch).toBeFalsy();
    });

    it("does not flag titleColumn changed when regularChoice is identical", () => {
      const prev = makeSettings({ titleColumn: { regularChoice: "colA" } });
      comp.settings.value = prev;
      const next = makeSettings({ titleColumn: { regularChoice: "colA" } });
      const diff = comp.updateSettings(prev, next);
      expect(diff.needsRefetch).toBeFalsy();
    });

    it("detects titleColumn change (regularChoice)", () => {
      const prev = makeSettings({
        titleColumn: { regularChoice: "colA" },
      });
      comp.settings.value = prev;
      const next = makeSettings({
        titleColumn: { regularChoice: "colB" },
      });
      const diff = comp.updateSettings(prev, next);
      expect(diff.needsRefetch).toBeTruthy();
    });

    it("detects colorColumn change", () => {
      const prev = makeSettings({
        colorColumn: { specialChoice: "NONE" },
      });
      comp.settings.value = prev;
      const next = makeSettings({
        colorColumn: { regularChoice: "myCol" },
      });
      const diff = comp.updateSettings(prev, next);
      expect(diff.needsRefetch).toBeTruthy();
      expect(diff.needsPageReset).toBeFalsy();
    });

    it("detects pageSize change and sets needsPageReset", () => {
      const prev = makeSettings({ pageSize: 10 });
      comp.settings.value = prev;
      const next = makeSettings({ pageSize: 20 });
      const diff = comp.updateSettings(prev, next);
      expect(diff.needsRefetch).toBeTruthy();
      expect(diff.needsPageReset).toBeTruthy();
    });

    it("detects showOnlySelectedRows change and sets needsPageReset", () => {
      const prev = makeSettings({ showOnlySelectedRows: false });
      comp.settings.value = prev;
      const next = makeSettings({ showOnlySelectedRows: true });
      const diff = comp.updateSettings(prev, next);
      expect(diff.needsRefetch).toBeTruthy();
      expect(diff.needsPageReset).toBeTruthy();
    });
  });
});
