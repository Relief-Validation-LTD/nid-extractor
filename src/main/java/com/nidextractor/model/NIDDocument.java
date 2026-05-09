package com.nidextractor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NIDDocument {

    @JsonProperty("document_type")
    private String documentType = "Bangladesh National ID Card";

    @JsonProperty("name_bengali")
    private String nameBengali;

    @JsonProperty("name_english")
    private String nameEnglish;

    @JsonProperty("father_name_bengali")
    private String fatherNameBengali;

    @JsonProperty("mother_name_bengali")
    private String motherNameBengali;

    @JsonProperty("date_of_birth")
    private String dateOfBirth;

    @JsonProperty("nid_number")
    private String nidNumber;

    @JsonProperty("extraction_engine")
    private String extractionEngine;

    @JsonProperty("confidence_score")
    private Double confidenceScore;


    @JsonIgnore
    private String rawText;

    // ── Builder ──────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final NIDDocument doc = new NIDDocument();
        public Builder nameBengali(String v)      { doc.nameBengali = v;      return this; }
        public Builder nameEnglish(String v)      { doc.nameEnglish = v;      return this; }
        public Builder fatherNameBengali(String v){ doc.fatherNameBengali = v; return this; }
        public Builder motherNameBengali(String v){ doc.motherNameBengali = v; return this; }
        public Builder dateOfBirth(String v)      { doc.dateOfBirth = v;      return this; }
        public Builder nidNumber(String v)        { doc.nidNumber = v;        return this; }
        public Builder extractionEngine(String v) { doc.extractionEngine = v; return this; }
        public Builder confidenceScore(Double v)  { doc.confidenceScore = v;  return this; }
        public Builder rawText(String v)          { doc.rawText = v;          return this; }
        public NIDDocument build()                { return doc; }
    }

    // ── Getters & Setters ─────────────────────────────────────
    public String getDocumentType()          { return documentType; }
    public String getNameBengali()           { return nameBengali; }
    public String getNameEnglish()           { return nameEnglish; }
    public String getFatherNameBengali()     { return fatherNameBengali; }
    public String getMotherNameBengali()     { return motherNameBengali; }
    public String getDateOfBirth()           { return dateOfBirth; }
    public String getNidNumber()             { return nidNumber; }
    public String getExtractionEngine()      { return extractionEngine; }
    public Double getConfidenceScore()       { return confidenceScore; }
    public String getRawText()               { return rawText; }

    @Override
    public String toString() {
        return "NIDDocument{" +
                "nameEnglish='" + nameEnglish + '\'' +
                ", nameBengali='" + nameBengali + '\'' +
                ", fatherNameBengali='" + fatherNameBengali + '\'' +
                ", motherNameBengali='" + motherNameBengali + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", nidNumber='" + nidNumber + '\'' +
                ", engine='" + extractionEngine + '\'' +
                '}';
    }
}