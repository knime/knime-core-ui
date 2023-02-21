// This is a workaround for https://github.com/eslint/eslint/issues/3458
require('@rushstack/eslint-patch/modern-module-resolution');

module.exports = {
    extends: ['@knime/eslint-config/vue3-typescript', '@knime/eslint-config/vitest'],
    env: {
        node: true,
        browser: true
    },
    settings: {
        'import/resolver': {
            alias: {
                map: [
                    ['@', 'src/.'],
                    ['@@', '.']
                ]
            }
        }
    },
    rules: {
        'import/extensions': [
            'error',
            { vue: 'always', json: 'always', mjs: 'always', svg: 'always', config: 'ignorePackages' }
        ]
    },
    overrides: [
        {
            extends: ['@knime/eslint-config/vitest'],
            files: ['test/**/*.test.*'],
            rules: {
                'no-magic-numbers': 'off'
            }
        }
    ]
};
