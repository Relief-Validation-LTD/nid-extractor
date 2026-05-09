package com.nidextractor.config;

public class ExtractorConfig {

    public enum Engine { TESSERACT, CLAUDE_VISION, AUTO }

    private Engine engine = Engine.AUTO;
    private String tessDataPath = "/usr/share/tesseract-ocr/4.00/tessdata"; // Linux default
    private String claudeApiKey;
    private boolean preprocessImage = true;

    // ── Builder ───────────────────────────────────────────────
    public static ExtractorConfig defaults() { return new ExtractorConfig(); }

    public ExtractorConfig engine(Engine e)         { this.engine = e;          return this; }
    public ExtractorConfig tessDataPath(String p)   { this.tessDataPath = p;    return this; }
    public ExtractorConfig claudeApiKey(String k)   { this.claudeApiKey = k;    return this; }
    public ExtractorConfig preprocessImage(boolean b){ this.preprocessImage = b; return this; }

    // ── Getters ───────────────────────────────────────────────
    public Engine getEngine()           { return engine; }
    public String getTessDataPath()     { return tessDataPath; }
    public String getClaudeApiKey()     { return claudeApiKey; }
    public boolean isPreprocessImage()  { return preprocessImage; }
}