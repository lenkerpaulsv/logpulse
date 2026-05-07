package com.logpulse.masking;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Defines a rule for masking sensitive data in log messages.
 * Supports regex-based pattern matching with a configurable replacement string.
 */
public class MaskingRule {

    private final String name;
    private final Pattern pattern;
    private final String replacement;

    public MaskingRule(String name, String regex, String replacement) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Masking rule name must not be blank");
        }
        if (regex == null || regex.isBlank()) {
            throw new IllegalArgumentException("Masking rule regex must not be blank");
        }
        this.name = name;
        this.pattern = Pattern.compile(regex);
        this.replacement = Objects.requireNonNullElse(replacement, "[MASKED]");
    }

    /**
     * Applies this masking rule to the given input string.
     *
     * @param input the raw string to mask
     * @return the masked string
     */
    public String apply(String input) {
        if (input == null) {
            return null;
        }
        return pattern.matcher(input).replaceAll(replacement);
    }

    public String getName() {
        return name;
    }

    public String getReplacement() {
        return replacement;
    }

    @Override
    public String toString() {
        return "MaskingRule{name='" + name + "', pattern='" + pattern.pattern() + "', replacement='" + replacement + "'}";
    }
}
