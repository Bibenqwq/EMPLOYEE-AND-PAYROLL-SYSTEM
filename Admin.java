import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Admin {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path DATA_DIRECTORY = Paths.get("data");
    private static final Path EMPLOYEES_FILE = DATA_DIRECTORY.resolve("employees.csv");
    private static final Path ATTENDANCE_FILE = DATA_DIRECTORY.resolve("attendance.csv");

    private final Scanner scanner = new Scanner(System.in);
    private List<Employee> employees;
    private List<Attendance> attendanceRecords;

    public static void main(String[] args) {
        new Admin().run();
    }

    private void run() {
        try {
            employees = loadEmployees();
            attendanceRecords = loadAttendance();
        } catch (IOException ex) {
            System.out.println("Hindi mabasa ang saved data: " + ex.getMessage());
            return;
        }

        System.out.println("=== Employee Attendance and Payroll System ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Piliin ang menu: ");
            try {
                switch (choice) {
                    case "1":
                        addEmployee();
                        break;
                    case "2":
                        viewEmployees();
                        break;
                    case "3":
                        timeIn();
                        break;
                    case "4":
                        timeOut();
                        break;
                    case "5":
                        viewAttendance();
                        break;
                    case "6":
                        generatePayroll();
                        break;
                    case "0":
                        running = false;
                        System.out.println("Salamat sa paggamit ng system.");
                        break;
                    default:
                        System.out.println("Hindi valid ang pinili. Subukan muli.");
                }
            } catch (IOException ex) {
                System.out.println("Hindi na-save ang pagbabago: " + ex.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1. Magdagdag ng employee");
        System.out.println("2. Tingnan ang employees");
        System.out.println("3. Time in");
        System.out.println("4. Time out");
        System.out.println("5. Attendance report");
        System.out.println("6. Gumawa ng payroll report");
        System.out.println("0. Lumabas");
    }

    private void addEmployee() throws IOException {
        String id = readRequired("Employee ID: ");
        if (findEmployee(id) != null) {
            System.out.println("May employee na gamit ang ID na iyan.");
            return;
        }

        String name = readRequired("Pangalan: ");
        String position = readRequired("Posisyon: ");
        BigDecimal dailyRate = readPositiveMoney("Daily rate (PHP): ");

        List<Employee> updatedEmployees = new ArrayList<Employee>(employees);
        updatedEmployees.add(new Employee(id, name, position, dailyRate));
        saveEmployees(updatedEmployees);
        employees = updatedEmployees;
        System.out.println("Naidagdag at na-save ang employee.");
    }

    private void viewEmployees() {
        if (employees.isEmpty()) {
            System.out.println("Wala pang employee records.");
            return;
        }

        System.out.println();
        System.out.printf("%-15s %-25s %-20s %12s%n", "ID", "Pangalan", "Posisyon", "Daily Rate");
        for (Employee employee : employees) {
            System.out.printf("%-15s %-25s %-20s PHP %,10.2f%n",
                    employee.id, employee.name, employee.position, employee.dailyRate);
        }
    }

    private void timeIn() throws IOException {
        Employee employee = promptForEmployee();
        if (employee == null) {
            return;
        }

        for (Attendance record : attendanceRecords) {
            if (record.employeeId.equalsIgnoreCase(employee.id) && record.timeOut == null) {
                System.out.println("May hindi pa na-time-out na attendance record ang employee.");
                return;
            }
            if (record.employeeId.equalsIgnoreCase(employee.id)
                    && record.timeIn.toLocalDate().equals(LocalDate.now())) {
                System.out.println("May attendance record na ang employee para ngayong araw.");
                return;
            }
        }

        LocalDateTime timeIn = LocalDateTime.now();
        List<Attendance> updatedRecords = new ArrayList<Attendance>(attendanceRecords);
        updatedRecords.add(new Attendance(employee.id, timeIn, null));
        saveAttendance(updatedRecords);
        attendanceRecords = updatedRecords;
        System.out.println("Time in recorded: " + TIME_FORMAT.format(timeIn));
    }

    private void timeOut() throws IOException {
        Employee employee = promptForEmployee();
        if (employee == null) {
            return;
        }

        int openRecordIndex = -1;
        for (int i = 0; i < attendanceRecords.size(); i++) {
            Attendance record = attendanceRecords.get(i);
            if (record.employeeId.equalsIgnoreCase(employee.id) && record.timeOut == null) {
                openRecordIndex = i;
            }
        }
        if (openRecordIndex == -1) {
            System.out.println("Walang bukas na attendance record para sa employee na ito.");
            return;
        }

        Attendance openRecord = attendanceRecords.get(openRecordIndex);
        LocalDateTime timeOut = LocalDateTime.now();
        if (!timeOut.isAfter(openRecord.timeIn)) {
            System.out.println("Dapat mas huli ang time out kaysa time in.");
            return;
        }

        List<Attendance> updatedRecords = new ArrayList<Attendance>(attendanceRecords);
        updatedRecords.set(openRecordIndex,
                new Attendance(openRecord.employeeId, openRecord.timeIn, timeOut));
        saveAttendance(updatedRecords);
        attendanceRecords = updatedRecords;
        System.out.println("Time out recorded: " + TIME_FORMAT.format(timeOut));
    }

    private void viewAttendance() {
        if (attendanceRecords.isEmpty()) {
            System.out.println("Wala pang attendance records.");
            return;
        }

        System.out.println();
        System.out.printf("%-15s %-25s %-19s %-19s%n",
                "Employee ID", "Pangalan", "Time in", "Time out");
        for (Attendance record : attendanceRecords) {
            Employee employee = findEmployee(record.employeeId);
            String name = employee == null ? "(unknown)" : employee.name;
            String timeIn = TIME_FORMAT.format(record.timeIn);
            String timeOut = record.timeOut == null
                    ? "Naka-time in pa" : TIME_FORMAT.format(record.timeOut);
            System.out.printf("%-15s %-25s %-19s %-19s%n",
                    record.employeeId, name, timeIn, timeOut);
        }
    }

    private void generatePayroll() {
        Employee employee = promptForEmployee();
        if (employee == null) {
            return;
        }

        LocalDate startDate = readDate("Simula ng pay period (YYYY-MM-DD): ");
        LocalDate endDate = readDate("Katapusan ng pay period (YYYY-MM-DD): ");
        if (endDate.isBefore(startDate)) {
            System.out.println("Hindi puwedeng mas maaga ang katapusan kaysa simula ng pay period.");
            return;
        }

        int paidDays = 0;
        int incompleteDays = 0;
        for (Attendance record : attendanceRecords) {
            LocalDate workDate = record.timeIn.toLocalDate();
            if (record.employeeId.equalsIgnoreCase(employee.id)
                    && !workDate.isBefore(startDate) && !workDate.isAfter(endDate)) {
                if (record.timeOut == null) {
                    incompleteDays++;
                } else {
                    paidDays++;
                }
            }
        }

        BigDecimal grossPay = employee.dailyRate.multiply(BigDecimal.valueOf(paidDays))
                .setScale(2, RoundingMode.HALF_UP);
        System.out.printf("Completed paid days: %d%n", paidDays);
        if (incompleteDays > 0) {
            System.out.printf("Incomplete attendance (hindi kasama): %d%n", incompleteDays);
        }
        System.out.printf("Gross pay: PHP %,.2f%n", grossPay);

        BigDecimal deduction = readMoney("Deduction (PHP, 0 kung wala): ");
        if (deduction.compareTo(grossPay) > 0) {
            System.out.println("Hindi puwedeng mas mataas ang deduction kaysa gross pay.");
            return;
        }
        BigDecimal netPay = grossPay.subtract(deduction).setScale(2, RoundingMode.HALF_UP);

        System.out.println();
        System.out.println("=== PAYROLL REPORT ===");
        System.out.println("Employee: " + employee.name + " (" + employee.id + ")");
        System.out.println("Posisyon: " + employee.position);
        System.out.println("Pay period: " + startDate + " hanggang " + endDate);
        System.out.println("Daily rate: " + formatMoney(employee.dailyRate));
        System.out.println("Paid days: " + paidDays);
        System.out.println("Gross pay: " + formatMoney(grossPay));
        System.out.println("Deduction: " + formatMoney(deduction));
        System.out.println("Net pay: " + formatMoney(netPay));
    }

    private Employee promptForEmployee() {
        if (employees.isEmpty()) {
            System.out.println("Magdagdag muna ng employee.");
            return null;
        }
        String id = readRequired("Employee ID: ");
        Employee employee = findEmployee(id);
        if (employee == null) {
            System.out.println("Hindi makita ang employee ID na iyan.");
        }
        return employee;
    }

    private Employee findEmployee(String id) {
        for (Employee employee : employees) {
            if (employee.id.equalsIgnoreCase(id.trim())) {
                return employee;
            }
        }
        return null;
    }

    private String readRequired(String label) {
        while (true) {
            String value = prompt(label).trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Hindi puwedeng walang laman.");
        }
    }

    private BigDecimal readPositiveMoney(String label) {
        while (true) {
            BigDecimal amount = readMoney(label);
            if (amount.signum() > 0) {
                return amount;
            }
            System.out.println("Dapat mas mataas sa zero ang halaga.");
        }
    }

    private BigDecimal readMoney(String label) {
        while (true) {
            try {
                BigDecimal amount = new BigDecimal(prompt(label).trim());
                if (amount.signum() < 0) {
                    System.out.println("Hindi puwedeng negatibo ang halaga.");
                } else {
                    return amount.setScale(2, RoundingMode.HALF_UP);
                }
            } catch (NumberFormatException ex) {
                System.out.println("Maglagay ng valid na numero, halimbawa 500.00.");
            }
        }
    }

    private LocalDate readDate(String label) {
        while (true) {
            try {
                return LocalDate.parse(prompt(label).trim());
            } catch (DateTimeParseException ex) {
                System.out.println("Gamitin ang format na YYYY-MM-DD.");
            }
        }
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine();
    }

    private List<Employee> loadEmployees() throws IOException {
        List<Employee> loaded = new ArrayList<Employee>();
        if (!Files.exists(EMPLOYEES_FILE)) {
            return loaded;
        }

        try (BufferedReader reader = Files.newBufferedReader(EMPLOYEES_FILE, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] fields = line.split(",", -1);
                try {
                    if (fields.length != 4) {
                        throw new IllegalArgumentException("maling bilang ng fields");
                    }
                    Employee employee = new Employee(decode(fields[0]), decode(fields[1]),
                            decode(fields[2]), new BigDecimal(fields[3]));
                    if (employee.id.isEmpty() || employee.name.isEmpty() || employee.position.isEmpty()
                            || employee.dailyRate.signum() <= 0 || findEmployeeIn(loaded, employee.id)) {
                        throw new IllegalArgumentException("invalid o duplicate na employee");
                    }
                    loaded.add(employee);
                } catch (IllegalArgumentException ex) {
                    throw new IOException("employees.csv line " + lineNumber + ": " + ex.getMessage(), ex);
                }
            }
        }
        return loaded;
    }

    private List<Attendance> loadAttendance() throws IOException {
        List<Attendance> loaded = new ArrayList<Attendance>();
        if (!Files.exists(ATTENDANCE_FILE)) {
            return loaded;
        }

        try (BufferedReader reader = Files.newBufferedReader(ATTENDANCE_FILE, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] fields = line.split(",", -1);
                try {
                    if (fields.length != 3) {
                        throw new IllegalArgumentException("maling bilang ng fields");
                    }
                    LocalDateTime timeIn = LocalDateTime.parse(fields[1]);
                    LocalDateTime timeOut = fields[2].isEmpty() ? null : LocalDateTime.parse(fields[2]);
                    if (decode(fields[0]).isEmpty()
                            || (timeOut != null && !timeOut.isAfter(timeIn))) {
                        throw new IllegalArgumentException("invalid na attendance record");
                    }
                    loaded.add(new Attendance(decode(fields[0]), timeIn, timeOut));
                } catch (IllegalArgumentException | DateTimeParseException ex) {
                    throw new IOException("attendance.csv line " + lineNumber + ": " + ex.getMessage(), ex);
                }
            }
        }
        return loaded;
    }

    private boolean findEmployeeIn(List<Employee> records, String id) {
        for (Employee employee : records) {
            if (employee.id.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private void saveEmployees(List<Employee> records) throws IOException {
        List<String> lines = new ArrayList<String>();
        for (Employee employee : records) {
            lines.add(encode(employee.id) + "," + encode(employee.name) + ","
                    + encode(employee.position) + "," + employee.dailyRate.toPlainString());
        }
        saveLines(EMPLOYEES_FILE, lines);
    }

    private void saveAttendance(List<Attendance> records) throws IOException {
        List<String> lines = new ArrayList<String>();
        for (Attendance record : records) {
            lines.add(encode(record.employeeId) + "," + record.timeIn + ","
                    + (record.timeOut == null ? "" : record.timeOut.toString()));
        }
        saveLines(ATTENDANCE_FILE, lines);
    }

    private void saveLines(Path file, List<String> lines) throws IOException {
        Files.createDirectories(DATA_DIRECTORY);
        Path temporaryFile = Files.createTempFile(DATA_DIRECTORY, file.getFileName().toString(), ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            try {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(Locale.US, "PHP %,.2f", amount);
    }

    private static final class Employee {
        private final String id;
        private final String name;
        private final String position;
        private final BigDecimal dailyRate;

        private Employee(String id, String name, String position, BigDecimal dailyRate) {
            this.id = id.trim();
            this.name = name.trim();
            this.position = position.trim();
            this.dailyRate = dailyRate.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private static final class Attendance {
        private final String employeeId;
        private final LocalDateTime timeIn;
        private final LocalDateTime timeOut;

        private Attendance(String employeeId, LocalDateTime timeIn, LocalDateTime timeOut) {
            this.employeeId = employeeId;
            this.timeIn = timeIn;
            this.timeOut = timeOut;
        }
    }
}
