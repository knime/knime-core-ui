import { expect, it } from 'vitest';
import { vanillaRenderers } from '@jsonforms/vue-vanilla';
import { fallbackRenderers, defaultRenderers } from '@/components/renderers';
import { dialogInitialData, expectedRenderers } from '@@/test-setup/mocks/dialogInitialData';
import { determineRenderer } from '@@/test-setup/utils/rendererTestUtils';

const renderers = [...vanillaRenderers, ...fallbackRenderers, ...defaultRenderers];

const getElementsToTest = (elements) => elements.map(element => {
    if (element.elements) {
        return getElementsToTest(element.elements);
    }
    return element.scope && (!element.rule || (element.rule && element.rule.effect !== 'DISABLE')) && element;
}).flat().filter(Boolean);

it('renders test dialog elements', () => {
    const elementsToTest = getElementsToTest(dialogInitialData.ui_schema.elements);

    elementsToTest.forEach(element => {
        const expectedRenderer = expectedRenderers.find(r => r.scope === element.scope);
        expect(determineRenderer(element, dialogInitialData.schema, renderers)).toBe(expectedRenderer.component);
    });
});
