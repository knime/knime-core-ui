<script lang="ts">
import { FlowSettings } from "@/nodeDialog/api/types";
import {
  JsonDataService,
  type UIExtensionService,
  type UIExtensionEvents,
  ReportingService,
} from "@knime/ui-extension-service";

export default {
  inject: ["getKnimeService"],
  data() {
    return {
      richTextContent: "",
      flowVariablesMap: null as null | Record<string, FlowSettings>,
      jsonDataService: null as null | JsonDataService,
    };
  },
  computed: {
    knimeService() {
      return (this.getKnimeService as () => UIExtensionService)();
    },
  },
  async mounted() {
    this.jsonDataService = new JsonDataService(this.knimeService);
    this.jsonDataService!.addOnDataChangeCallback(
      this.onViewSettingsChange.bind(this),
    );
    const data = await this.jsonDataService.initialData();
    const { content, flowVariablesMap } = data;
    this.flowVariablesMap = flowVariablesMap;
    this.richTextContent = this.replaceFlowVariablesInContent(content);
    const reportingService = new ReportingService(this.knimeService);
    const isReport = reportingService.isReportingActive();
    await this.$nextTick();
    if (isReport) {
      reportingService.setRenderCompleted();
    }
  },
  methods: {
    onViewSettingsChange(event: UIExtensionEvents.Event<any>) {
      // TODO: Can be removed once we have frontend sanitization
      if (
        event.data.flowVariableSettings["view.richTextContent"]
          ?.controllingFlowVariableAvailable
      ) {
        return;
      }
      this.richTextContent = this.replaceFlowVariablesInContent(
        event.data.data.view.richTextContent,
      );
    },
    replaceFlowVariablesInContent(newRichTextContent: string) {
      if (this.flowVariablesMap === null) {
        return newRichTextContent;
      }
      Object.entries(this.flowVariablesMap).forEach(([key, value]) => {
        newRichTextContent = newRichTextContent.replaceAll(
          `$$["${key}"]`,
          value,
        );
        newRichTextContent = newRichTextContent.replaceAll(
          `$$[&#34;${key}&#34;]`,
          value,
        );
      });
      return newRichTextContent;
    },
  },
};
</script>

<template>
  <div class="text-view-container" v-html="richTextContent" />
</template>

<style lang="postcss" scoped>
@import url("webapps-common/ui/components/forms/RichTextEditor/styles.css");

.text-view-container:deep() {
  @mixin rich-text-editor-styles;

  /* needed to display multiple line breaks https://github.com/ueberdosis/tiptap/issues/412 */
  & p:empty::after {
    content: "\00A0";
  }

  & p.small-text {
    font-size: 7px;
  }
}
</style>
