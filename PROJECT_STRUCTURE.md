# UML MCP Service - Project Structure

## Overview

This document describes the complete structure of the UML MCP Service project.

## Directory Structure

```
uml-mcp/
├── server/                           # MCP Server implementation
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/mcp/uml/
│   │       │       ├── MCPServer.java           # Main server class
│   │       │       ├── PlantUMLService.java     # PlantUML integration
│   │       │       ├── handlers/
│   │       │       │   └── DiagramHandler.java  # Request handlers
│   │       │       ├── models/
│   │       │       │   ├── MCPRequest.java      # Request model
│   │       │       │   └── MCPResponse.java     # Response model
│   │       │       └── utils/                   # Utility classes
│   │       └── resources/
│   │           └── application.properties       # Server configuration
│   ├── pom.xml                       # Maven configuration
│   ├── Dockerfile                    # Docker image definition
│   └── docker-compose.yml           # Docker Compose configuration
│
├── clients/                          # Client implementations
│   ├── java/                        # Java client
│   │   ├── src/
│   │   │   └── main/
│   │   │       └── java/
│   │   │           └── com/mcp/client/
│   │   │               └── UMLMCPClient.java    # Java client implementation
│   │   └── pom.xml                  # Maven configuration
│   │
│   └── python/                      # Python client
│       ├── uml_mcp_client.py        # Python client implementation
│       ├── requirements.txt          # Python dependencies
│       └── README.md                # Python client documentation
│
├── docs/                            # Documentation
│   ├── API.md                       # Complete API reference
│   ├── QUICKSTART.md               # Quick start guide
│   └── DEPLOYMENT.md               # Deployment guide
│
├── examples/                        # Example UML diagrams
│   ├── sequence-diagram.puml       # Sequence diagram example
│   ├── class-diagram.puml          # Class diagram example
│   ├── usecase-diagram.puml        # Use case diagram example
│   └── activity-diagram.puml       # Activity diagram example
│
├── scripts/                         # Utility scripts
│   ├── build-all.sh                # Build all components
│   ├── test-server.sh              # Server test suite
│   └── run-example.sh              # Run example diagrams
│
├── README.md                        # Main project documentation
├── LICENSE                          # MIT License
├── .gitignore                       # Git ignore rules
└── PROJECT_STRUCTURE.md            # This file
```

## Component Descriptions

### Server

**Location:** `server/`

The server component is a Java-based MCP service that:
- Accepts JSON-RPC 2.0 requests
- Generates UML diagrams using PlantUML
- Returns diagrams in PNG, SVG, or TXT format
- Provides REST and MCP endpoints

**Key Files:**
- `MCPServer.java`: Main entry point, configures Javalin web server
- `PlantUMLService.java`: Wrapper around PlantUML library
- `DiagramHandler.java`: Handles HTTP requests and responses
- `MCPRequest.java`, `MCPResponse.java`: Data models

**Technologies:**
- Java 17
- Javalin (web framework)
- PlantUML (diagram generation)
- Gson (JSON processing)
- Maven (build tool)

### Java Client

**Location:** `clients/java/`

A Java client library and CLI tool for interacting with the MCP server.

**Features:**
- Generate diagrams programmatically
- Validate UML syntax
- Query server capabilities
- Save diagrams to files
- Command-line interface

**Usage:**
```java
UMLMCPClient client = new UMLMCPClient("http://localhost:8080");
MCPResponse response = client.generateDiagram(uml, "png", "sequence");
client.saveDiagram(response, "output.png");
```

### Python Client

**Location:** `clients/python/`

A Python client library and CLI tool for interacting with the MCP server.

**Features:**
- Same features as Java client
- More concise syntax
- Easy integration with Python projects

**Usage:**
```python
from uml_mcp_client import UMLMCPClient

client = UMLMCPClient("http://localhost:8080")
response = client.generate_diagram(uml_source, "png", "sequence")
client.save_diagram(response, "output.png")
```

### Documentation

**Location:** `docs/`

Comprehensive documentation including:
- **API.md**: Complete API reference with all endpoints, request/response formats, error codes
- **QUICKSTART.md**: 5-minute getting started guide
- **DEPLOYMENT.md**: Production deployment guide for various platforms

### Examples

**Location:** `examples/`

Real-world UML diagram examples:
- **sequence-diagram.puml**: Authentication flow
- **class-diagram.puml**: E-commerce system
- **usecase-diagram.puml**: Online shopping system
- **activity-diagram.puml**: Order processing workflow

### Scripts

