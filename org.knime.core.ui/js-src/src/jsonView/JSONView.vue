<script>
import { JsonDataService } from "@knime/ui-extension-service";
import "../common/main.css";

export default {
  inject: ["getKnimeService"],
  data() {
    return {
      jsonDataService: null,
      json: null,
    };
  },
  computed: {
    knimeService() {
      return this.getKnimeService();
    },
  },
  async mounted() {
    this.jsonDataService = new JsonDataService(this.knimeService);

    const { json } = await this.jsonDataService.initialData();
    this.json = json;
  },
};
</script>

<template>
  <div>
    {{ json }}
  </div>
</template>
