package com.holisun.backend.util;

import org.springframework.stereotype.Component;

@Component
public class PhoneNumberNormalizer {
    public String normalizePhoneNumber(String number) {
        String cleanedNumber = number.replaceAll("[()-. ]", "");

        if(cleanedNumber.startsWith("+")) {
            return cleanedNumber;
        }

        if(cleanedNumber.startsWith("00")) {
            return "+" + cleanedNumber.substring(2);
        }

        if(cleanedNumber.length() == 10 && cleanedNumber.startsWith("07")) {
            return "+4" + cleanedNumber;
        }

        if(cleanedNumber.length() == 11 && cleanedNumber.startsWith("40")) {
            return "+" + cleanedNumber;
        }

        throw new IllegalArgumentException("Invalid phone number");
    }
}
