import { afterEach, describe, expect, it, vi } from "vitest";

import { KnimeMessageReader, KnimeMessageWriter } from "../knime-io";

vi.mock("@s/scripting-service");

describe("knime-io", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  describe("reader", () => {
    const newMessageReader = () => {
      const registerEventHandler = vi.fn();
      const reader = new KnimeMessageReader(registerEventHandler);
      const eventHandler = registerEventHandler.mock.calls[0][0];
      return { reader, eventHandler };
    };

    it("calls callback", () => {
      const { reader, eventHandler } = newMessageReader();
      const callback = vi.fn();
      reader.listen(callback);

      const message0 = { foo: "myMessage" };
      eventHandler(JSON.stringify(message0));
      expect(callback).toHaveBeenCalledWith(message0);

      const message1 = { bar: "myMessage 2" };
      eventHandler(JSON.stringify(message1));
      expect(callback).toHaveBeenCalledWith(message1);
    });

    it("collects message before starting listening", () => {
      const { reader, eventHandler } = newMessageReader();

      const message0 = { foo: "myMessage" };
      eventHandler(JSON.stringify(message0));

      const message1 = { bar: "myMessage 2" };
      eventHandler(JSON.stringify(message1));

      const callback = vi.fn();
      reader.listen(callback);
      expect(callback).toHaveBeenCalledWith(message0);
      expect(callback).toHaveBeenCalledWith(message1);
    });

    it("disposes callback", () => {
      const { reader, eventHandler } = newMessageReader();
      const callback = vi.fn();
      const { dispose } = reader.listen(callback);

      const message0 = { foo: "myMessage" };
      eventHandler(JSON.stringify(message0));
      expect(callback).toHaveBeenCalledWith(message0);

      dispose();

      const message1 = { bar: "myMessage 2" };
      eventHandler(JSON.stringify(message1));
      expect(callback).not.toHaveBeenCalledWith(message1);
    });
  });

  describe("writer", () => {
    it("sends messages to the scripting service", () => {
      const sendMessage = vi.fn();
      const writer = new KnimeMessageWriter(sendMessage);
      const message = {
        jsonrpc: "2.0",
        method: "foo",
        params: "bar",
      };
      writer.write(message);
      expect(sendMessage).toHaveBeenCalledWith(JSON.stringify(message));
    });
  });
});
