package com.nidextractor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class NIDExtractorApplication {

    public static void main(String[] args) {
        SpringApplication.run(NIDExtractorApplication.class, args);
        System.out.println("\n==============================================");
        System.out.println("  NID Extractor API running on port 8080");
        System.out.println("  Test: GET http://localhost:8080/api/nid/health");
        System.out.println("==============================================\n");
    }
}