**Location:** `scripts/`

Utility scripts for development and testing:
- **build-all.sh**: Builds server and both clients
- **test-server.sh**: Comprehensive server test suite
- **run-example.sh**: Generates example diagrams

## Build Artifacts

After building, the following artifacts are created:

```
server/target/
└── uml-mcp-server-1.0.0.jar         # Executable server JAR

clients/java/target/
└── uml-mcp-java-client-1.0.0.jar    # Executable client JAR
```

## Configuration Files

| File | Purpose |
|------|---------|
| `server/pom.xml` | Maven build configuration for server |
| `clients/java/pom.xml` | Maven build configuration for Java client |
| `clients/python/requirements.txt` | Python dependencies |
| `server/docker-compose.yml` | Docker Compose setup |
| `server/Dockerfile` | Docker image definition |
| `.gitignore` | Git ignore rules |

## API Endpoints

The server exposes the following endpoints:

### MCP Endpoints

- `POST /mcp/generate` - Generate UML diagram
- `POST /mcp/validate` - Validate UML source
- `POST /mcp/capabilities` - Get capabilities

### REST Endpoints

- `POST /api/v1/diagrams/generate` - Generate diagram
- `POST /api/v1/diagrams/validate` - Validate UML
- `GET /api/v1/capabilities` - Get capabilities
- `GET /health` - Health check

## Data Flow

```
┌──────────────┐
│   Client     │
│ (Java/Python)│
└──────┬───────┘
       │ HTTP POST (JSON-RPC)
       │
┌──────▼───────────────────┐
│   MCPServer (Javalin)    │
├──────────────────────────┤
│   DiagramHandler         │
├──────────────────────────┤
│   PlantUMLService        │
├──────────────────────────┤
│   PlantUML Library       │
└──────┬───────────────────┘
       │
┌──────▼───────────────────┐
│  Diagram (PNG/SVG/TXT)   │
└──────────────────────────┘
```

## Extension Points

The architecture supports easy extension:

1. **Add new diagram types**: Update `PlantUMLService.getSupportedDiagramTypes()`
2. **Add new formats**: Extend `PlantUMLService.getFileFormat()`
3. **Add authentication**: Modify `DiagramHandler` to check auth headers
4. **Add caching**: Implement caching layer in `PlantUMLService`
5. **Add rate limiting**: Use Javalin middleware

## Testing

### Manual Testing

```bash
# Start server
java -jar server/target/uml-mcp-server-1.0.0.jar

# Run test suite
./scripts/test-server.sh

# Generate examples
./scripts/run-example.sh
```

### Automated Testing

```bash
# Server tests
cd server
mvn test

# Client tests (when implemented)
cd clients/java
mvn test
```

## Deployment Options

1. **Standalone JAR**: Run directly with Java
2. **Docker Container**: Use provided Dockerfile
3. **Docker Compose**: Multi-container setup
4. **Kubernetes**: Use provided deployment YAML
5. **Cloud Services**: AWS ECS, Google Cloud Run, etc.

See [DEPLOYMENT.md](docs/DEPLOYMENT.md) for details.

## Dependencies

### Server Dependencies

- Javalin 5.6.3 (web framework)
- PlantUML 1.2024.3 (diagram generation)
- Gson 2.10.1 (JSON processing)
- SLF4J 2.0.9 (logging)

### Client Dependencies

**Java:**
- Gson 2.10.1 (JSON processing)
- Java 17 HttpClient (HTTP requests)

**Python:**
- requests >= 2.31.0 (HTTP requests)

## Security Considerations

- No built-in authentication (add via reverse proxy or modify code)
- CORS enabled for all origins (configure for production)
- Input validation performed on UML source
- No file system access from UML diagrams
- Rate limiting recommended for production

## Performance

- Typical diagram generation: 100-500ms
- Memory usage: ~256MB base + diagram complexity
- Concurrent requests supported
- Stateless design enables horizontal scaling

## Future Enhancements

Potential improvements:
- [ ] Add authentication/authorization
- [ ] Implement diagram caching
- [ ] Add metrics/monitoring endpoints
- [ ] Support batch diagram generation
- [ ] Add WebSocket support for real-time updates
- [ ] Implement request rate limiting
- [ ] Add support for custom PlantUML themes
- [ ] Create additional client libraries (Go, Node.js, etc.)

## Contributing

To contribute to this project:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Support

For issues or questions:
- Check documentation in `docs/`
- Review examples in `examples/`
- Open an issue on GitHub
- See [README.md](README.md) for more information

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) file for details.
