package Util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public class Helper {

    public static final String DATA_FILE = "data.json";

    //OrangeHRM 5.9 renders and parses dates as yyyy-dd-MM -- year, DAY, month
    public static final DateTimeFormatter APP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-dd-MM");

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

    public static String getData(String key) {
        return getJsonValue(DATA_FILE, key);
    }

    public static String formatForApp(LocalDate date) {
        return date.format(APP_DATE_FORMAT);
    }

    //Anchored to a Monday so the working-day count stays a stable 3
    public static LocalDate leaveStartDate() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    public static LocalDate leaveEndDate() {
        return leaveStartDate().plusDays(2);
    }

    public static LocalDate invalidEndDate() {
        return leaveStartDate().minusDays(5);
    }
}
