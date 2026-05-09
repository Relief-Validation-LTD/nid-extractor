package com.nidextractor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nid.extractor")
public class NIDExtractorProperties {

    private String engine = "AUTO";
    private String claudeApiKey;
    private String tessDataPath;
    private boolean preprocessImage = true;

    // Getters & Setters
    public String getEngine()              { return engine; }
    public void setEngine(String v)        { this.engine = v; }
    public String getClaudeApiKey()        { return claudeApiKey; }
    public void setClaudeApiKey(String v)  { this.claudeApiKey = v; }
    public String getTessDataPath()        { return tessDataPath; }
    public void setTessDataPath(String v)  { this.tessDataPath = v; }
    public boolean isPreprocessImage()     { return preprocessImage; }
    public void setPreprocessImage(boolean v) { this.preprocessImage = v; }
}