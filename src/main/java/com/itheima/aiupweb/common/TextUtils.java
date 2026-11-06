package com.itheima.aiupweb.common;

public final class TextUtils {

    private TextUtils() {
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.strip().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        int end = Math.max(0, maxLength - 1);
        if (end > 0 && Character.isHighSurrogate(normalized.charAt(end - 1))) {
            end--;
        }
        return normalized.substring(0, end) + "…";
    }

    public static String plainSnippet(String markdown, int maxLength) {
        if (markdown == null) {
            return "";
        }
        String plain = markdown
                .replaceAll("```[a-zA-Z0-9_-]*", " ")
                .replaceAll("`", "")
                .replaceAll("[#>*_~\\[\\]()!|-]", " ");
        return truncate(plain, maxLength);
    }

    public static String escapeLike(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
