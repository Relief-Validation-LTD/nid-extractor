package com.nidextractor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImagePreprocessor {

    private static final Logger log = LoggerFactory.getLogger(ImagePreprocessor.class);

    public File preprocess(File inputFile) throws IOException {
        log.info("Preprocessing: {}", inputFile.getName());

        BufferedImage original = ImageIO.read(inputFile);
        log.info("Original size: {}x{}", original.getWidth(), original.getHeight());

        // Step 1: Upscale 2x only — 3x was making it too large
        BufferedImage scaled = resize(original, 2.0);

        // Step 2: Grayscale
        BufferedImage gray = toGrayscale(scaled);

        // Step 3: Mild contrast only — strong contrast destroys Bengali curves
        BufferedImage result = increaseContrast(gray, 1.3f);

        File outputFile = File.createTempFile("nid_pre_", ".png");
        outputFile.deleteOnExit();
        ImageIO.write(result, "png", outputFile);
        log.info("Preprocessed saved: {}", outputFile.getAbsolutePath());
        return outputFile;
    }

    private BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return gray;
    }

    private BufferedImage increaseContrast(BufferedImage src, float factor) {
        BufferedImage dest = new BufferedImage(
                src.getWidth(), src.getHeight(), src.getType());
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                int r = clamp((int)(((rgb >> 16 & 0xFF) - 128) * factor + 128));
                int g = clamp((int)(((rgb >> 8  & 0xFF) - 128) * factor + 128));
                int b = clamp((int)(((rgb        & 0xFF) - 128) * factor + 128));
                dest.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return dest;
    }

    private BufferedImage resize(BufferedImage src, double scale) {
        int w = (int)(src.getWidth()  * scale);
        int h = (int)(src.getHeight() * scale);
        BufferedImage dest = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dest;
    }

    private int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}