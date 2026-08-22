package Util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;

public final class DataFactory {

    //TestData
    private static final String DATA_FILE = "data.json";
    private static final String FIRST_NAME = Helper.getJsonValue(DATA_FILE, "firstName");
    private static final String MIDDLE_NAME = Helper.getJsonValue(DATA_FILE, "middleName");
    private static final String LAST_NAME_PREFIX = Helper.getJsonValue(DATA_FILE, "lastNamePrefix");
    private static final String EMPLOYEE_ID_PREFIX = Helper.getJsonValue(DATA_FILE, "employeeIdPrefix");

    //Uniqueness: every generated value carries this run's tag, so two runs never collide
    private static final String RUN_TAG = String.valueOf(System.currentTimeMillis()).substring(7);
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    /**
     * OrangeHRM 5.9 renders and parses dates as yyyy-dd-MM -- year, DAY, month.
     * Verified: the 2026 leave period is displayed as "2026-01-01 - 2026-31-12".
     */
    public static final DateTimeFormatter APP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-dd-MM");

    private DataFactory() {
    }

    public static String runTag() {
        return RUN_TAG;
    }

    public static Employee newEmployee() {
        int sequence = COUNTER.incrementAndGet();
        return new Employee(FIRST_NAME, MIDDLE_NAME,
                LAST_NAME_PREFIX + RUN_TAG + sequence,
                EMPLOYEE_ID_PREFIX + RUN_TAG + sequence);
    }

    public static String updatedFirstName() {
        return Helper.getJsonValue(DATA_FILE, "updatedFirstNamePrefix")
                + COUNTER.incrementAndGet() + RUN_TAG;
    }

    public static String updatedEmployeeId() {
        return Helper.getJsonValue(DATA_FILE, "updatedEmployeeIdPrefix")
                + RUN_TAG + COUNTER.incrementAndGet();
    }

    public static String negativeFirstName() {
        return Helper.getJsonValue(DATA_FILE, "negativeFirstNamePrefix") + RUN_TAG;
    }

    public static String negativeLastName() {
        return Helper.getJsonValue(DATA_FILE, "negativeLastNamePrefix") + RUN_TAG;
    }

    public static String overlongFirstName() {
        return "A".repeat(Integer.parseInt(Helper.getJsonValue(DATA_FILE, "overlongFirstNameLength")));
    }

    public static String entitlementDays() {
        return Helper.getJsonValue(DATA_FILE, "entitlementDays");
    }

    public static String invalidPassword() {
        return Helper.getJsonValue(DATA_FILE, "invalidPassword");
    }

    //Dates are calculated at runtime, never hard-coded
    public static String formatForApp(LocalDate date) {
        return date.format(APP_DATE_FORMAT);
    }

    /** Anchored to a Monday so the working-day count stays a stable 3. */
    public static LocalDate leaveStartDate() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    public static LocalDate leaveEndDate() {
        return leaveStartDate().plusDays(2);
    }

    public static int expectedLeaveDays() {
        return 3;
    }

    public static LocalDate invalidEndDate() {
        return leaveStartDate().minusDays(5);
    }

    public record Employee(String firstName, String middleName, String lastName, String employeeId) {
        public String fullName() {
            return firstName + " " + middleName + " " + lastName;
        }

        public String firstAndMiddleName() {
            return firstName + " " + middleName;
        }

        public Employee withFirstName(String newFirstName) {
            return new Employee(newFirstName, middleName, lastName, employeeId);
        }

        public Employee withEmployeeId(String newEmployeeId) {
            return new Employee(firstName, middleName, lastName, newEmployeeId);
        }
    }
}
