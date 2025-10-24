# UML MCP Python Client

Python client for the UML MCP Server.

## Installation

```bash
pip install -r requirements.txt
```

## Usage

### Command Line

```bash
# Generate a diagram using the default example
python uml_mcp_client.py generate --server http://localhost:8080

# Generate from a file
python uml_mcp_client.py generate --uml diagram.puml --format png --output result.png

# Generate from inline UML
python uml_mcp_client.py generate --uml "@startuml\nAlice -> Bob\n@enduml" --format svg

# Use built-in examples
python uml_mcp_client.py generate --example sequence --format png
python uml_mcp_client.py generate --example class --format svg

# Validate UML
python uml_mcp_client.py validate --uml diagram.puml

# Get server capabilities
python uml_mcp_client.py capabilities --server http://localhost:8080
```

### As a Library

```python
from uml_mcp_client import UMLMCPClient

# Create client
client = UMLMCPClient("http://localhost:8080")

# Generate diagram
uml_source = """
@startuml
Alice -> Bob: Hello
Bob --> Alice: Hi!
@enduml
"""

response = client.generate_diagram(uml_source, "png", "sequence")

if not response.has_error():
    client.save_diagram(response, "output.png")
else:
    print(f"Error: {response.get_error_message()}")
```

## Command Line Options

- `command`: The command to execute (generate, validate, capabilities)
- `--server`: MCP server URL (default: http://localhost:8080)
- `--uml`: PlantUML source code or path to .puml file
- `--format`: Output format - png, svg, txt (default: png)
- `--output`: Output file path
- `--diagram-type`: Diagram type (default: sequence)
- `--example`: Use a built-in example (sequence, class)
