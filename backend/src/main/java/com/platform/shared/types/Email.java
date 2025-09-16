package com.platform.shared.types;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

/**
 * Email value object with validation and normalization.
 */
@Embeddable
public class Email {

    private static final String EMAIL_REGEX =
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    @NotBlank(message = "Email cannot be blank")
    @Pattern(regexp = EMAIL_REGEX, message = "Invalid email format")
    private String value;

    // JPA constructor
    protected Email() {}

    public Email(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        String normalized = normalize(value);
        if (!isValid(normalized)) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }

        this.value = normalized;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private boolean isValid(String email) {
        return email.matches(EMAIL_REGEX);
    }

    public String getValue() {
        return value;
    }

    public String getDomain() {
        int atIndex = value.indexOf('@');
        return atIndex > 0 ? value.substring(atIndex + 1) : "";
    }

    public String getLocalPart() {
        int atIndex = value.indexOf('@');
        return atIndex > 0 ? value.substring(0, atIndex) : value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Email other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}