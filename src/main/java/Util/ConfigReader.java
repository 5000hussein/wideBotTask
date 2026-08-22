package Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Single source of truth for every environment / runtime value.
 *
 * Resolution order (first hit wins):
 *   1. JVM system property   -> -Dusername=Admin
 *   2. OS environment var    -> ORANGEHRM_USERNAME (prefixed, see toEnvKey)
 *   3. config.properties     -> src/main/resources/config.properties
 *
 * This is what keeps credentials out of the test classes: a test asks for
 * ConfigReader.username(), never for the literal string.
 */
public final class ConfigReader {

    private static final Properties PROPERTIES = new Properties();
    private static final String CONFIG_FILE = "config.properties";
    private static final String ENV_PREFIX = "ORANGEHRM_";

    static {
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(CONFIG_FILE + " not found on the classpath");
            }
            PROPERTIES.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + CONFIG_FILE, e);
        }
    }

    private ConfigReader() {
    }

    public static String get(String key) {
        String systemValue = System.getProperty(key);
        if (isPresent(systemValue)) {
            return systemValue.trim();
        }

        String envValue = System.getenv(toEnvKey(key));
        if (isPresent(envValue)) {
            return envValue.trim();
        }

        String fileValue = PROPERTIES.getProperty(key);
        if (isPresent(fileValue)) {
            return fileValue.trim();
        }

        throw new IllegalStateException(
                "Missing configuration key '" + key + "'. Set it in " + CONFIG_FILE
                        + ", as -D" + key + "=<value>, or as env var " + toEnvKey(key));
    }

    public static String get(String key, String defaultValue) {
        try {
            return get(key);
        } catch (IllegalStateException e) {
            return defaultValue;
        }
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    // -- Typed accessors: the vocabulary the rest of the framework speaks ----

    public static String baseUrl() {
        return get("base.url");
    }

    public static String username() {
        return get("username");
    }

    public static String password() {
        return get("password");
    }

    public static String browser() {
        return get("browser", "chrome");
    }

    public static boolean headless() {
        return Boolean.parseBoolean(get("headless", "true"));
    }

    public static int explicitWait() {
        return Integer.parseInt(get("timeout.explicit", "20"));
    }

    public static int pageLoadTimeout() {
        return Integer.parseInt(get("timeout.pageload", "45"));
    }

    public static boolean cleanupEnabled() {
        return Boolean.parseBoolean(get("cleanup.enabled", "true"));
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * base.url -> ORANGEHRM_BASE_URL, username -> ORANGEHRM_USERNAME.
     *
     * The ORANGEHRM_ prefix is deliberate. An unprefixed lookup collides with
     * variables the OS already defines: on Windows %USERNAME% is the logged-in
     * account, so a bare "USERNAME" lookup silently resolved to the developer's
     * own login and every test failed with "Invalid credentials". Namespacing
     * the variables makes the config surface explicit and collision-free.
     */
    private static String toEnvKey(String key) {
        return ENV_PREFIX + key.toUpperCase().replace('.', '_');
    }
}
