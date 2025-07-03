#!/bin/bash
set -e
npm ci
npm run type-check
npm run ci:lint-format
npm run coverage
npm run audit
npm run build

