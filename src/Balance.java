import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

// Неизменяемый объект: операции возвращают новый Balance.
public final class Balance {
    // Сотые доли выбранной валюты: копейки, центы и т. д.
    private final long amountInMinorUnits;
    private final Currency currency;

    public Balance(long amountInKopecks) {
        this(amountInKopecks, Currency.RUB);
    }

    public Balance(long amountInMinorUnits, Currency currency) {
        if (amountInMinorUnits < 0) {
            throw new IllegalArgumentException("Баланс не может быть отрицательным");
        }
        this.currency = Objects.requireNonNull(currency, "Валюта не задана");
        this.amountInMinorUnits = amountInMinorUnits;
    }

    public long getAmountInMinorUnits() { return amountInMinorUnits; }
    public Currency getCurrency() { return currency; }

    public Balance add(long amount) {
        checkAmount(amount);
        return new Balance(Math.addExact(amountInMinorUnits, amount), currency);
    }

    public Balance subtract(long amount) {
        checkAmount(amount);
        if (amount > amountInMinorUnits) {
            throw new IllegalArgumentException("Недостаточно денег");
        }
        return new Balance(amountInMinorUnits - amount, currency);
    }

    public Balance convertTo(Currency targetCurrency) {
        Objects.requireNonNull(targetCurrency, "Валюта назначения не задана");
        // Новая сумма = старая сумма * курс исходной валюты / курс новой.
        // BigDecimal предотвращает ошибки double и переполнение произведения.
        // Результат снова хранится в long.
        long convertedAmount = BigDecimal.valueOf(amountInMinorUnits)
                .multiply(BigDecimal.valueOf(currency.getPriceInRublesKopecks()))
                .divide(BigDecimal.valueOf(targetCurrency.getPriceInRublesKopecks()),
                        0, RoundingMode.HALF_UP)
                .longValueExact();
        return new Balance(convertedAmount, targetCurrency);
    }

    private static void checkAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Сумма операции не может быть отрицательной");
        }
    }

    @Override
    public String toString() {
        long whole = amountInMinorUnits / 100;
        long fraction = amountInMinorUnits % 100;
        return whole + "." + (fraction < 10 ? "0" : "") + fraction
                + " " + currency.getCode() + " (" + currency.getSymbol() + ")";
    }
}
