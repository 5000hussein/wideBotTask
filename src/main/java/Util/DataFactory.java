package Util;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;

public final class DataFactory {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private static final String RUN_TAG =
            String.valueOf(System.currentTimeMillis()).substring(7);

    public static final DateTimeFormatter APP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-dd-MM");

    private DataFactory() {
    }

    public static String runTag() {
        return RUN_TAG;
    }

    public static Employee newEmployee() {
        int sequence = COUNTER.incrementAndGet();
        String lastName = "QA" + RUN_TAG + sequence;
        return new Employee("Automation", "Test", lastName, "EMP" + RUN_TAG + sequence);
    }

    public static String updatedFirstName() {
        return "Updated" + COUNTER.incrementAndGet() + RUN_TAG;
    }

    public static String updatedEmployeeId() {
        return "UPD" + RUN_TAG + COUNTER.incrementAndGet();
    }

    public static String formatForApp(LocalDate date) {
        return date.format(APP_DATE_FORMAT);
    }

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
