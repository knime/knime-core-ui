import {
  AbstractMessageReader,
  AbstractMessageWriter,
  type DataCallback,
  Disposable,
  Message,
  MessageReader,
  MessageWriter,
} from "vscode-languageserver-protocol";

import { getScriptingService } from "../scripting-service";

/**
 * This class is used to read messages from the language server. The callback
 * is registered by the connection and called each time a message from the
 * language server is received.
 */
export class KnimeMessageReader
  extends AbstractMessageReader
  implements MessageReader
{
  protected messageCache: string[] = [];
  protected callback: DataCallback | null;

  constructor() {
    super();
    getScriptingService().registerEventHandler("language-server", (message) => {
      this.readMessage(message);
    });
    this.callback = null;
  }

  listen(callback: DataCallback): Disposable {
    if (this.callback === null) {
      this.callback = callback;
      this.messageCache.forEach((message) => {
        this.readMessage(message);
      });
      this.messageCache = [];

      return {
        dispose: () => {
          if (this.callback === callback) {
            this.callback = null;
          }
        },
      };
    } else {
      throw Error(
        "Tried to register more than one language server callback. " +
          "This is an implementation error.",
      );
    }
  }

  readMessage(message: string) {
    if (this.callback) {
      const data = JSON.parse(message);
      this.callback(data);
    } else {
      this.messageCache.push(message);
    }
  }
}

/**
 * This class is used to write messages to the language server.
 */
export class KnimeMessageWriter
  extends AbstractMessageWriter
  implements MessageWriter
{
  // eslint-disable-next-line class-methods-use-this -- required for the interface
  async write(msg: Message): Promise<void> {
    await getScriptingService().sendToService("sendLanguageServerMessage", [
      JSON.stringify(msg),
    ]);
  }

  // eslint-disable-next-line class-methods-use-this -- required for the interface
  end(): void {
    // Required by the interface but nothing to do
  }
}
