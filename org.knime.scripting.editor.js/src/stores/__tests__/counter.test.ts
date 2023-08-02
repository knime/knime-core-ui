import { describe, it, expect, beforeEach } from "vitest";
import { setActivePinia, createPinia } from "pinia";
import { useCounterStore } from "../counter";

describe("useCounterStore", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it("should initialize count to 0", () => {
    const store = useCounterStore();
    expect(store.count).toBe(0);
  });
});
