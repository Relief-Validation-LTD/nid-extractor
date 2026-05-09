package com.nidextractor.ocr;

import java.io.File;

public interface OCREngine {
    /**
     * Performs OCR on the given image file and returns raw extracted text.
     */
    String extractText(File imageFile) throws Exception;

    /**
     * Returns the name of this engine.
     */
    String getEngineName();
}