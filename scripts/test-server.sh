#!/bin/bash
# Test script for UML MCP Server

set -e

SERVER_URL="${1:-http://localhost:8080}"
TEMP_DIR=$(mktemp -d)

echo "Testing UML MCP Server at $SERVER_URL"
echo "Temporary directory: $TEMP_DIR"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to test endpoint
test_endpoint() {
    local test_name="$1"
    local endpoint="$2"
    local data="$3"
    local expected_status="${4:-200}"

    echo -n "Testing: $test_name... "

    response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$data" \
        "$SERVER_URL$endpoint")

    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}PASSED${NC} (HTTP $status_code)"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "${RED}FAILED${NC} (Expected HTTP $expected_status, got $status_code)"
        echo "Response: $body"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

echo "=== Health Check ==="
echo -n "Testing: Health endpoint... "
health_response=$(curl -s -w "\n%{http_code}" "$SERVER_URL/health")
health_status=$(echo "$health_response" | tail -n1)

if [ "$health_status" = "200" ]; then
    echo -e "${GREEN}PASSED${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}FAILED${NC}"
    echo "Server appears to be down or unreachable"
    exit 1
fi
echo ""

echo "=== Generate Diagram Tests ==="

# Test 1: Generate PNG sequence diagram
test_endpoint "Generate PNG Sequence Diagram" \
    "/mcp/generate" \
    '{
        "jsonrpc": "2.0",
        "id": "test-1",
        "method": "generate",
        "params": {
            "uml": "@startuml\nAlice -> Bob: Hello\nBob --> Alice: Hi!\n@enduml",
            "format": "png",
            "diagramType": "sequence"
        }
    }'

# Test 2: Generate SVG class diagram
test_endpoint "Generate SVG Class Diagram" \
    "/mcp/generate" \
    '{
        "jsonrpc": "2.0",
        "id": "test-2",
        "method": "generate",
        "params": {
            "uml": "@startuml\nclass User {\n  +login()\n}\n@enduml",
            "format": "svg",
            "diagramType": "class"
        }
    }'

# Test 3: Generate TXT activity diagram
test_endpoint "Generate TXT Activity Diagram" \
    "/mcp/generate" \
    '{
        "jsonrpc": "2.0",
        "id": "test-3",
        "method": "generate",
        "params": {
            "uml": "@startuml\n:Start;\n:Process;\n:End;\n@enduml",
            "format": "txt",
            "diagramType": "activity"
        }
    }'

# Test 4: UML without tags (should auto-wrap)
test_endpoint "Generate Diagram Without Tags" \
    "/mcp/generate" \
    '{
        "jsonrpc": "2.0",
        "id": "test-4",
        "method": "generate",
        "params": {
            "uml": "Alice -> Bob: Test",
            "format": "png"
        }
    }'

echo ""
echo "=== Validation Tests ==="

# Test 5: Validate valid UML
test_endpoint "Validate Valid UML" \
    "/mcp/validate" \
    '{
        "jsonrpc": "2.0",
        "id": "test-5",
        "method": "validate",
        "params": {
            "uml": "@startuml\nAlice -> Bob\n@enduml"
        }
    }'

echo ""
echo "=== Capabilities Test ==="

# Test 6: Get capabilities
test_endpoint "Get Capabilities" \
    "/mcp/capabilities" \
    '{
        "jsonrpc": "2.0",
        "id": "test-6",
        "method": "capabilities"
    }'

echo ""
echo "=== Error Handling Tests ==="

# Test 7: Missing UML parameter
test_endpoint "Error: Missing UML Parameter" \
    "/mcp/generate" \
    '{
        "jsonrpc": "2.0",
        "id": "test-7",
        "method": "generate",
        "params": {
            "format": "png"
        }
    }' \
    "400"

# Test 8: Empty UML
test_endpoint "Error: Empty UML" \
    "/mcp/generate" \
    '{
        "jsonrpc": "2.0",
        "id": "test-8",
        "method": "generate",
        "params": {
            "uml": "",
            "format": "png"
        }
    }' \
    "400"

# Test 9: Invalid JSON
echo -n "Testing: Error: Invalid JSON... "
invalid_response=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d '{invalid json}' \
    "$SERVER_URL/mcp/generate")
invalid_status=$(echo "$invalid_response" | tail -n1)

if [ "$invalid_status" = "400" ]; then
    echo -e "${GREEN}PASSED${NC} (HTTP $invalid_status)"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}FAILED${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

echo ""
echo "=== Summary ==="
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo "Total Tests: $((TESTS_PASSED + TESTS_FAILED))"

# Cleanup
rm -rf "$TEMP_DIR"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "\n${RED}Some tests failed!${NC}"
    exit 1
fi
