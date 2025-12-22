package com.seregamazur.pulse.pipeline.daily;

import org.jsoup.Jsoup;

/**
 * Utility class to extract plain text from an HTML string.
 * <p>
 * This is used to sanitize news content by removing all HTML tags and
 * normalizing whitespace characters. It ensures the text is "clean"
 * for downstream AI processing or indexing.
 * </p>
 */
public class HtmlCleaner {

    public static String clean(String html) {
        if (html == null || html.isBlank()) return "";

        try {
            var doc = Jsoup.parse(html);
            var text = doc.text();
            return text.replace("\u00A0", " ").trim();
        } catch (Exception e) {
            throw new RuntimeException("An exception occurred when trying to clean text from html tags", e);
        }
    }
}
