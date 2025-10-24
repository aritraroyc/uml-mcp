package com.mcp.uml;

import com.mcp.uml.handlers.DiagramHandler;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main MCP Server class for UML diagram generation
 */
public class MCPServer {

    private static final Logger logger = LoggerFactory.getLogger(MCPServer.class);
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOST = "0.0.0.0";

    private final Javalin app;
    private final DiagramHandler diagramHandler;
    private final int port;
    private final String host;

    public MCPServer() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public MCPServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.diagramHandler = new DiagramHandler();
        this.app = createApp();
    }

    /**
     * Create and configure the Javalin application
     */
    private Javalin createApp() {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.anyHost();
                });
            });
        });

        // Configure routes
        configureRoutes(app);

        return app;
    }

    /**
     * Configure all application routes
     */
    private void configureRoutes(Javalin app) {
        // Health check endpoint
        app.get("/health", ctx -> {
            ctx.status(200).result("OK");
        });

        // MCP endpoints
        app.post("/mcp/generate", diagramHandler::generateDiagram);
        app.post("/mcp/validate", diagramHandler::validateUML);
        app.post("/mcp/capabilities", diagramHandler::getCapabilities);

        // Alternative REST-style endpoints for convenience
        app.post("/api/v1/diagrams/generate", diagramHandler::generateDiagram);
        app.post("/api/v1/diagrams/validate", diagramHandler::validateUML);
        app.get("/api/v1/capabilities", diagramHandler::getCapabilities);

        // Root endpoint with service information
        app.get("/", ctx -> {
            String info = "UML MCP Server v1.0.0\n\n" +
                "Available endpoints:\n" +
                "  POST /mcp/generate - Generate UML diagram\n" +
                "  POST /mcp/validate - Validate UML source\n" +
                "  POST /mcp/capabilities - Get server capabilities\n" +
                "  GET  /health - Health check\n\n" +
                "Documentation: /docs\n";
            ctx.contentType(ContentType.TEXT_PLAIN).result(info);
        });

        // Exception handlers
        app.exception(Exception.class, (e, ctx) -> {
            logger.error("Unhandled exception", e);
            ctx.status(500).json(createErrorResponse(e.getMessage()));
        });

        // 404 handler
        app.error(404, ctx -> {
            ctx.json(createErrorResponse("Endpoint not found"));
        });
    }

    /**
     * Start the server
     */
    public void start() {
        app.start(host, port);
        logger.info("MCP Server started on {}:{}", host, port);
        logger.info("PlantUML version: {}", getPlantUMLVersion());
    }

    /**
     * Stop the server
     */
    public void stop() {
        app.stop();
        logger.info("MCP Server stopped");
    }

    /**
     * Create a simple error response
     */
    private Object createErrorResponse(String message) {
        return new ErrorResponse(-1, message);
    }

    /**
     * Get PlantUML version information
     */
    private String getPlantUMLVersion() {
        try {
            return net.sourceforge.plantuml.version.Version.versionString();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Simple error response class
     */
    private static class ErrorResponse {
        private final String jsonrpc = "2.0";
        private final Error error;

        public ErrorResponse(int code, String message) {
            this.error = new Error(code, message);
        }

        private static class Error {
            private final int code;
            private final String message;

            public Error(int code, String message) {
                this.code = code;
                this.message = message;
            }
        }
    }

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        // Parse command line arguments
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host":
                case "-h":
                    if (i + 1 < args.length) {
                        host = args[++i];
                    }
                    break;
                case "--port":
                case "-p":
                    if (i + 1 < args.length) {
                        try {
                            port = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            logger.error("Invalid port number: {}", args[i]);
                            System.exit(1);
                        }
                    }
                    break;
                case "--help":
                    printHelp();
                    System.exit(0);
                    break;
            }
        }

        // Start the server
        MCPServer server = new MCPServer(host, port);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down MCP Server...");
            server.stop();
        }));

        server.start();
    }

    /**
     * Print help information
     */
    private static void printHelp() {
        System.out.println("UML MCP Server - PlantUML diagram generation service");
        System.out.println("\nUsage: java -jar uml-mcp-server.jar [options]");
        System.out.println("\nOptions:");
        System.out.println("  --host, -h <host>    Host to bind to (default: 0.0.0.0)");
        System.out.println("  --port, -p <port>    Port to listen on (default: 8080)");
        System.out.println("  --help               Show this help message");
        System.out.println("\nExample:");
        System.out.println("  java -jar uml-mcp-server.jar --host localhost --port 9000");
    }
}
