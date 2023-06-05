import { describe, expect, it } from 'vitest';
import { credentialsTester } from '../../credentialsRenderer';
import { inputFormats } from '@/nodeDialog/constants/inputFormats';
import { dialogInitialData } from '@@/test-setup/mocks/dialogInitialData';

describe('credentialsTester', () => {
    it('applies control with credentials format', () => {
        expect(
            credentialsTester({
                type: 'Control',
                scope: '#/properties/view/properties/credentials',
                options: {
                    format: inputFormats.credentials
                }
            },
            dialogInitialData.schema)
        ).toBe(true);
    });

    it('does not apply without credentials format', () => {
        expect(
            credentialsTester({
                type: 'Control',
                scope: '#/properties/view/properties/credentials'
            },
            dialogInitialData.schema)
        ).toBe(false);
    });

    it('does not apply if not a control', () => {
        expect(
            credentialsTester({
                type: 'Section',
                scope: '#/properties/view/properties/credentials',
                options: {
                    format: inputFormats.credentials
                }
            },
            dialogInitialData.schema)
        ).toBe(false);
    });
});
