package com.nidextractor.controller;

import com.nidextractor.NIDExtractor;
import com.nidextractor.config.ExtractorConfig;
import com.nidextractor.config.NIDExtractorProperties;
import com.nidextractor.model.ApiResponse;
import com.nidextractor.model.NIDDocument;
import com.nidextractor.ocr.TesseractOCREngine;
import com.nidextractor.util.ImagePreprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/nid")
public class NIDController {

    private static final Logger log = LoggerFactory.getLogger(NIDController.class);

    private final NIDExtractorProperties props;

    @Autowired
    public NIDController(NIDExtractorProperties props) {
        this.props = props;
    }

    /**
     * POST /api/nid/extract
     * Accepts a multipart image file and returns extracted NID fields.
     */
    @PostMapping(
            value = "/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<NIDDocument>> extract(
            @RequestPart("image") MultipartFile imageFile) {

        log.info("Received extraction request: filename={}, size={}KB",
                imageFile.getOriginalFilename(),
                imageFile.getSize() / 1024);

        // Validate file
        if (imageFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No image file provided"));
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File must be an image (JPEG, PNG, etc.)"));
        }

        try {
            // Save uploaded file to a temp location
            String suffix = getExtension(imageFile.getOriginalFilename());
            File tempFile = File.createTempFile("nid_upload_", suffix);
            tempFile.deleteOnExit();
            imageFile.transferTo(tempFile);

            // Build extractor config from properties
            ExtractorConfig config = buildConfig();

            // Run extraction
            NIDExtractor extractor = new NIDExtractor(config);
            NIDDocument doc = extractor.extract(tempFile);

            // Clean up
            Files.deleteIfExists(tempFile.toPath());

            log.info("Extraction complete. NID: {}, Confidence: {}%",
                    doc.getNidNumber(), doc.getConfidenceScore());

            return ResponseEntity.ok(ApiResponse.ok(doc, "Extraction successful"));

        } catch (Exception e) {
            log.error("Extraction failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Extraction failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/nid/health
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(
                ApiResponse.ok("NID Extractor API is running on port 8080", "OK")
        );
    }

    @PostMapping(
            value = "/debug-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> debugImage(
            @RequestPart("image") MultipartFile imageFile) throws Exception {

        File tempFile = File.createTempFile("debug_", ".jpg");
        imageFile.transferTo(tempFile);

        ImagePreprocessor preprocessor = new ImagePreprocessor();
        File processed = preprocessor.preprocess(tempFile);

        // Run OCR and return raw text only
        ExtractorConfig config = buildConfig();
        TesseractOCREngine engine = new TesseractOCREngine(config.getTessDataPath());
        String rawText = engine.extractText(processed);

        return ResponseEntity.ok("=== RAW OCR OUTPUT ===\n" + rawText);
    }

    // ── Helpers ───────────────────────────────────────────────

    private ExtractorConfig buildConfig() {
        ExtractorConfig.Engine engine = switch (props.getEngine().toUpperCase()) {
            case "CLAUDE_VISION" -> ExtractorConfig.Engine.CLAUDE_VISION;
            case "TESSERACT"     -> ExtractorConfig.Engine.TESSERACT;
            default              -> ExtractorConfig.Engine.AUTO;
        };

        return ExtractorConfig.defaults()
                .engine(engine)
                .claudeApiKey(props.getClaudeApiKey())
                .tessDataPath(props.getTessDataPath())
                .preprocessImage(props.isPreprocessImage());
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}