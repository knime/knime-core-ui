import { vi } from "vitest";
import * as Vue from "vue";
import consola from "consola";

window.Vue = Vue;
window.global.consola = consola;

vi.mock("@knime/ui-extension-service");
vi.mock("@knime/ui-extension-service/internal");
