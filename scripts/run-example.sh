#!/bin/bash
# Run example diagram generation

SERVER_URL="${1:-http://localhost:8080}"
OUTPUT_DIR="${2:-./output}"

echo "UML MCP Example Runner"
echo "Server: $SERVER_URL"
echo "Output Directory: $OUTPUT_DIR"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Check if server is running
echo "Checking server health..."
if ! curl -s "$SERVER_URL/health" > /dev/null; then
    echo "Error: Server is not reachable at $SERVER_URL"
    exit 1
fi
echo "Server is running!"
echo ""

# Generate examples
echo "Generating example diagrams..."

# Example 1: Sequence Diagram
echo -n "1. Sequence Diagram (PNG)... "
curl -s -X POST "$SERVER_URL/mcp/generate" \
    -H "Content-Type: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "id": "ex1",
        "method": "generate",
        "params": {
            "uml": "@startuml\nAlice -> Bob: Authentication Request\nBob --> Alice: Authentication Response\n@enduml",
            "format": "png"
        }
    }' | jq -r '.result.diagram' | base64 -d > "$OUTPUT_DIR/sequence-diagram.png"
echo "Done!"

# Example 2: Class Diagram
echo -n "2. Class Diagram (SVG)... "
curl -s -X POST "$SERVER_URL/mcp/generate" \
    -H "Content-Type: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "id": "ex2",
        "method": "generate",
        "params": {
            "uml": "@startuml\nclass User {\n  -username: String\n  -password: String\n  +login()\n  +logout()\n}\nclass Admin {\n  +manageUsers()\n}\nUser <|-- Admin\n@enduml",
            "format": "svg"
        }
    }' | jq -r '.result.diagram' | base64 -d > "$OUTPUT_DIR/class-diagram.svg"
echo "Done!"

# Example 3: Activity Diagram
echo -n "3. Activity Diagram (TXT)... "
curl -s -X POST "$SERVER_URL/mcp/generate" \
    -H "Content-Type: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "id": "ex3",
        "method": "generate",
        "params": {
            "uml": "@startuml\nstart\n:Initialize;\nif (Check condition?) then (yes)\n  :Process A;\nelse (no)\n  :Process B;\nendif\nstop\n@enduml",
            "format": "txt"
        }
    }' | jq -r '.result.diagramText' > "$OUTPUT_DIR/activity-diagram.txt"
echo "Done!"

# Example 4: Use Case Diagram
echo -n "4. Use Case Diagram (PNG)... "
curl -s -X POST "$SERVER_URL/mcp/generate" \
    -H "Content-Type: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "id": "ex4",
        "method": "generate",
        "params": {
            "uml": "@startuml\nactor User\nUser -- (Login)\nUser -- (Browse Products)\nUser -- (Place Order)\n@enduml",
            "format": "png"
        }
    }' | jq -r '.result.diagram' | base64 -d > "$OUTPUT_DIR/usecase-diagram.png"
echo "Done!"

echo ""
echo "Examples generated successfully!"
echo "Output files:"
ls -lh "$OUTPUT_DIR"
