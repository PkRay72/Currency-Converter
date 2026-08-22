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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ExchangeRateService.java
 *
 * The "Combustion Engine": performs mathematical translation and
 * precision scaling. Holds a central pivot exchange-rate table so any
 * currency pair can be converted without hardcoding every possible
 * combination (Cross-Rate Routing).
 *
 * All rates are stored as "units of currency per 1 USD" (the pivot
 * currency). Converting EUR -> INR therefore routes through an
 * intermediate USD value: EUR -> USD -> INR.
 *
 * Uses BigDecimal exclusively. A native double (e.g. 0.1 + 0.2) cannot
 * represent most base-10 decimals exactly in binary floating point,
 * which is unacceptable when the data represents money.
 */
public class ExchangeRateService {

    // Working precision for intermediate math, rounded down to 2 decimals
    // only at the very end (final financial polish).
    private static final int CALCULATION_SCALE = 10;

    private final Map<String, BigDecimal> ratesPerUsd = new LinkedHashMap<>();

    public ExchangeRateService() {
        // Predefined exchange rates: units of currency per 1 USD.
        ratesPerUsd.put("USD", new BigDecimal("1.0000"));
        ratesPerUsd.put("INR", new BigDecimal("83.4128"));
        ratesPerUsd.put("EUR", new BigDecimal("0.9254"));
        ratesPerUsd.put("GBP", new BigDecimal("0.7891"));
        ratesPerUsd.put("JPY", new BigDecimal("151.2300"));
        ratesPerUsd.put("AUD", new BigDecimal("1.5263"));
    }

    public boolean isSupported(String currencyCode) {
        return currencyCode != null && ratesPerUsd.containsKey(currencyCode.toUpperCase());
    }

    public Map<String, BigDecimal> getSupportedCurrencies() {
        return ratesPerUsd;
    }

    /**
     * Converts an amount from one currency to another via USD cross-rate
     * routing, then applies the enterprise rounding standard
     * (RoundingMode.HALF_EVEN, a.k.a. Banker's Rounding) to exactly two
     * decimal places. HALF_EVEN eliminates the cumulative rounding bias
     * that RoundingMode.HALF_UP introduces across millions of transactions.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null.");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }

        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();

        if (!isSupported(from)) {
            throw new IllegalArgumentException("Unsupported source currency: " + from);
        }
        if (!isSupported(to)) {
            throw new IllegalArgumentException("Unsupported target currency: " + to);
        }

        if (from.equals(to)) {
            return amount.setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal fromRate = ratesPerUsd.get(from);
        BigDecimal toRate = ratesPerUsd.get(to);

        // Step 1: source -> USD (the intermediate/pivot value)
        BigDecimal amountInUsd = amount.divide(fromRate, CALCULATION_SCALE, RoundingMode.HALF_EVEN);

        // Step 2: USD -> target (the final value)
        BigDecimal converted = amountInUsd.multiply(toRate);

        // Final financial polish: exactly two decimal places, Banker's Rounding.
        return converted.setScale(2, RoundingMode.HALF_EVEN);
    }
}
