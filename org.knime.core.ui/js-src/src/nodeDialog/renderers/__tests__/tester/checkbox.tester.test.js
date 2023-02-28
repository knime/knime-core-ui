import { describe, expect, it } from 'vitest';
import { dialogInitialData } from '@@/test-setup/mocks/dialogInitialData';
import { inputFormats } from '@/nodeDialog/constants/inputFormats';
import { checkboxTester } from '../../checkboxRenderer';

describe('checkboxTester', () => {
    it('applies for boolean control with checkbox format', () => {
        expect(
            checkboxTester({
                type: 'Control',
                scope: '#/properties/view/properties/showTooltip',
                options: {
                    format: inputFormats.checkbox
                }
            },
            dialogInitialData.schema)
        ).toEqual(true);
    });

    it('does not apply if checkbox format is not set', () => {
        expect(
            checkboxTester({
                type: 'Control',
                scope: '#/properties/view/properties/showTooltip'
            },
            dialogInitialData.schema)
        ).toEqual(false);
    });

    it('does not apply if not a Control', () => {
        expect(
            checkboxTester({
                type: 'Section',
                scope: '#/properties/view/properties/showTooltip',
                options: {
                    format: inputFormats.checkbox
                }
            },
            dialogInitialData.schema)
        ).toEqual(false);
    });
});
