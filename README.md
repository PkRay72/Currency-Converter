# Currency-ConverterApp

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class CurrencyConverterApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final ExchangeRateService rateService =
            new ExchangeRateService();

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

    // ---------------- MENU ----------------

    private static void printMenu() {

        System.out.println();
        System.out.println("-----------------------------------------");
        System.out.println("1. Convert Currency");
        System.out.println("2. List Supported Currencies");
        System.out.println("3. Exit");
        System.out.println("-----------------------------------------");
        System.out.print("Choose an option: ");
    }

    // ---------------- MENU INPUT ----------------

    private static int readMenuChoice() {

        while (true) {

            try {

                int value = scanner.nextInt();

                // Consume the remaining newline
                scanner.nextLine();

                return value;

            } catch (InputMismatchException e) {

                System.out.println(
                        "Invalid input. Please enter a number."
                );

                // Clear invalid input
                scanner.nextLine();

                System.out.print("Choose an option: ");
            }
        }
    }

    // ---------------- LIST CURRENCIES ----------------

    private static void listSupportedCurrencies() {

        System.out.println();
        System.out.println("Supported currencies:");

        for (Map.Entry<String, BigDecimal> entry :
                rateService.getSupportedCurrencies().entrySet()) {

            System.out.printf(
                    "  %-5s (1 USD = %s %s)%n",
                    entry.getKey(),
                    entry.getValue(),
                    entry.getKey()
            );
        }
    }

    // ---------------- CONVERSION ----------------

    private static void performConversion() {

        String from = readCurrencyCode(
                "Enter source currency code (e.g., USD): "
        );

        String to = readCurrencyCode(
                "Enter target currency code (e.g., INR): "
        );

        BigDecimal amount = readAmount(
                "Enter amount to convert: "
        );

        try {

            BigDecimal result =
                    rateService.convert(amount, from, to);

            System.out.printf(
                    "Converted Amount: %,10.2f %s%n",
                    result,
                    to
            );

        } catch (IllegalArgumentException e) {

            System.out.println(
                    "Conversion failed: " + e.getMessage()
            );
        }
    }

    // ---------------- CURRENCY INPUT ----------------

    private static String readCurrencyCode(String prompt) {

        while (true) {

            System.out.print(prompt);

            String code = scanner.nextLine()
                    .trim()
                    .toUpperCase();

            if (rateService.isSupported(code)) {
                return code;
            }

            System.out.println(
                    "Unsupported currency code: " + code +
                    ". Use option 2 to see supported currencies."
            );
        }
    }

    // ---------------- AMOUNT INPUT ----------------

    private static BigDecimal readAmount(String prompt) {

        while (true) {

            System.out.print(prompt);

            String input = scanner.nextLine().trim();

            try {

                BigDecimal amount =
                        new BigDecimal(input);

                if (amount.signum() < 0) {

                    System.out.println(
                            "Please enter a valid number. " +
                            "Negative amounts are not allowed."
                    );

                    continue;
                }

                return amount;

            } catch (NumberFormatException e) {

                System.out.println(
                        "Please enter a valid number."
                );
            }
        }
    }
}


// ============================================================
// EXCHANGE RATE SERVICE
// ============================================================

class ExchangeRateService {

    // Working precision for intermediate calculations
    private static final int CALCULATION_SCALE = 10;

    private final Map<String, BigDecimal> ratesPerUsd =
            new LinkedHashMap<>();

    // Constructor
    public ExchangeRateService() {

        // Rates = units of currency for 1 USD

        ratesPerUsd.put("USD", new BigDecimal("1.0000"));
        ratesPerUsd.put("INR", new BigDecimal("83.4128"));
        ratesPerUsd.put("EUR", new BigDecimal("0.9254"));
        ratesPerUsd.put("GBP", new BigDecimal("0.7891"));
        ratesPerUsd.put("JPY", new BigDecimal("151.2300"));
        ratesPerUsd.put("AUD", new BigDecimal("1.5263"));
    }

    // Check whether currency is supported
    public boolean isSupported(String currencyCode) {

        return currencyCode != null
                && ratesPerUsd.containsKey(
                        currencyCode.toUpperCase()
                );
    }

    // Return all supported currencies
    public Map<String, BigDecimal> getSupportedCurrencies() {

        return ratesPerUsd;
    }

    // ---------------- CONVERSION LOGIC ----------------

    public BigDecimal convert(
            BigDecimal amount,
            String fromCurrency,
            String toCurrency) {

        // Check amount
        if (amount == null) {

            throw new IllegalArgumentException(
                    "Amount cannot be null."
            );
        }

        // Check negative amount
        if (amount.signum() < 0) {

            throw new IllegalArgumentException(
                    "Amount cannot be negative."
            );
        }

        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();

        // Check source currency
        if (!isSupported(from)) {

            throw new IllegalArgumentException(
                    "Unsupported source currency: " + from
            );
        }

        // Check target currency
        if (!isSupported(to)) {

            throw new IllegalArgumentException(
                    "Unsupported target currency: " + to
            );
        }

        // Same currency
        if (from.equals(to)) {

            return amount.setScale(
                    2,
                    RoundingMode.HALF_EVEN
            );
        }

        BigDecimal fromRate =
                ratesPerUsd.get(from);

        BigDecimal toRate =
                ratesPerUsd.get(to);

        // Step 1:
        // Source Currency -> USD

        BigDecimal amountInUsd =
                amount.divide(
                        fromRate,
                        CALCULATION_SCALE,
                        RoundingMode.HALF_EVEN
                );

        // Step 2:
        // USD -> Target Currency

        BigDecimal converted =
                amountInUsd.multiply(toRate);

        // Final result = exactly 2 decimal places

        return converted.setScale(
                2,
                RoundingMode.HALF_EVEN
        );
    }
}
