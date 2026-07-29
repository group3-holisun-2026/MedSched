package com.holisun.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PhoneNumberNormalizerTest {
    PhoneNumberNormalizer phoneNumberNormalizer;

    @BeforeEach
    void setUp() {
        phoneNumberNormalizer = new PhoneNumberNormalizer();
    }

    @Test
    void testNormalizeCorrectPhoneNumber() {
        List<String> mockNumbers = new ArrayList<>(List.of("0721111111", "+40721111111", "0040721111111", "072 111 1111"));

        mockNumbers.replaceAll(mockNumber -> phoneNumberNormalizer.normalizePhoneNumber(mockNumber));

        assertEquals(List.of("+40721111111", "+40721111111", "+40721111111", "+40721111111"), mockNumbers);
    }

    @Test
    void testNormalizeInvalidPhoneNumber() {
        List<String> mockNumbers = new ArrayList<>(List.of("123"));

        IllegalArgumentException exception1 = assertThrows(
            IllegalArgumentException.class,
            () -> phoneNumberNormalizer.normalizePhoneNumber(mockNumbers.getFirst())
        );
    }
}
