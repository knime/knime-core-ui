import { vi } from "vitest";

const Terminal = vi.fn();
Terminal.prototype.open = vi.fn();
Terminal.prototype.write = vi.fn();
Terminal.prototype.loadAddon = vi.fn();
Terminal.prototype.reset = vi.fn();

export { Terminal };
