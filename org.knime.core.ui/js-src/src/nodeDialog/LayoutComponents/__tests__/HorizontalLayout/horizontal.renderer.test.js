import { describe, expect, it } from 'vitest';
import { vanillaRenderers } from '@jsonforms/vue-vanilla';
import { fallbackRenderers, defaultRenderers } from '@/components/renderers';
import { determineRenderer } from '@@/test-setup/utils/rendererTestUtils';

const renderers = [...vanillaRenderers, ...fallbackRenderers, ...defaultRenderers];

describe('HorizontalLayout', () => {
    const schema = {};

    it('Empty HorizontalLayout', () => {
        const uiSchema = {
            type: 'HorizontalLayout',
            scope: '#/properties/test'
        };

        expect(determineRenderer(uiSchema, schema, renderers)).toBe('HorizontalLayout');
    });
});