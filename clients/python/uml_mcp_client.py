#!/usr/bin/env python3
"""
UML MCP Python Client

A Python client for interacting with the UML MCP Server to generate
UML diagrams using PlantUML.
"""

import argparse
import base64
import json
import sys
import uuid
from pathlib import Path
from typing import Optional, Dict, Any

try:
    import requests
except ImportError:
    print("Error: requests library is required. Install it with: pip install requests")
    sys.exit(1)


class MCPRequest:
    """MCP Request model"""

    def __init__(self, method: str, params: Dict[str, Any], request_id: Optional[str] = None):
        self.jsonrpc = "2.0"
        self.id = request_id or str(uuid.uuid4())
        self.method = method
        self.params = params

    def to_dict(self) -> Dict[str, Any]:
        return {
            "jsonrpc": self.jsonrpc,
            "id": self.id,
            "method": self.method,
            "params": self.params
        }


class MCPResponse:
    """MCP Response model"""

    def __init__(self, response_data: Dict[str, Any]):
        self.jsonrpc = response_data.get("jsonrpc")
        self.id = response_data.get("id")
        self.result = response_data.get("result")
        self.error = response_data.get("error")

    def has_error(self) -> bool:
        return self.error is not None

    def get_error_message(self) -> str:
        if self.error:
            return f"Error {self.error.get('code')}: {self.error.get('message')}"
        return ""


class UMLMCPClient:
    """
    Client for UML MCP Server
    """

    def __init__(self, server_url: str = "http://localhost:8080", timeout: int = 30):
        """
        Initialize the MCP client

        Args:
            server_url: The base URL of the MCP server
            timeout: Request timeout in seconds
        """
        self.server_url = server_url.rstrip('/')
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json'
        })

    def generate_diagram(
        self,
        uml_source: str,
        output_format: str = "png",
        diagram_type: str = "sequence"
    ) -> MCPResponse:
        """
        Generate a UML diagram

        Args:
            uml_source: PlantUML source code
            output_format: Output format (png, svg, txt)
            diagram_type: Diagram type (sequence, class, usecase, etc.)

        Returns:
            MCPResponse object

        Raises:
            requests.RequestException: If the request fails
        """
        params = {
            "uml": uml_source,
            "format": output_format,
            "diagramType": diagram_type
        }

        request = MCPRequest("generate", params)
        response = self._send_request("/mcp/generate", request)

        return MCPResponse(response)

    def validate_uml(self, uml_source: str) -> MCPResponse:
        """
        Validate PlantUML source code

        Args:
            uml_source: PlantUML source code to validate

        Returns:
            MCPResponse object

        Raises:
            requests.RequestException: If the request fails
        """
        params = {"uml": uml_source}
        request = MCPRequest("validate", params)
        response = self._send_request("/mcp/validate", request)

        return MCPResponse(response)

    def get_capabilities(self) -> MCPResponse:
        """
        Get server capabilities

        Returns:
            MCPResponse object

        Raises:
            requests.RequestException: If the request fails
        """
        request = MCPRequest("capabilities", {})
        response = self._send_request("/mcp/capabilities", request)

        return MCPResponse(response)

    def save_diagram(self, response: MCPResponse, output_path: str) -> None:
        """
        Save diagram to file

        Args:
            response: MCPResponse containing the diagram
            output_path: Path to save the diagram

        Raises:
            ValueError: If the response contains an error or no diagram data
            IOError: If saving fails
        """
        if response.has_error():
            raise ValueError(f"Cannot save diagram: {response.get_error_message()}")

        result = response.result
        if not result:
            raise ValueError("No result in response")

        path = Path(output_path)

        if result.get('diagramText'):
            # Text format
            path.write_text(result['diagramText'])
        elif result.get('diagram'):
            # Binary format (Base64 encoded)
            diagram_bytes = base64.b64decode(result['diagram'])
            path.write_bytes(diagram_bytes)
        else:
            raise ValueError("No diagram data in response")

        print(f"Diagram saved to: {path.absolute()}")

    def _send_request(self, endpoint: str, request: MCPRequest) -> Dict[str, Any]:
        """
        Send request to the server

        Args:
            endpoint: API endpoint
            request: MCPRequest object

        Returns:
            Response data as dictionary

        Raises:
            requests.RequestException: If the request fails
        """
        url = f"{self.server_url}{endpoint}"
        response = self.session.post(
            url,
            json=request.to_dict(),
            timeout=self.timeout
        )

        response.raise_for_status()
        return response.json()


