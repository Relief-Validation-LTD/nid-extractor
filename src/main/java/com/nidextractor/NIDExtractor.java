package com.nidextractor;

import com.nidextractor.config.ExtractorConfig;
import com.nidextractor.model.NIDDocument;
import com.nidextractor.ocr.ClaudeVisionEngine;
import com.nidextractor.ocr.OCREngine;
import com.nidextractor.ocr.TesseractOCREngine;
import com.nidextractor.parser.BangladeshNIDParser;
import com.nidextractor.util.ImagePreprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Main entry point for the NID Extractor library.
 *
 * Usage:
 * <pre>
 *   ExtractorConfig config = ExtractorConfig.defaults()
 *       .claudeApiKey("sk-ant-...")
 *       .engine(ExtractorConfig.Engine.CLAUDE_VISION);
 *
 *   NIDExtractor extractor = new NIDExtractor(config);
 *   NIDDocument doc = extractor.extract(new File("nid.jpg"));
 *   System.out.println(doc.getNidNumber());
 * </pre>
 */
public class NIDExtractor {

    private static final Logger log = LoggerFactory.getLogger(NIDExtractor.class);

    private final ExtractorConfig config;
    private final OCREngine ocrEngine;
    private final BangladeshNIDParser parser;
    private final ImagePreprocessor preprocessor;

    public NIDExtractor(ExtractorConfig config) {
        this.config = config;
        this.parser = new BangladeshNIDParser();
        this.preprocessor = new ImagePreprocessor();
        this.ocrEngine = buildEngine(config);
        log.info("NIDExtractor initialized with engine: {}", ocrEngine.getEngineName());
    }

    /**
     * Extract structured data from a NID card image.
     *
     * @param imageFile  JPEG or PNG image of the NID card
     * @return           Populated NIDDocument
     */
    public NIDDocument extract(File imageFile) throws Exception {
        if (!imageFile.exists()) {
            throw new IllegalArgumentException("Image file not found: " + imageFile.getAbsolutePath());
        }

        // Preprocess image for better OCR accuracy
//        File processedFile = config.isPreprocessImage()
//                ? preprocessor.preprocess(imageFile)
//                : imageFile;

        File processedFile = imageFile;
        // Run OCR
        String rawText = ocrEngine.extractText(processedFile);

        // Parse fields
        return parser.parse(rawText, ocrEngine.getEngineName());
    }

    private OCREngine buildEngine(ExtractorConfig config) {
        switch (config.getEngine()) {
            case CLAUDE_VISION:
                return new ClaudeVisionEngine(config.getClaudeApiKey());
            case TESSERACT:
                return new TesseractOCREngine(config.getTessDataPath());
            case AUTO:
            default:
                // Prefer Claude if API key is provided, fall back to Tesseract
                if (config.getClaudeApiKey() != null && !config.getClaudeApiKey().isBlank()) {
                    log.info("AUTO mode: using Claude Vision (API key found)");
                    return new ClaudeVisionEngine(config.getClaudeApiKey());
                }
                log.info("AUTO mode: using Tesseract (no API key provided)");
                return new TesseractOCREngine(config.getTessDataPath());
        }
    }
}