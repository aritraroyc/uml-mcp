package com.mcp.uml.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model representing an MCP response for UML diagram generation
 */
public class MCPResponse {

    @SerializedName("jsonrpc")
    private String jsonrpc = "2.0";

    @SerializedName("id")
    private String id;

    @SerializedName("result")
    private ResponseResult result;

    @SerializedName("error")
    private ResponseError error;

    public MCPResponse() {
    }

    public MCPResponse(String id, ResponseResult result) {
        this.id = id;
        this.result = result;
    }

    public MCPResponse(String id, ResponseError error) {
        this.id = id;
        this.error = error;
    }

    // Getters and setters
    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ResponseResult getResult() {
        return result;
    }

    public void setResult(ResponseResult result) {
        this.result = result;
    }

    public ResponseError getError() {
        return error;
    }

    public void setError(ResponseError error) {
        this.error = error;
    }

    public static class ResponseResult {
        @SerializedName("diagram")
        private String diagram; // Base64 encoded diagram for binary formats

        @SerializedName("diagramText")
        private String diagramText; // Text diagram for txt format

        @SerializedName("format")
        private String format;

        @SerializedName("diagramType")
        private String diagramType;

        @SerializedName("timestamp")
        private long timestamp;

        public ResponseResult() {
            this.timestamp = System.currentTimeMillis();
        }

        public ResponseResult(String diagram, String format, String diagramType) {
            this.diagram = diagram;
            this.format = format;
            this.diagramType = diagramType;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public String getDiagram() {
            return diagram;
        }

        public void setDiagram(String diagram) {
            this.diagram = diagram;
        }

        public String getDiagramText() {
            return diagramText;
        }

        public void setDiagramText(String diagramText) {
            this.diagramText = diagramText;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getDiagramType() {
            return diagramType;
        }

        public void setDiagramType(String diagramType) {
            this.diagramType = diagramType;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class ResponseError {
        @SerializedName("code")
        private int code;

        @SerializedName("message")
        private String message;

        @SerializedName("data")
        private String data;

        public ResponseError() {
        }

        public ResponseError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public ResponseError(int code, String message, String data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        // Getters and setters
        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
