#!/bin/bash
# Build script for all components

set -e

echo "Building UML MCP Service..."
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Build Server
echo -e "${YELLOW}Building Server...${NC}"
cd server
mvn clean package -DskipTests
echo -e "${GREEN}Server build complete!${NC}"
echo ""

# Build Java Client
echo -e "${YELLOW}Building Java Client...${NC}"
cd ../clients/java
mvn clean package -DskipTests
echo -e "${GREEN}Java Client build complete!${NC}"
echo ""

# Check Python dependencies
echo -e "${YELLOW}Checking Python Client...${NC}"
cd ../python
if command -v python3 &> /dev/null; then
    if [ -f "requirements.txt" ]; then
        echo "Python client ready. Install dependencies with:"
        echo "  pip install -r requirements.txt"
    fi
    echo -e "${GREEN}Python Client ready!${NC}"
else
    echo "Python3 not found. Skipping Python client check."
fi
echo ""

cd ../../..

echo -e "${GREEN}Build complete!${NC}"
echo ""
echo "Built artifacts:"
echo "  Server: server/target/uml-mcp-server-1.0.0.jar"
echo "  Java Client: clients/java/target/uml-mcp-java-client-1.0.0.jar"
echo "  Python Client: clients/python/uml_mcp_client.py"
echo ""
echo "To run the server:"
echo "  java -jar server/target/uml-mcp-server-1.0.0.jar"
