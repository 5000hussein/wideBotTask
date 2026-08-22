package Util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;

public class Helper {

    /**
     * Reads a value from a JSON file under src/main/resources/TestData.
     *
     * @param filePath file name, e.g. "data.json"
     * @param key      the key to read
     */
    public static String getJsonValue(String filePath, String key) {
        try (FileReader reader = new FileReader("src/main/resources/TestData/" + filePath)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

            if (jsonObject.has(key)) {
                return jsonObject.get(key).getAsString();
            }
            throw new RuntimeException("Key not found: " + key);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + filePath, e);
        }
    }
}
