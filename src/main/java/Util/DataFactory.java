package Util;

import net.datafaker.Faker;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates the test data for a run. Nothing the suite types into the
 * application is a fixed literal: names, employee ids and dates are all
 * derived at runtime.
 *
 * Uniqueness strategy -- a run tag of {timestamp}{counter} is embedded in
 * every generated last name and employee id, so:
 *   - two runs never collide, even in parallel or on a shared environment;
 *   - a record created by this suite is always identifiable as ours;
 *   - a search by last name is guaranteed to return exactly one row, which is
 *     what lets the assertions be exact rather than "contains something".
 */
public final class DataFactory {

    private static final Faker FAKER = new Faker();
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    /** Stable for the lifetime of the JVM, i.e. one suite execution. */
    private static final String RUN_TAG =
            String.valueOf(System.currentTimeMillis()).substring(7);

    /**
     * OrangeHRM 5.9 renders and parses dates as yyyy-dd-MM -- year, DAY, month.
     * Verified against the application: the 2026 leave period is displayed as
     * "2026-01-01 - 2026-31-12", i.e. 31 December is written 2026-31-12.
     * Using ISO yyyy-MM-dd here silently submits the wrong day whenever the day
     * of month is <= 12, and is rejected outright when it is > 12.
     */
    public static final DateTimeFormatter APP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-dd-MM");

    private DataFactory() {
    }

    public static String runTag() {
        return RUN_TAG;
    }

    // ------------------------------------------------------------ employees

    /** A fresh employee whose last name is unique across runs. */
    public static Employee newEmployee() {
        int sequence = COUNTER.incrementAndGet();
        String lastName = "QA" + RUN_TAG + sequence;
        return new Employee("Automation", "Test", lastName, "EMP" + RUN_TAG + sequence);
    }

    /** A different-but-valid value, for the edit scenario. */
    public static String updatedFirstName() {
        return "Updated" + COUNTER.incrementAndGet() + RUN_TAG;
    }

    public static String updatedEmployeeId() {
        return "UPD" + RUN_TAG + COUNTER.incrementAndGet();
    }

    public static String randomFirstName() {
        return FAKER.name().firstName().replaceAll("[^A-Za-z]", "");
    }

    // ---------------------------------------------------------------- dates

    public static String formatForApp(LocalDate date) {
        return date.format(APP_DATE_FORMAT);
    }

    /**
     * Start of a leave window: the Monday of next week.
     * Anchoring to a Monday keeps the working-day count deterministic --
     * a window that straddles a weekend would make "number of days" vary.
     */
    public static LocalDate leaveStartDate() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    /** Monday + 2 = Wednesday, so the request is exactly 3 working days. */
    public static LocalDate leaveEndDate() {
        return leaveStartDate().plusDays(2);
    }

    public static int expectedLeaveDays() {
        return 3;
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    /** For the negative case: a To Date that precedes the From Date. */
    public static LocalDate invalidEndDate() {
        return leaveStartDate().minusDays(5);
    }

    // ------------------------------------------------------------ container

    /** Immutable carrier for the data one employee was created with. */
    public record Employee(String firstName, String middleName, String lastName, String employeeId) {

        /** How OrangeHRM shows the name in the details banner and search results. */
        public String fullName() {
            return firstName + " " + middleName + " " + lastName;
        }

        /** The "First (& Middle) Name" column joins first and middle. */
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
