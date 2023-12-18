<script setup lang="ts">
import { inject } from "vue";

import {
  ReportingService,
  type UIExtensionService,
} from "@knime/ui-extension-service";

import TableViewInteractive from "./TableViewInteractive.vue";
import TableViewReport from "./TableViewReport.vue";

const knimeService = inject<() => UIExtensionService>("getKnimeService")!();
const reportingService = new ReportingService(knimeService);
const isReport = reportingService.isReportingActive();
const onRendered = () => reportingService.setRenderCompleted();
</script>

<template>
  <TableViewReport v-if="isReport" @rendered="onRendered" />
  <TableViewInteractive v-else />
</template>
