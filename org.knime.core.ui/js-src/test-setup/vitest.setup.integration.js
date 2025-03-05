import { vi } from "vitest";
import * as Vue from "vue";
import consola from "consola";

window.consola = consola;
window.Vue = Vue;

vi.mock("@knime/ui-extension-service");
vi.mock("@knime/ui-extension-service/internal");
