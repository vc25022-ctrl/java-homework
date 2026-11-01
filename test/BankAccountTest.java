public class BankAccountTest {
    private static int checks;

    public static void main(String[] args) {
        Balance start = new Balance(10_000);
        Balance added = start.add(500);
        equal(10_000, start.getAmountInMinorUnits(), "Исходный баланс неизменен");
        equal(10_500, added.getAmountInMinorUnits(), "Сложение");
        equal(9_500, start.subtract(500).getAmountInMinorUnits(), "Вычитание");
        equal(0, start.subtract(10_000).getAmountInMinorUnits(), "Вычитание всего баланса");
        check(start != start.add(0), "add возвращает новый объект");
        check(start != start.subtract(0), "subtract возвращает новый объект");
        check(start != start.convertTo(Currency.RUB), "convertTo возвращает новый объект");
        check(start.getCurrency() == Currency.RUB, "Валюта по умолчанию");
        check(new Balance(5).toString().equals("0.05 RUB (₽)"), "Формат суммы");
        fails(IllegalArgumentException.class, () -> new Balance(-1));
        fails(NullPointerException.class, () -> new Balance(0, null));
        fails(NullPointerException.class, () -> start.convertTo(null));
        fails(IllegalArgumentException.class, () -> start.add(-1));
        fails(IllegalArgumentException.class, () -> start.subtract(-1));
        fails(IllegalArgumentException.class, () -> start.subtract(10_001));
        fails(ArithmeticException.class, () -> new Balance(Long.MAX_VALUE).add(1));

        equal(10_000, new Balance(960_000).convertTo(Currency.USD).getAmountInMinorUnits(), "RUB -> USD");
        equal(960_000, new Balance(10_000, Currency.USD).convertTo(Currency.RUB).getAmountInMinorUnits(), "USD -> RUB");
        equal(100, new Balance(10_300).convertTo(Currency.EUR).getAmountInMinorUnits(), "RUB -> EUR");
        equal(100, new Balance(1_320).convertTo(Currency.CNY).getAmountInMinorUnits(), "RUB -> CNY");
        equal(107, new Balance(100, Currency.EUR).convertTo(Currency.USD).getAmountInMinorUnits(), "EUR -> USD с округлением");
        equal(0, new Balance(47).convertTo(Currency.USD).getAmountInMinorUnits(), "Округление вниз");
        equal(1, new Balance(48).convertTo(Currency.USD).getAmountInMinorUnits(), "Ровно половина вверх");
        equal(Long.MAX_VALUE, new Balance(Long.MAX_VALUE).convertTo(Currency.RUB).getAmountInMinorUnits(), "Нет промежуточного переполнения");
        fails(ArithmeticException.class, () -> new Balance(Long.MAX_VALUE, Currency.USD).convertTo(Currency.RUB));
        for (Currency currency : Currency.values()) {
            Balance converted = new Balance(0).convertTo(currency);
            equal(0, converted.getAmountInMinorUnits(), "Нулевой баланс");
            check(converted.getCurrency() == currency, "Сохраняется целевая валюта");
        }

        fails(NullPointerException.class, () -> new BankAccount(null, start, 100));
        fails(NullPointerException.class, () -> new BankAccount("A", null, 100));
        fails(IllegalArgumentException.class, () -> new BankAccount("  ", start, 100));
        fails(IllegalArgumentException.class, () -> new BankAccount("A", start, -1));
        BankAccount sender = new BankAccount(" A ", new Balance(1_000), 500);
        BankAccount receiver = new BankAccount("B", new Balance(0), 500);
        check(sender.getOwner().equals("A"), "Имя владельца");
        equal(500, sender.getWithdrawalLimit(), "Лимит");
        sender.deposit(100);
        sender.withdraw(100);
        equal(1_000, sender.getBalance().getAmountInMinorUnits(), "Пополнение и снятие");
        Balance snapshot = sender.getBalance();
        sender.transferTo(receiver, 500);
        equal(500, sender.getBalance().getAmountInMinorUnits(), "Перевод на границе лимита");
        equal(500, receiver.getBalance().getAmountInMinorUnits(), "Зачисление перевода");
        equal(1_000, snapshot.getAmountInMinorUnits(), "Старый Balance не изменился");
        fails(IllegalArgumentException.class, () -> sender.deposit(0));
        fails(IllegalArgumentException.class, () -> sender.deposit(-1));
        fails(IllegalArgumentException.class, () -> sender.withdraw(0));
        fails(IllegalArgumentException.class, () -> sender.withdraw(-1));
        fails(IllegalArgumentException.class, () -> sender.withdraw(501));
        transferFails(IllegalArgumentException.class, sender, receiver, 501);
        transferFails(IllegalArgumentException.class, sender, receiver, 0);
        transferFails(IllegalArgumentException.class, sender, receiver, -1);
        transferFails(IllegalArgumentException.class, sender, sender, 1);
        fails(NullPointerException.class, () -> sender.transferTo(null, 1));

        receiver.block();
        check(receiver.isBlocked(), "Блокировка включена");
        transferFails(IllegalStateException.class, sender, receiver, 1);
        fails(IllegalStateException.class, () -> receiver.deposit(1));
        fails(IllegalStateException.class, () -> receiver.withdraw(1));
        transferFails(IllegalStateException.class, receiver, sender, 1);
        receiver.unblock();
        check(!receiver.isBlocked(), "Блокировка выключена");
        sender.transferTo(receiver, 1);
        equal(501, receiver.getBalance().getAmountInMinorUnits(), "Перевод после разблокировки");

        BankAccount empty = new BankAccount("Empty", new Balance(0), 100);
        transferFails(IllegalArgumentException.class, empty, receiver, 1);
        fails(IllegalArgumentException.class, () -> empty.withdraw(1));
        BankAccount noWithdrawals = new BankAccount("Zero limit", new Balance(100), 0);
        fails(IllegalArgumentException.class, () -> noWithdrawals.withdraw(1));
        transferFails(IllegalArgumentException.class, noWithdrawals, receiver, 1);

        BankAccount full = new BankAccount("Full", new Balance(Long.MAX_VALUE), Long.MAX_VALUE);
        Balance beforeOverflow = full.getBalance();
        fails(ArithmeticException.class, () -> full.deposit(1));
        check(full.getBalance() == beforeOverflow, "Переполнение пополнения не меняет баланс");
        transferFails(ArithmeticException.class, sender, full, 1);

        BankAccount rub = new BankAccount("RUB", new Balance(960_000), 960_000);
        BankAccount usd = new BankAccount("USD", new Balance(0, Currency.USD), Long.MAX_VALUE);
        transferFails(IllegalArgumentException.class, rub, usd, 1);
        rub.transferTo(usd, 960_000);
        equal(0, rub.getBalance().getAmountInMinorUnits(), "Списание всего остатка");
        equal(10_000, usd.getBalance().getAmountInMinorUnits(), "Валютный перевод");
        check(usd.getBalance().getCurrency() == Currency.USD, "Валюта счёта сохраняется");
        BankAccount hugeUsd = new BankAccount("Huge", new Balance(Long.MAX_VALUE, Currency.USD), Long.MAX_VALUE);
        transferFails(ArithmeticException.class, hugeUsd, rub, Long.MAX_VALUE);
        System.out.println("Все проверки пройдены: " + checks);
    }

    private static void transferFails(Class<? extends Throwable> type,
                                      BankAccount sender, BankAccount receiver, long amount) {
        Balance beforeSender = sender.getBalance();
        Balance beforeReceiver = receiver.getBalance();
        fails(type, () -> sender.transferTo(receiver, amount));
        check(sender.getBalance() == beforeSender, "При ошибке баланс отправителя не меняется");
        check(receiver.getBalance() == beforeReceiver, "При ошибке баланс получателя не меняется");
    }

    private static void equal(long expected, long actual, String message) {
        check(expected == actual, message + ": ожидалось " + expected + ", получено " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
        checks++;
    }

    private static void fails(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            if (!expected.isInstance(error)) {
                throw new AssertionError("Ожидалось " + expected.getSimpleName(), error);
            }
            checks++;
            return;
        }
        throw new AssertionError("Ожидалось исключение " + expected.getSimpleName());
    }
}
