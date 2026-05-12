package com.berdachuk.meteoris.insight.security.genai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Demonstrates information disclosure through error bodies: internal JDBC URL leaks to client on
 * {@link VerboseApiErrorDisclosureLab.Unsafe}, hidden on {@link VerboseApiErrorDisclosureLab.Safe}.
 */
class VerboseApiErrorDisclosureLabTest {

    private static final IllegalStateException INTERNAL_FAILURE = new IllegalStateException(
            "Query failed: jdbc:postgresql://meteoris:secret@db.internal:5432/meteoris_prod");

    @Test
    void unsafe_clientSeesFullExceptionMessage_includingJdbcUrl() {
        var lab = new VerboseApiErrorDisclosureLab.Unsafe();
        String detail = lab.toClientDetail(INTERNAL_FAILURE);
        assertThat(detail).contains("jdbc:postgresql://").contains("secret@");
    }

    @Test
    void safe_clientBodyOmitsInternalDiagnostics_failedAttack() {
        var lab = new VerboseApiErrorDisclosureLab.Safe();
        String detail = lab.toClientDetail(INTERNAL_FAILURE, "req-7f3a9c");
        assertThat(detail).doesNotContain("jdbc:postgresql").doesNotContain("secret@");
        assertThat(detail).contains("req-7f3a9c");
    }
}
