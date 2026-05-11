package com.berdachuk.meteoris.insight.security.genai;

/**
 * Lab-only code for OWASP GenAI red teaming: sensitive information disclosure via verbose API
 * errors (system integration / trust-boundary). Not production code — see tests and
 * {@code docs/GENAI-SECURITY-REDTEAM-LAB.md}.
 */
public final class VerboseApiErrorDisclosureLab {

    private VerboseApiErrorDisclosureLab() {}

    /** Maps internal exceptions directly to client-visible Problem Detail text (anti-pattern). */
    public static final class Unsafe {

        public String toClientDetail(Throwable ex) {
            return ex.getMessage() == null ? "Unknown error" : ex.getMessage();
        }
    }

    /** Returns a generic client message; operator uses correlation id in server logs only. */
    public static final class Safe {

        public String toClientDetail(Throwable ex, String correlationId) {
            return "Request failed. Reference " + correlationId + ".";
        }
    }
}
