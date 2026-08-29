package com.lampify.validation;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Syntactic checkout checks that accept real Estonian (and similar EU) addresses:
 * 7–8 digit mobiles after +372, village names with commas and diacritics, 5-digit postcodes.
 */
public final class CheckoutFieldValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_DIGITS =
            Pattern.compile("^\\+?[0-9]{8,15}$");
    private static final Pattern POSTAL_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9\\s-]{1,11}$");
    private static final Pattern ADDRESS_PATTERN =
            Pattern.compile("^[\\p{L}\\p{M}\\p{N}\\s.,'#/\\\\-]{3,255}$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern CITY_PATTERN =
            Pattern.compile("^[\\p{L}\\p{M}][\\p{L}\\p{M}\\s.'(),/-]{1,79}$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern COUNTRY_PATTERN =
            Pattern.compile("^[\\p{L}\\p{M}][\\p{L}\\p{M}\\s.-]{1,55}$", Pattern.UNICODE_CHARACTER_CLASS);

    private CheckoutFieldValidator() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String flattened = value
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replace('\u2007', ' ')
                .replace("\uFEFF", "")
                .replaceAll("\\s+", " ")
                .trim();
        return flattened.isEmpty() ? null : flattened;
    }

    public static String normalizePhone(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return normalized.replaceAll("[().\\-\\s]", "");
    }

    public static void collectFieldErrors(
            String fullName,
            String email,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String postalCode,
            String country,
            Map<String, String> fieldErrors) {

        String name = normalize(fullName);
        if (name == null) {
            fieldErrors.put("fullName", "Full name is required");
        } else if (name.length() < 2) {
            fieldErrors.put("fullName", "Full name must be at least 2 characters");
        }

        String mail = normalize(email);
        if (mail == null) {
            fieldErrors.put("email", "Email is required");
        } else if (!EMAIL_PATTERN.matcher(mail).matches()) {
            fieldErrors.put("email", "Invalid email format");
        }

        String tel = normalizePhone(phone);
        if (tel == null) {
            fieldErrors.put("phone", "Phone is required");
        } else if (!PHONE_DIGITS.matcher(tel).matches()) {
            fieldErrors.put("phone", "Invalid phone format");
        }

        String line1 = normalize(addressLine1);
        if (line1 == null) {
            fieldErrors.put("addressLine1", "Address is required");
        } else if (!ADDRESS_PATTERN.matcher(line1).matches()) {
            fieldErrors.put("addressLine1", "Invalid address format");
        }

        String line2 = normalize(addressLine2);
        if (line2 != null && !ADDRESS_PATTERN.matcher(line2).matches()) {
            fieldErrors.put("addressLine2", "Invalid address format");
        }

        String town = normalize(city);
        if (town == null) {
            fieldErrors.put("city", "City is required");
        } else if (!CITY_PATTERN.matcher(town).matches()) {
            fieldErrors.put("city", "Invalid city");
        }

        String postal = normalize(postalCode);
        if (postal == null) {
            fieldErrors.put("postalCode", "Postal code is required");
        } else if (!POSTAL_PATTERN.matcher(postal).matches()) {
            fieldErrors.put("postalCode", "Invalid postal code format");
        }

        String nation = normalize(country);
        if (nation == null) {
            fieldErrors.put("country", "Country is required");
        } else if (!COUNTRY_PATTERN.matcher(nation).matches()) {
            fieldErrors.put("country", "Invalid country");
        }
    }
}
