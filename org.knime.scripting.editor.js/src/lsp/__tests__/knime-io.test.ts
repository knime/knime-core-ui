import { afterEach, describe, expect, it, vi } from "vitest";

import { getScriptingService } from "@/init";
import { KnimeMessageReader, KnimeMessageWriter } from "../knime-io";

vi.mock("@/scripting-service");

describe("knime-io", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  describe("reader", () => {
    const languageServerEventHandler = () =>
      vi.mocked(getScriptingService().registerEventHandler).mock.calls[0][1];

    it("calls callback", () => {
      const reader = new KnimeMessageReader();
      const callback = vi.fn();
      reader.listen(callback);

      const message0 = { foo: "myMessage" };
      languageServerEventHandler()(JSON.stringify(message0));
      expect(callback).toHaveBeenCalledWith(message0);

      const message1 = { bar: "myMessage 2" };
      languageServerEventHandler()(JSON.stringify(message1));
      expect(callback).toHaveBeenCalledWith(message1);
    });

    it("collects message before starting listening", () => {
      const reader = new KnimeMessageReader();

      const message0 = { foo: "myMessage" };
      languageServerEventHandler()(JSON.stringify(message0));

      const message1 = { bar: "myMessage 2" };
      languageServerEventHandler()(JSON.stringify(message1));

      const callback = vi.fn();
      reader.listen(callback);
      expect(callback).toHaveBeenCalledWith(message0);
      expect(callback).toHaveBeenCalledWith(message1);
    });

    it("disposes callback", () => {
      const reader = new KnimeMessageReader();
      const callback = vi.fn();
      const { dispose } = reader.listen(callback);

      const message0 = { foo: "myMessage" };
      languageServerEventHandler()(JSON.stringify(message0));
      expect(callback).toHaveBeenCalledWith(message0);

      dispose();

      const message1 = { bar: "myMessage 2" };
      languageServerEventHandler()(JSON.stringify(message1));
      expect(callback).not.toHaveBeenCalledWith(message1);
    });
  });

  describe("writer", () => {
    it("sends messages to the scripting service", () => {
      const writer = new KnimeMessageWriter();
      const message = {
        jsonrpc: "2.0",
        method: "foo",
        params: "bar",
      };
      writer.write(message);
      expect(getScriptingService().sendToService).toHaveBeenCalledWith(
        "sendLanguageServerMessage",
        [JSON.stringify(message)],
      );
    });
  });
});
