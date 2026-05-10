package com.nidextractor.parser;

import com.nidextractor.model.NIDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BangladeshNIDParser {

    private static final Logger log = LoggerFactory.getLogger(BangladeshNIDParser.class);

    public NIDDocument parse(String rawText, String engineName) {
        log.info("Raw OCR text:\n{}", rawText);

        // Build clean line list
        String[] lines = rawText.split("\\n");
        List<String> cleanLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) cleanLines.add(trimmed);
        }

        log.info("Clean lines ({}):", cleanLines.size());
        for (int i = 0; i < cleanLines.size(); i++) {
            log.info("  [{}] {}", i, cleanLines.get(i));
        }

        // Categorise every line
        List<String> bengaliLines  = new ArrayList<>();
        List<String> englishLines  = new ArrayList<>();

        for (String line : cleanLines) {
            String bengali = extractBengali(line);
            String english = extractEnglish(line);

            if (bengali != null && bengali.length() > 4
                    && !isHeaderBengali(bengali)
                    && !containsDigits(line)          // ← ADD THIS
                    && !isSingleWord(bengali)) {       // ← ADD THIS (মিহাত is just 1 word)
                bengaliLines.add(bengali);
            }

            if (english != null && english.length() > 4
                    && !isHeaderEnglish(english)) {
                englishLines.add(english);
            }
        }

        log.info("Bengali candidates: {}", bengaliLines);
        log.info("English candidates: {}", englishLines);

        // From logs:
        // Bengali order: [0]=name  [1]=mother  [2]=father  (and possibly more noise)
        // English order: [0]=name

        NIDDocument.Builder builder = NIDDocument.builder()
                .rawText(rawText)
                .extractionEngine(engineName);

        // Bengali name — first Bengali candidate
        if (!bengaliLines.isEmpty())
            builder.nameBengali(bengaliLines.get(0));

        // Mother — second Bengali candidate
        if (bengaliLines.size() > 1)
            builder.motherNameBengali(bengaliLines.get(1));

        // Father — third Bengali candidate
        if (bengaliLines.size() > 2)
            builder.fatherNameBengali(bengaliLines.get(2));

        // English name — first English candidate
        if (!englishLines.isEmpty())
            builder.nameEnglish(englishLines.get(0));

        // DOB and NID from full raw text
        builder.dateOfBirth(extractDob(rawText));
        builder.nidNumber(extractNid(rawText));
        builder.confidenceScore(calculateConfidence(builder.build()));

        NIDDocument doc = builder.build();
        log.info("Final parsed doc: {}", doc);
        return doc;
    }

    // ── Extract only Bengali Unicode chars from a noisy line ─────────────
    private String extractBengali(String line) {
        String result = line
                .replaceAll("[^\\u0980-\\u09FF\\s]", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return result.length() > 3 ? result : null;
    }

    // ── Extract only uppercase English words from a noisy line ───────────
    private String extractEnglish(String line) {
        // Remove everything that isn't a capital letter or space
        String result = line
                .replaceAll("[^A-Z\\s]", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        // Must be at least 2 words of capital letters
        if (result.matches("[A-Z]{2,}(\\s+[A-Z]{2,})+")) return result;
        return null;
    }

    // ── Skip known header/footer Bengali text ────────────────────────────
    private boolean isHeaderBengali(String text) {
        return text.contains("গণপ্রজাতন্ত্রী") ||
                text.contains("বাংলাদেশ") ||
                text.contains("সরকার") ||
                text.contains("জাতীয়") ||
                text.contains("পরিচয়পত্র");
    }
    // Exclude lines that contain digits — signature line has NID number
    private boolean containsDigits(String line) {
        return line.matches(".*\\d+.*");
    }

    // Exclude single-word Bengali — real names have at least 2 words
    private boolean isSingleWord(String text) {
        return !text.trim().contains(" ");
    }

    // ── Skip known header English text ───────────────────────────────────
    private boolean isHeaderEnglish(String text) {
        return text.contains("BANGLADESH") ||
                text.contains("GOVERNMENT") ||
                text.contains("REPUBLIC") ||
                text.contains("NATIONAL") ||
                text.contains("CARD");
    }

    // ── NID Number ───────────────────────────────────────────────────────
    // Handles: "464 045 9105", "464.045 9105", "4640459105"
    private String extractNid(String text) {
        // Replace dots between digit groups with spaces first
        String normalized = text.replaceAll("(\\d+)\\.(\\d+)", "$1 $2");

        Pattern p1 = Pattern.compile("\\b(\\d{3}[\\s]\\d{3}[\\s]\\d{4})\\b");
        Matcher m1 = p1.matcher(normalized);
        if (m1.find()) return m1.group(1).trim();

        // After NID label
        Pattern p2 = Pattern.compile(
                "(?:NID|nid)[^\\d]{0,20}((?:\\d+\\s*){3,})",
                Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(normalized);
        if (m2.find()) {
            String raw = m2.group(1).replaceAll("\\s+", " ").trim();
            if (raw.replaceAll("\\s", "").length() >= 10) return raw;
        }

        // 10-digit fallback
        Pattern p3 = Pattern.compile("\\b(\\d{10}|\\d{13})\\b");
        Matcher m3 = p3.matcher(normalized);
        if (m3.find()) return m3.group(1);

        return null;
    }

    // ── Date of Birth ─────────────────────────────────────────────────────
    // Also fixes OCR year errors like "1902" → "1992"
    private String extractDob(String text) {
        Pattern p = Pattern.compile(
                "\\b(\\d{1,2})\\s+" +
                        "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+" +
                        "(\\d{4})\\b",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String day   = m.group(1);
            String month = capitalize(m.group(2));
            String year  = fixYear(m.group(3));
            return day + " " + month + " " + year;
        }
        return null;
    }

    // ── Fix OCR year misreads e.g. "1902" → "1992" ───────────────────────
    // NID holders are born roughly between 1930 and 2010
    private String fixYear(String year) {
        int y = Integer.parseInt(year);
        if (y >= 1930 && y <= 2010) return year; // already valid

        // Try reversing last two digits: 1902 → 1920... then 19→92 swap
        // Common OCR swap: 9↔0, so 1992 becomes 1902
        String s = year;
        // swap digit at index 2 and 3
        if (s.length() == 4) {
            char[] c = s.toCharArray();
            // try swapping c[2] and c[3]
            char tmp = c[2]; c[2] = c[3]; c[3] = tmp;
            int swapped = Integer.parseInt(new String(c));
            if (swapped >= 1930 && swapped <= 2010) return new String(c);

            // try: if 3rd digit is 0, make it 9
            c = s.toCharArray();
            if (c[2] == '0') { c[2] = '9'; }
            int fixed = Integer.parseInt(new String(c));
            if (fixed >= 1930 && fixed <= 2010) return new String(c);
        }
        return year; // return as-is if no fix found
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private double calculateConfidence(NIDDocument doc) {
        int found = 0, total = 5;
        if (doc.getNidNumber() != null)        found++;
        if (doc.getDateOfBirth() != null)       found++;
        if (doc.getNameEnglish() != null)       found++;
        if (doc.getNameBengali() != null)       found++;
        if (doc.getFatherNameBengali() != null) found++;
        return Math.round((found * 100.0 / total) * 10.0) / 10.0;
    }
}
