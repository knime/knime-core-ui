<script>
import { JsonDataService } from '@knime/ui-extension-service';

export default {
    inject: ['getKnimeService'],
    data() {
        return {
            HTMLContent: ''
        };
    },
    
    computed: {
        knimeService() {
            return this.getKnimeService();
        }
    },
    async mounted() {
        this.jsonDataService = new JsonDataService(this.knimeService);
        this.jsonDataService.addOnDataChangeCallback(this.onViewSettingsChange.bind(this));
        const initialData = await this.jsonDataService.initialData();
        this.HTMLContent = initialData.settings.HTMLContent;
    },
    methods: {
        
        onViewSettingsChange(event) {
            this.HTMLContent = event.data.data.view.HTMLContent;
        }
    }
};
</script>

<template>
  <div
    class="text-view-container"
    v-html="HTMLContent"
  />
</template>

<style lang="postcss" scoped>
.text-view-container {
    font-size: 13px;
    font-weight: 300;
    & :deep(h1) {
            font-size: 36px;
            margin: 32px 0 16px;
            font-weight: bold;
        }

        & :deep(h2) {
            font-size: 30px;
            margin: 24px 0 12px;
            font-weight: bold;
        }

        & :deep(h3) {
            font-size: 26px;
            margin: 20px 0 10px;
            font-weight: bold;
        }

        & :deep(h4) {
            font-size: 22px;
            margin: 16px 0 8px;
            font-weight: bold;
        }

        & :deep(h5) {
            font-size: 18px;
            margin: 12px 0 6px;
            font-weight: bold;
        }

        & :deep(h6) {
            font-size: 16px;
            margin: 10px 0 5px;
            font-weight: bold;
        }

}
</style>
