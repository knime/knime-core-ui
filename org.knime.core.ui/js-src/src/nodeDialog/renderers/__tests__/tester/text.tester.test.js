import { describe, expect, it } from 'vitest';
import { textTester } from '../../textRenderer';
import { dialogInitialData } from '@@/test-setup/mocks/dialogInitialData';

describe('textTester', () => {
    it('applies on string control', () => {
        expect(
            textTester(
                {
                    type: 'Control',
                    scope: '#/properties/view/properties/xAxisLabel'
                },
                dialogInitialData.schema
            )
        ).toEqual(true);
    });

    it('does not apply if not a control', () => {
        expect(
            textTester({
                type: 'Section',
                scope: '#/properties/view/properties/xAxisLabel'
            },
            dialogInitialData.schema)
        ).toEqual(false);
    });

    it('does not apply if not a string', () => {
        expect(
            textTester({
                type: 'Control',
                scope: '#/properties/view/properties/xAxisColumn',
                options: {
                    format: 'columnSelection'
                }
            },
            dialogInitialData.schema)
        ).toEqual(false);
    });
});
