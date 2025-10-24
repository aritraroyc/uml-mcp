package com.mcp.uml.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model representing an MCP request for UML diagram generation
 */
public class MCPRequest {

    @SerializedName("jsonrpc")
    private String jsonrpc = "2.0";

    @SerializedName("id")
    private String id;

    @SerializedName("method")
    private String method;

    @SerializedName("params")
    private RequestParams params;

    public MCPRequest() {
    }

    public MCPRequest(String id, String method, RequestParams params) {
        this.id = id;
        this.method = method;
        this.params = params;
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public RequestParams getParams() {
        return params;
    }

    public void setParams(RequestParams params) {
        this.params = params;
    }

    public static class RequestParams {
        @SerializedName("uml")
        private String uml;

        @SerializedName("format")
        private String format = "png"; // png, svg, txt

        @SerializedName("diagramType")
        private String diagramType = "sequence"; // sequence, class, usecase, activity, etc.

        public RequestParams() {
        }

        public RequestParams(String uml, String format, String diagramType) {
            this.uml = uml;
            this.format = format;
            this.diagramType = diagramType;
        }

        public String getUml() {
            return uml;
        }

        public void setUml(String uml) {
            this.uml = uml;
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
    }
}
