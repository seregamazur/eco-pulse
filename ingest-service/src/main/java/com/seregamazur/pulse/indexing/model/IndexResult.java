package com.seregamazur.pulse.indexing.model;

public record IndexResult(String title, boolean success, String errorMessage) {
    public static IndexResult ok(String title) {
        return new IndexResult(title, true, null);
    }

    public static IndexResult fail(String title, Throwable ex) {
        return new IndexResult(title, false, ex.getMessage());
    }

    public static IndexResult skip(String title) {
        return new IndexResult(title, false, "No enrichment");
    }
}