def get_default_sequence_diagram() -> str:
    """Get a default sequence diagram example"""
    return """@startuml
Alice -> Bob: Authentication Request
Bob --> Alice: Authentication Response
Alice -> Bob: Another authentication Request
Alice <-- Bob: Another authentication Response
@enduml"""


def get_default_class_diagram() -> str:
    """Get a default class diagram example"""
    return """@startuml
class User {
    -String username
    -String password
    +login()
    +logout()
}

class Admin {
    +manageUsers()
}

User <|-- Admin
@enduml"""


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(
        description="UML MCP Python Client - Generate UML diagrams using PlantUML"
    )

    parser.add_argument(
        "command",
        choices=["generate", "validate", "capabilities"],
        help="Command to execute"
    )

    parser.add_argument(
        "--server",
        default="http://localhost:8080",
        help="MCP server URL (default: http://localhost:8080)"
    )

    parser.add_argument(
        "--uml",
        help="PlantUML source code or path to .puml file"
    )

    parser.add_argument(
        "--format",
        default="png",
        choices=["png", "svg", "txt"],
        help="Output format (default: png)"
    )

    parser.add_argument(
        "--output",
        help="Output file path (default: output.<format>)"
    )

    parser.add_argument(
        "--diagram-type",
        default="sequence",
        help="Diagram type (default: sequence)"
    )

    parser.add_argument(
        "--example",
        choices=["sequence", "class"],
        help="Use a built-in example diagram"
    )

    args = parser.parse_args()

    # Create client
    client = UMLMCPClient(server_url=args.server)

    try:
        if args.command == "generate":
            # Get UML source
            if args.example:
                if args.example == "sequence":
                    uml_source = get_default_sequence_diagram()
                else:
                    uml_source = get_default_class_diagram()
            elif args.uml:
                # Check if it's a file path
                uml_path = Path(args.uml)
                if uml_path.exists() and uml_path.is_file():
                    uml_source = uml_path.read_text()
                else:
                    uml_source = args.uml
            else:
                uml_source = get_default_sequence_diagram()
                print("No UML source provided, using default sequence diagram")

            # Generate diagram
            print(f"Generating {args.format} diagram...")
            response = client.generate_diagram(
                uml_source,
                args.format,
                args.diagram_type
            )

            if response.has_error():
                print(f"Error: {response.get_error_message()}", file=sys.stderr)
                sys.exit(1)

            # Save diagram
            output_path = args.output or f"output.{args.format}"
            client.save_diagram(response, output_path)
            print("Success!")

        elif args.command == "validate":
            if not args.uml and not args.example:
                print("Error: --uml or --example is required for validate command",
                      file=sys.stderr)
                sys.exit(1)

            # Get UML source
            if args.example:
                uml_source = (get_default_sequence_diagram() if args.example == "sequence"
                             else get_default_class_diagram())
            else:
                uml_path = Path(args.uml)
                if uml_path.exists() and uml_path.is_file():
                    uml_source = uml_path.read_text()
                else:
                    uml_source = args.uml

            print("Validating UML...")
            response = client.validate_uml(uml_source)

            if response.has_error():
                print(f"Error: {response.get_error_message()}", file=sys.stderr)
                sys.exit(1)

            print(f"Result: {response.result.get('diagramText')}")

        elif args.command == "capabilities":
            print("Fetching server capabilities...")
            response = client.get_capabilities()

            if response.has_error():
                print(f"Error: {response.get_error_message()}", file=sys.stderr)
                sys.exit(1)

            print(response.result.get('diagramText'))

    except requests.RequestException as e:
        print(f"Request failed: {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
