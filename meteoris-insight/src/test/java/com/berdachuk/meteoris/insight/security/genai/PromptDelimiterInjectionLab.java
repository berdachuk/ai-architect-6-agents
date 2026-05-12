package com.berdachuk.meteoris.insight.security.genai;

/**
 * Lab-only code for OWASP GenAI red teaming: delimiter / instruction-boundary abuse (prompt
 * injection family). Not used by production Spring beans — see tests and
 * {@code docs/GENAI-SECURITY-REDTEAM-LAB.md}.
 */
public final class PromptDelimiterInjectionLab {

    private PromptDelimiterInjectionLab() {}

    /**
     * Vulnerable pattern: a fixed system segment ends with a sentinel the model is told to treat
     * as a boundary, but the same sentinel is allowed inside user-controlled text. An attacker
     * can close the synthetic "system" region early and append new instructions.
     */
    public static final class Vulnerable {

        public static final String END_SYSTEM_SENTINEL = "<<<END_SYSTEM>>>";

        private static final String SYSTEM_PREFIX =
                "You are Meteoris. Refuse harmful requests. End system instructions at sentinel.\n"
                        + END_SYSTEM_SENTINEL;

        public String combineForModel(String userMessage) {
            return SYSTEM_PREFIX + "\n" + (userMessage == null ? "" : userMessage);
        }

        /**
         * True when the combined prompt contains a second occurrence of the end-system sentinel,
         * meaning the user likely injected a parallel "system" block.
         */
        public boolean attackerCanCloseSystemBlockEarly(String userMessage) {
            String combined = combineForModel(userMessage);
            int first = combined.indexOf(END_SYSTEM_SENTINEL);
            if (first < 0) {
                return false;
            }
            return combined.indexOf(END_SYSTEM_SENTINEL, first + END_SYSTEM_SENTINEL.length()) >= 0;
        }
    }

    /**
     * Mitigated pattern: reject user content that contains the sentinel; wrap remaining user text
     * in explicit tags so downstream prompts separate roles more clearly (defense in depth).
     */
    public static final class Mitigated {

        public static final String END_SYSTEM_SENTINEL = Vulnerable.END_SYSTEM_SENTINEL;

        private static final String SYSTEM_PREFIX =
                "You are Meteoris. Refuse harmful requests. End system instructions at sentinel.\n"
                        + END_SYSTEM_SENTINEL;

        public String combineForModel(String userMessage) {
            if (userMessage == null) {
                userMessage = "";
            }
            if (userMessage.contains(END_SYSTEM_SENTINEL)) {
                throw new IllegalArgumentException("User message contains reserved delimiter sequence");
            }
            return SYSTEM_PREFIX + "\n<user_input>\n" + userMessage + "\n</user_input>";
        }

        public boolean attackerCanCloseSystemBlockEarly(String userMessage) {
            try {
                String combined = combineForModel(userMessage);
                int first = combined.indexOf(END_SYSTEM_SENTINEL);
                if (first < 0) {
                    return false;
                }
                return combined.indexOf(END_SYSTEM_SENTINEL, first + END_SYSTEM_SENTINEL.length())
                        >= 0;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}
