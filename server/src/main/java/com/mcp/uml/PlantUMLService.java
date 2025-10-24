package com.mcp.uml;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service class for generating UML diagrams using PlantUML
 */
public class PlantUMLService {

    private static final Logger logger = LoggerFactory.getLogger(PlantUMLService.class);

    /**
     * Generate UML diagram from PlantUML source code
     *
     * @param umlSource The PlantUML source code
     * @param format The output format (png, svg, txt)
     * @return Base64 encoded diagram for binary formats, or plain text for txt format
     * @throws IOException if diagram generation fails
     */
    public String generateDiagram(String umlSource, String format) throws IOException {
        logger.info("Generating diagram with format: {}", format);

        // Ensure the UML source has proper @startuml/@enduml tags
        String processedSource = ensureUMLTags(umlSource);

        SourceStringReader reader = new SourceStringReader(processedSource);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        FileFormat fileFormat = getFileFormat(format);
        FileFormatOption option = new FileFormatOption(fileFormat);

        try {
            // Generate the diagram
            String description = reader.outputImage(outputStream, option);
            logger.debug("Diagram generated: {}", description);

            byte[] diagramBytes = outputStream.toByteArray();

            // For text format, return as plain text
            if (format.equalsIgnoreCase("txt")) {
                return new String(diagramBytes, StandardCharsets.UTF_8);
            }

            // For binary formats (png, svg), return as Base64
            return Base64.getEncoder().encodeToString(diagramBytes);

        } catch (Exception e) {
            logger.error("Error generating diagram", e);
            throw new IOException("Failed to generate diagram: " + e.getMessage(), e);
        } finally {
            outputStream.close();
        }
    }

    /**
     * Validate PlantUML source code
     *
     * @param umlSource The PlantUML source code to validate
     * @return true if valid, false otherwise
     */
    public boolean validateUML(String umlSource) {
        try {
            String processedSource = ensureUMLTags(umlSource);
            SourceStringReader reader = new SourceStringReader(processedSource);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            reader.outputImage(outputStream, new FileFormatOption(FileFormat.PNG));
            outputStream.close();
            return true;
        } catch (Exception e) {
            logger.warn("Invalid UML source: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Ensure the UML source has proper @startuml/@enduml tags
     *
     * @param umlSource The original UML source
     * @return Processed UML source with tags
     */
    private String ensureUMLTags(String umlSource) {
        String trimmed = umlSource.trim();

        // If already has tags, return as is
        if (trimmed.startsWith("@start") && trimmed.contains("@end")) {
            return trimmed;
        }

        // Otherwise, wrap with tags
        return "@startuml\n" + trimmed + "\n@enduml";
    }

    /**
     * Convert format string to PlantUML FileFormat
     *
     * @param format The format string (png, svg, txt)
     * @return Corresponding FileFormat
     */
    private FileFormat getFileFormat(String format) {
        switch (format.toLowerCase()) {
            case "svg":
                return FileFormat.SVG;
            case "txt":
            case "text":
                return FileFormat.UTXT;
            case "png":
            default:
                return FileFormat.PNG;
        }
    }

    /**
     * Get supported diagram types
     *
     * @return Array of supported diagram types
     */
    public String[] getSupportedDiagramTypes() {
        return new String[]{
            "sequence", "usecase", "class", "activity", "component",
            "state", "object", "deployment", "timing", "network",
            "wireframe", "archimate", "gantt", "mindmap", "wbs"
        };
    }

    /**
     * Get supported output formats
     *
     * @return Array of supported output formats
     */
    public String[] getSupportedFormats() {
        return new String[]{"png", "svg", "txt"};
    }
}
