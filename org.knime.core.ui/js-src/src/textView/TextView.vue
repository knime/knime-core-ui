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
  <div v-html="HTMLContent" />
</template>

<style lang="postcss" scoped>

</style>
