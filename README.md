# Currency-Converter

import java.math.BigDecimal;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;

/**
 * CurrencyConverterApp.java
 *
 * Project 4: The Financial Translation Engine.
 *
 * IPO Architecture:
 *   Intake     -> Scanner captures source currency, target currency, and
 *                 amount, guarded by a security gate (try-catch +
 *                 negative-amount rejection).
 *   Combustion -> ExchangeRateService performs BigDecimal cross-rate math.
 *   Exhaust    -> System.out.printf renders a strict two-decimal,
 *                 comma-grouped financial output.
 *
 * A do-while loop + switch statement (the "Switch Board") lets the user
 * perform multiple conversions without restarting the application.
 */
public class CurrencyConverterApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final ExchangeRateService rateService = new ExchangeRateService();

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   DECODELABS CURRENCY CONVERTER ENGINE");
        System.out.println("=========================================");

        int choice;
        do {
            printMenu();
            choice = readMenuChoice();

            switch (choice) {
                case 1:
                    performConversion();
                    break;
                case 2:
                    listSupportedCurrencies();
                    break;
                case 3:
                    System.out.println("Shutting down engine. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1-3.");
            }
        } while (choice != 3);

        scanner.close();
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("-----------------------------------------");
        System.out.println("1. Convert Currency");
        System.out.println("2. List Supported Currencies");
        System.out.println("3. Exit");
        System.out.println("-----------------------------------------");
        System.out.print("Choose an option: ");
    }

    /**
     * Reads the menu choice safely. Wraps the read in a try-catch to
     * survive InputMismatchException (the "Buffer Trap"), and always
     * calls scanner.nextLine() in the catch block to clear the stuck
     * token from the buffer — otherwise the loop would re-read the same
     * bad token forever.
     */
    private static int readMenuChoice() {
        while (true) {
            try {
                int value = scanner.nextInt();
                scanner.nextLine(); // consume trailing newline
                return value;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // clear the buffer
                System.out.print("Choose an option: ");
            }
        }
    }

    private static void listSupportedCurrencies() {
        System.out.println();
        System.out.println("Supported currencies:");
        for (Map.Entry<String, BigDecimal> entry : rateService.getSupportedCurrencies().entrySet()) {
            System.out.printf("  %-5s (1 USD = %s %s)%n",
                    entry.getKey(), entry.getValue(), entry.getKey());
        }
    }

    private static void performConversion() {
        String from = readCurrencyCode("Enter source currency code (e.g., USD): ");
        String to = readCurrencyCode("Enter target currency code (e.g., INR): ");
        BigDecimal amount = readAmount("Enter amount to convert: ");

        try {
            BigDecimal result = rateService.convert(amount, from, to);
            System.out.printf("Converted Amount: %,10.2f %s%n", result, to);
        } catch (IllegalArgumentException e) {
            System.out.println("Conversion failed: " + e.getMessage());
        }
    }

    /**
     * Reads and validates a currency code, looping until it matches a
     * supported currency.
     */
    private static String readCurrencyCode(String prompt) {
        while (true) {
            System.out.print(prompt);
            String code = scanner.nextLine().trim().toUpperCase();
            if (rateService.isSupported(code)) {
                return code;
            }
            System.out.println("Unsupported currency code: " + code
                    + ". Use option 2 to see supported currencies.");
        }
    }

    /**
     * The Security Gate: reads a monetary amount, guarding against both
     * malformed input (letters, symbols) and negative amounts. Loops
     * until valid, non-negative data is received rather than crashing
     * or producing nonsensical output.
     */
    private static BigDecimal readAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                BigDecimal amount = new BigDecimal(input);
                if (amount.signum() < 0) {
                    System.out.println("Please enter a valid number. Negative amounts are not allowed.");
                    continue;
                }
                return amount;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
