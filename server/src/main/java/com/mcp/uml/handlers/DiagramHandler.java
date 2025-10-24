package com.mcp.uml.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcp.uml.PlantUMLService;
import com.mcp.uml.models.MCPRequest;
import com.mcp.uml.models.MCPResponse;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for processing UML diagram generation requests
 */
public class DiagramHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiagramHandler.class);
    private final PlantUMLService plantUMLService;
    private final Gson gson;

    public DiagramHandler() {
        this.plantUMLService = new PlantUMLService();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Handle diagram generation request
     */
    public void generateDiagram(Context ctx) {
        logger.info("Received diagram generation request");

        try {
            // Parse the MCP request
            MCPRequest request = gson.fromJson(ctx.body(), MCPRequest.class);

            if (request == null || request.getParams() == null) {
                sendErrorResponse(ctx, request != null ? request.getId() : "unknown",
                    -32600, "Invalid Request: Missing parameters");
                return;
            }

            String requestId = request.getId();
            MCPRequest.RequestParams params = request.getParams();

            // Validate required parameters
            if (params.getUml() == null || params.getUml().trim().isEmpty()) {
                sendErrorResponse(ctx, requestId, -32602,
                    "Invalid params: 'uml' parameter is required");
                return;
            }

            // Set defaults if not provided
            String format = params.getFormat() != null ? params.getFormat() : "png";
            String diagramType = params.getDiagramType() != null ? params.getDiagramType() : "sequence";

            logger.info("Processing request - ID: {}, Format: {}, Type: {}",
                requestId, format, diagramType);

            // Generate the diagram
            try {
                String diagram = plantUMLService.generateDiagram(params.getUml(), format);

                // Create response
                MCPResponse.ResponseResult result = new MCPResponse.ResponseResult();
                result.setFormat(format);
                result.setDiagramType(diagramType);

                if (format.equalsIgnoreCase("txt")) {
                    result.setDiagramText(diagram);
                } else {
                    result.setDiagram(diagram);
                }

                MCPResponse response = new MCPResponse(requestId, result);

                ctx.status(200);
                ctx.json(response);

                logger.info("Successfully generated diagram for request: {}", requestId);

            } catch (Exception e) {
                logger.error("Error generating diagram", e);
                sendErrorResponse(ctx, requestId, -32000,
                    "Server error: " + e.getMessage(), e.toString());
            }

        } catch (Exception e) {
            logger.error("Error processing request", e);
            sendErrorResponse(ctx, "unknown", -32700,
                "Parse error: " + e.getMessage());
        }
    }

    /**
     * Handle validation request
     */
    public void validateUML(Context ctx) {
        logger.info("Received UML validation request");

        try {
            MCPRequest request = gson.fromJson(ctx.body(), MCPRequest.class);

            if (request == null || request.getParams() == null ||
                request.getParams().getUml() == null) {
                sendErrorResponse(ctx, request != null ? request.getId() : "unknown",
                    -32602, "Invalid params: 'uml' parameter is required");
                return;
            }

            String requestId = request.getId();
            boolean isValid = plantUMLService.validateUML(request.getParams().getUml());

            MCPResponse.ResponseResult result = new MCPResponse.ResponseResult();
            result.setDiagramText(isValid ? "Valid UML" : "Invalid UML");

            MCPResponse response = new MCPResponse(requestId, result);
            ctx.status(200);
            ctx.json(response);

        } catch (Exception e) {
            logger.error("Error validating UML", e);
            sendErrorResponse(ctx, "unknown", -32700,
                "Parse error: " + e.getMessage());
        }
    }

    /**
     * Handle capabilities request
     */
    public void getCapabilities(Context ctx) {
        logger.info("Received capabilities request");

        try {
            MCPRequest request = gson.fromJson(ctx.body(), MCPRequest.class);
            String requestId = request != null ? request.getId() : "1";

            MCPResponse.ResponseResult result = new MCPResponse.ResponseResult();
            result.setDiagramText(String.format(
                "Supported Diagram Types: %s\nSupported Formats: %s",
                String.join(", ", plantUMLService.getSupportedDiagramTypes()),
                String.join(", ", plantUMLService.getSupportedFormats())
            ));

            MCPResponse response = new MCPResponse(requestId, result);
            ctx.status(200);
            ctx.json(response);

        } catch (Exception e) {
            logger.error("Error getting capabilities", e);
            ctx.status(500).result("Internal Server Error");
        }
    }

    /**
     * Send error response
     */
    private void sendErrorResponse(Context ctx, String requestId, int code, String message) {
        sendErrorResponse(ctx, requestId, code, message, null);
    }

    /**
     * Send error response with additional data
     */
    private void sendErrorResponse(Context ctx, String requestId, int code,
                                   String message, String data) {
        MCPResponse.ResponseError error = new MCPResponse.ResponseError(code, message, data);
        MCPResponse response = new MCPResponse(requestId, error);
        ctx.status(400).json(response);
    }
}
