/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable class-methods-use-this */
import {
  JsonDataService,
  AlertingService,
  SharedDataService,
} from "@knime/ui-extension-service";
import "../styles.css";
import "@knime/styles/css/fonts.css";

export class ImageRenderer {
  jsonDataService!: JsonDataService;
  sharedDataService!: SharedDataService;
  container: HTMLElement;

constructor(
  container: HTMLElement,
) {
  if (!container) {
    const message = "View container element not found";
    AlertingService.getInstance().then((service) => {
      service.sendAlert({ message });
    });
    throw new Error(message);
  }
  container.style.overflow = "hidden";
  this.container = container;
}

async init() {
  this.jsonDataService = await JsonDataService.getInstance();
  this.sharedDataService = await SharedDataService.getInstance();
  const initialData = (await this.jsonDataService.initialData());

  await this.waitForFontsToBeLoaded();
  this.container.innerHTML = `<img src="data:image/png;base64, ${initialData}">`;

}

private async waitForFontsToBeLoaded() {
  await Promise.all([
    document.fonts.load("400 1em Roboto"),
    document.fonts.load("700 1em Roboto"),
  ]);
}
}