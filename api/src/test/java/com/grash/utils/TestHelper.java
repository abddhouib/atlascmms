package com.grash.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.grash.dto.RestResponsePage;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

public class TestHelper {

    public static String generatePhone() {
        return String.valueOf(TestHelper.generateRandomInt(8));
    }

    public static String generateString() {
        return UUID.randomUUID().toString();
    }

    public static int generateRandomInt(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }

        int min = (int) Math.pow(10, length - 1); // Smallest number with the desired length
        int max = (int) Math.pow(10, length) - 1; // Largest number with the desired length

        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }

    public static int generateRandomIntBetween(int low, int high) {
        Random r = new Random();
        return r.nextInt(high - low) + low;
    }

    public static double generateRandomDoubleBetween(double low, double high) {
        Random r = new Random();
        return low + (high - low) * r.nextDouble();
    }

    public static BigDecimal generateMoneyAmount() {
        return BigDecimal.valueOf(TestHelper.generateRandomInt(4));
    }

    public static String generateEmail() {
        return TestHelper.generateEightCharString() + "@" + TestHelper.generateEightCharString() + ".com";
    }

    public static String generateEightCharString() {
        StringBuilder returnValue = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            returnValue.append(ALPHABET.charAt(new SecureRandom().nextInt(ALPHABET.length())));
        }
        return returnValue.toString();
    }

    public static boolean areDatesClose(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return date1 == date2;
        }
        long differenceInMilli = Math.abs(date1.getTime() - date2.getTime());
        return differenceInMilli <= 1000;
    }

    public static <E> Optional<E> getRandomFromCollection(Collection<E> e) {
        return e.stream()
                .skip((int) (e.size() * Math.random()))
                .findFirst();
    }

    /**
     * Creates a TypeReference for RestResponsePage of the specified type.
     * This is useful for deserializing paginated responses in tests.
     *
     * @param <T> The type of objects in the page
     * @return TypeReference for RestResponsePage<T>
     */
    public static <T> TypeReference<RestResponsePage<T>> getPageTypeReference() {
        return new TypeReference<RestResponsePage<T>>() {
        };
    }
}
