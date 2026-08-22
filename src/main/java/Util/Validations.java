package Util;

import org.testng.Assert;

public class Validations {

    public static void validateTrue(boolean condition, String message) {
        Assert.assertTrue(condition, message);
    }

    public static void validateFalse(boolean condition, String message) {
        Assert.assertFalse(condition, message);
    }

    public static void validateEquals(Object actual, Object expected, String message) {
        Assert.assertEquals(actual, expected, message);
    }

    public static void validateNotEquals(Object actual, Object expected, String message) {
        Assert.assertNotEquals(actual, expected, message);
    }

    public static void validateContains(String actual, String expected, String message) {
        Assert.assertTrue(actual != null && actual.contains(expected),
                message + " -- expected '" + actual + "' to contain '" + expected + "'");
    }

    public static void validateNotBlank(String actual, String message) {
        Assert.assertTrue(actual != null && !actual.isBlank(), message);
    }
}
