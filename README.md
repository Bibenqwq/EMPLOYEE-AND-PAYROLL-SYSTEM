# Employee Attendance and Payroll System

A Java console application that can be imported and run as an Eclipse Java project.

## Features

- Add employees with an ID, name, position, and daily rate.
- Record one time-in and time-out per employee per day.
- View attendance history, including shifts that have not been completed.
- Generate a payroll report for a date range.
- Calculate gross pay from completed attendance days and the employee's daily rate.
- Enter a deduction and calculate net pay.
- Save employee and attendance records in CSV files under the project's `data` folder.

Payroll reports are displayed in the console and are not saved as separate payslips. Incomplete shifts are excluded from paid days. This is a basic daily-rate example; configure payroll rules to match your organization's policies before using it for actual payroll.

## Run in Eclipse

1. Open Eclipse and choose **File > Import... > General > Existing Projects into Workspace**.
2. Select this project folder and finish the import.
3. Open `Admin.java`, then choose **Run As > Java Application**.
4. The `data` folder and its CSV files are created automatically when the first record is saved.

The project targets Java 8 or later and uses only standard Java libraries.

## Run from a terminal

From the project folder:

```text
javac Admin.java
java Admin
```
