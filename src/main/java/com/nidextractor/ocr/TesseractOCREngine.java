package com.nidextractor.ocr;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * OCR engine using Tesseract (via Tess4J).
 * Supports Bengali + English mixed text.
 */
public class TesseractOCREngine implements OCREngine {

    private static final Logger log = LoggerFactory.getLogger(TesseractOCREngine.class);
    private final Tesseract tesseract;

    /**
     * @param tessDataPath  Path to tessdata folder
     *                      e.g. "C:/Program Files/Tesseract-OCR/tessdata" on Windows
     *                      e.g. "/usr/share/tesseract-ocr/4.00/tessdata" on Linux
     */
    public TesseractOCREngine(String tessDataPath) {
        System.setProperty("jna.library.path", "/opt/homebrew/lib");
        System.setProperty("java.library.path", "/opt/homebrew/lib");

        try {
            java.lang.reflect.Field f =
                    ClassLoader.class.getDeclaredField("sys_paths");
            f.setAccessible(true);
            f.set(null, null);
        } catch (Exception ignored) {}

        tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("ben+eng");
        tesseract.setPageSegMode(4);   // single column — best for ID cards
        tesseract.setOcrEngineMode(1); // LSTM
        tesseract.setVariable("preserve_interword_spaces", "1");
    }

    @Override
    public String extractText(File imageFile) throws TesseractException {
        log.info("Running Tesseract OCR on: {}", imageFile.getName());
        String result = tesseract.doOCR(imageFile);
        log.debug("Raw OCR output:\n{}", result);
        return result;
    }

    @Override
    public String getEngineName() {
        return "Tesseract (Tess4J)";
    }
}