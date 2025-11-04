#!/bin/bash

# Test script for @knime/core-ui and scripting-editor package.json scripts
# Only reports success/failure, no stdout/stderr output

echo "Testing package.json scripts..."
echo "================================"

# Function to test a script
test_script() {
    local package=$1
    local script=$2
    local command="pnpm run --filter $package $script"
    
    echo -n "Testing $package:$script... "
    
    if $command > /dev/null 2>&1; then
        echo "✅ PASS"
    else
        echo "❌ FAIL"
        echo "  Debug command: $command"
    fi
}

# @knime/core-ui scripts
echo "@knime/core-ui scripts:"
echo "---------------------"
test_script "@knime/core-ui" "type-check"
test_script "@knime/core-ui" "format:check"
test_script "@knime/core-ui" "cleanDist"
test_script "@knime/core-ui" "lint:js"
test_script "@knime/core-ui" "lint:css"
test_script "@knime/core-ui" "audit"
test_script "@knime/core-ui" "coverage:unit"
test_script "@knime/core-ui" "coverage:integration"
test_script "@knime/core-ui" "sbom"
test_script "@knime/core-ui" "build"

echo ""
echo "@knime/scripting-editor scripts:"
echo "--------------------------------"
test_script "@knime/scripting-editor" "type-check"
test_script "@knime/scripting-editor" "format:check"
test_script "@knime/scripting-editor" "lint:js"
test_script "@knime/scripting-editor" "lint:css"
test_script "@knime/scripting-editor" "audit"
test_script "@knime/scripting-editor" "coverage"
test_script "@knime/scripting-editor" "build"

echo ""
echo "Testing complete!"