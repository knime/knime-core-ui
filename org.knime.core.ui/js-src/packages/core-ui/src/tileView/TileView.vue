<script setup lang="ts">
import "../common/main.css";

import { ReportingService } from "@knime/ui-extension-service";

import getKnimeService from "@/tableView/utils/getKnimeService";

import TileViewInteractive from "./TileViewInteractive.vue";
import TileViewReport from "./TileViewReport.vue";

const knimeService = getKnimeService();
const reportingService = new ReportingService(knimeService);
const isReport = reportingService.isReportingActive();
const onRendered = () => reportingService.setRenderCompleted();
</script>

<template>
  <TileViewReport v-if="isReport" @rendered="onRendered" />
  <TileViewInteractive v-else />
</template>
