package com.mcp.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Java client for UML MCP Server
 */
public class UMLMCPClient {

    private final String serverUrl;
    private final HttpClient httpClient;
    private final Gson gson;

    public UMLMCPClient(String serverUrl) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Generate a UML diagram
     *
     * @param umlSource The PlantUML source code
     * @param format The output format (png, svg, txt)
     * @param diagramType The diagram type
     * @return MCPResponse object
     * @throws IOException if the request fails
     * @throws InterruptedException if the request is interrupted
     */
    public MCPResponse generateDiagram(String umlSource, String format, String diagramType)
        throws IOException, InterruptedException {

        MCPRequest request = new MCPRequest();
        request.setId(UUID.randomUUID().toString());
        request.setMethod("generate");

        MCPRequest.RequestParams params = new MCPRequest.RequestParams();
        params.setUml(umlSource);
        params.setFormat(format);
        params.setDiagramType(diagramType);
        request.setParams(params);

        String requestBody = gson.toJson(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/mcp/generate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Server returned status code: " + response.statusCode() +
                ", body: " + response.body());
        }

        return gson.fromJson(response.body(), MCPResponse.class);
    }

    /**
     * Validate UML source code
     *
     * @param umlSource The PlantUML source code to validate
     * @return MCPResponse object
     * @throws IOException if the request fails
     * @throws InterruptedException if the request is interrupted
     */
    public MCPResponse validateUML(String umlSource) throws IOException, InterruptedException {
        MCPRequest request = new MCPRequest();
        request.setId(UUID.randomUUID().toString());
        request.setMethod("validate");

        MCPRequest.RequestParams params = new MCPRequest.RequestParams();
        params.setUml(umlSource);
        request.setParams(params);

        String requestBody = gson.toJson(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/mcp/validate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest,
            HttpResponse.BodyHandlers.ofString());

        return gson.fromJson(response.body(), MCPResponse.class);
    }

    /**
     * Get server capabilities
     *
     * @return MCPResponse object
     * @throws IOException if the request fails
     * @throws InterruptedException if the request is interrupted
     */
    public MCPResponse getCapabilities() throws IOException, InterruptedException {
        MCPRequest request = new MCPRequest();
        request.setId(UUID.randomUUID().toString());
        request.setMethod("capabilities");

        String requestBody = gson.toJson(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/mcp/capabilities"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest,
            HttpResponse.BodyHandlers.ofString());

        return gson.fromJson(response.body(), MCPResponse.class);
    }

    /**
     * Save diagram to file
     *
     * @param response The MCPResponse containing the diagram
     * @param outputPath The output file path
     * @throws IOException if saving fails
     */
    public void saveDiagram(MCPResponse response, String outputPath) throws IOException {
        if (response.getError() != null) {
            throw new IOException("Cannot save diagram: " + response.getError().getMessage());
        }

        MCPResponse.ResponseResult result = response.getResult();
        Path path = Paths.get(outputPath);

        if (result.getDiagramText() != null) {
            // Text format
            Files.writeString(path, result.getDiagramText());
        } else if (result.getDiagram() != null) {
            // Binary format (Base64 encoded)
            byte[] diagramBytes = Base64.getDecoder().decode(result.getDiagram());
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                fos.write(diagramBytes);
            }
        } else {
            throw new IOException("No diagram data in response");
        }

        System.out.println("Diagram saved to: " + path.toAbsolutePath());
    }

    /**
     * Main method with example usage
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String serverUrl = args.length > 1 ? args[1] : "http://localhost:8080";
        UMLMCPClient client = new UMLMCPClient(serverUrl);

        try {
            String command = args[0];

            switch (command) {
                case "generate":
                    generateExample(client, args);
                    break;
                case "validate":
                    validateExample(client, args);
                    break;
                case "capabilities":
                    capabilitiesExample(client);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void generateExample(UMLMCPClient client, String[] args)
        throws IOException, InterruptedException {

        String umlSource = args.length > 2 ? args[2] : getDefaultSequenceDiagram();
        String format = args.length > 3 ? args[3] : "png";
        String outputPath = args.length > 4 ? args[4] : "output." + format;

        System.out.println("Generating diagram...");
        System.out.println("Format: " + format);

        MCPResponse response = client.generateDiagram(umlSource, format, "sequence");

        if (response.getError() != null) {
            System.err.println("Error: " + response.getError().getMessage());
            System.exit(1);
        }

        client.saveDiagram(response, outputPath);
        System.out.println("Success!");
    }

    private static void validateExample(UMLMCPClient client, String[] args)
        throws IOException, InterruptedException {

        String umlSource = args.length > 2 ? args[2] : getDefaultSequenceDiagram();

        System.out.println("Validating UML...");
        MCPResponse response = client.validateUML(umlSource);

        if (response.getError() != null) {
            System.err.println("Error: " + response.getError().getMessage());
        } else {
            System.out.println("Result: " + response.getResult().getDiagramText());
        }
    }

    private static void capabilitiesExample(UMLMCPClient client)
        throws IOException, InterruptedException {

        System.out.println("Fetching server capabilities...");
        MCPResponse response = client.getCapabilities();

        if (response.getError() != null) {
            System.err.println("Error: " + response.getError().getMessage());
        } else {
            System.out.println(response.getResult().getDiagramText());
        }
    }

    private static String getDefaultSequenceDiagram() {
        return "@startuml\n" +
            "Alice -> Bob: Authentication Request\n" +
            "Bob --> Alice: Authentication Response\n" +
            "Alice -> Bob: Another authentication Request\n" +
            "Alice <-- Bob: Another authentication Response\n" +
            "@enduml";
    }

    private static void printUsage() {
        System.out.println("UML MCP Java Client");
        System.out.println("\nUsage: java -jar uml-mcp-java-client.jar <command> [server-url] [options]");
        System.out.println("\nCommands:");
        System.out.println("  generate [server-url] [uml-source] [format] [output-path]");
        System.out.println("  validate [server-url] [uml-source]");
        System.out.println("  capabilities [server-url]");
        System.out.println("\nExamples:");
        System.out.println("  java -jar client.jar generate http://localhost:8080");
        System.out.println("  java -jar client.jar generate http://localhost:8080 \"@startuml...@enduml\" png output.png");
        System.out.println("  java -jar client.jar capabilities http://localhost:8080");
    }

    // Inner classes for request/response (same as server models)
    private static class MCPRequest {
        private String jsonrpc = "2.0";
        private String id;
        private String method;
        private RequestParams params;

        public String getJsonrpc() { return jsonrpc; }
        public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public RequestParams getParams() { return params; }
        public void setParams(RequestParams params) { this.params = params; }

        private static class RequestParams {
            private String uml;
            private String format;
            private String diagramType;

            public String getUml() { return uml; }
            public void setUml(String uml) { this.uml = uml; }
            public String getFormat() { return format; }
            public void setFormat(String format) { this.format = format; }
            public String getDiagramType() { return diagramType; }
            public void setDiagramType(String diagramType) { this.diagramType = diagramType; }
        }
    }

    private static class MCPResponse {
        private String jsonrpc;
        private String id;
        private ResponseResult result;
        private ResponseError error;

        public String getJsonrpc() { return jsonrpc; }
        public String getId() { return id; }
        public ResponseResult getResult() { return result; }
        public ResponseError getError() { return error; }

        private static class ResponseResult {
            private String diagram;
            private String diagramText;
            private String format;
            private String diagramType;
            private long timestamp;

            public String getDiagram() { return diagram; }
            public String getDiagramText() { return diagramText; }
            public String getFormat() { return format; }
            public String getDiagramType() { return diagramType; }
            public long getTimestamp() { return timestamp; }
        }

        private static class ResponseError {
            private int code;
            private String message;
            private String data;

            public int getCode() { return code; }
            public String getMessage() { return message; }
            public String getData() { return data; }
        }
    }
}
