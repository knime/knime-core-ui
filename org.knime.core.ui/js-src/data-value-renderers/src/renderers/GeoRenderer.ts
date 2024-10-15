/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable class-methods-use-this */
import {
  JsonDataService,
  AlertingService,
  SharedDataService,
} from "@knime/ui-extension-service";
import "../styles.css";
import "@knime/styles/css/fonts.css";

export class GeoRenderer {
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
  
  const geojson = Terraformer.wktToGeoJSON(initialData);
  if (geojson.type === "Point") {
    this.container = L.map("map").setView(geojson.coordinates, 13);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(this.container);
    L.marker(geojson.coordinates).addTo(this.container);
  } else if (geojson.type === "Polygon") {
    this.container = L.map("map").setView([0,0], 13);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(this.container);
    const polygone = L.polygon(geojson.coordinates[0]).addTo(this.container);
    this.container.fitBounds(polygone.getBounds());
  } else {
    this.container = L.map("map").setView([0,0], 13);
    const leafletGeo = L.geoJSON(geojson).addTo(this.container);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(this.container);
    this.container.fitBounds(leafletGeo.getBounds());
  }
}

private async waitForFontsToBeLoaded() {
  await Promise.all([
    document.fonts.load("400 1em Roboto"),
    document.fonts.load("700 1em Roboto"),
  ]);
}
}