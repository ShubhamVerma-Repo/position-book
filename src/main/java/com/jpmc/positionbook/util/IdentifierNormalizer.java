package com.jpmc.positionbook.util;

public final class IdentifierNormalizer {

    private IdentifierNormalizer() {
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
