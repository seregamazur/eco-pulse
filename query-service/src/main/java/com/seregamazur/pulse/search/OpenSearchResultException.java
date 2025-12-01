package com.seregamazur.pulse.search;

public class OpenSearchResultException extends RuntimeException {
    public OpenSearchResultException(String message) {
        super(message);
    }

    public OpenSearchResultException(String message, Throwable cause) {
        super(message, cause);
    }
}
