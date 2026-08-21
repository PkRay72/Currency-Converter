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
