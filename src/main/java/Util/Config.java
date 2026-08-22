package Util;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class Config {

    private static final String ENV_PREFIX = "ORANGEHRM_";

    private static Config instance;
    private final Map<String, Object> config;

    private Config() {
        Yaml yaml = new Yaml();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yaml");
        if (inputStream == null) {
            throw new IllegalStateException("config.yaml not found on the classpath");
        }
        config = yaml.load(inputStream);
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    /**
     * Resolution order: -Dkey=value, then the ORANGEHRM_KEY environment variable,
     * then config.yaml. The prefix on the environment variable is deliberate --
     * a bare USERNAME lookup collides with the Windows %USERNAME% variable.
     */
    private String get(String key) {
        String fromSystem = System.getProperty(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem.trim();
        }

        String fromEnv = System.getenv(ENV_PREFIX + key.toUpperCase());
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        Object fromFile = config.get(key);
        if (fromFile == null) {
            throw new IllegalStateException("Missing configuration key '" + key + "'");
        }
        return String.valueOf(fromFile);
    }

    public String getBaseUrl() {
        return get("base_url");
    }

    public String getUsername() {
        return get("username");
    }

    public String getPassword() {
        return get("password");
    }

    public String getBrowser() {
        return get("browser");
    }

    public boolean isHeadless() {
        return Boolean.parseBoolean(get("headless"));
    }

    public int getExplicitWait() {
        return Integer.parseInt(get("explicit_wait"));
    }

    public int getPageLoadTimeout() {
        return Integer.parseInt(get("page_load_timeout"));
    }

    public boolean isCleanupEnabled() {
        return Boolean.parseBoolean(get("cleanup_enabled"));
    }
}
