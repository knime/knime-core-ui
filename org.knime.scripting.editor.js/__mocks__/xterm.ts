import { vi } from "vitest";

const Terminal = vi.fn();
Terminal.prototype.open = vi.fn();
Terminal.prototype.write = vi.fn();
Terminal.prototype.writeln = vi.fn();
Terminal.prototype.loadAddon = vi.fn();
Terminal.prototype.reset = vi.fn();
Terminal.prototype.onLineFeed = vi.fn();
Terminal.prototype.unicode = vi.fn();
Terminal.prototype.attachCustomKeyEventHandler = vi.fn();

export { Terminal };
