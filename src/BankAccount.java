import java.util.Objects;

public final class BankAccount {
    private final String owner;
    // Лимит одной исходящей операции в сотых долях валюты счёта.
    private final long withdrawalLimit;
    private Balance balance;
    private boolean blocked;

    public BankAccount(String owner, Balance balance, long withdrawalLimit) {
        Objects.requireNonNull(owner, "Владелец не задан");
        if (owner.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя владельца не может быть пустым");
        }
        if (withdrawalLimit < 0) {
            throw new IllegalArgumentException("Лимит не может быть отрицательным");
        }
        this.owner = owner.trim();
        this.balance = Objects.requireNonNull(balance, "Баланс не задан");
        this.withdrawalLimit = withdrawalLimit;
    }

    public String getOwner() { return owner; }
    public long getWithdrawalLimit() { return withdrawalLimit; }
    public boolean isBlocked() { return blocked; }

    public Balance getBalance() {
        // Это безопасно: сам объект Balance нельзя изменить.
        return balance;
    }

    public void block() { blocked = true; }
    public void unblock() { blocked = false; }

    public void deposit(long amount) {
        checkNotBlocked();
        checkPositiveAmount(amount);
        balance = balance.add(amount);
    }

    public void withdraw(long amount) {
        checkWithdrawal(amount);
        balance = balance.subtract(amount);
    }

    // amount задаётся в сотых долях валюты отправителя.
    public void transferTo(BankAccount recipient, long amount) {
        Objects.requireNonNull(recipient, "Получатель не задан");
        if (recipient == this) {
            throw new IllegalArgumentException("Нельзя переводить деньги на тот же счёт");
        }
        checkWithdrawal(amount);
        recipient.checkNotBlocked();

        Balance newSenderBalance = balance.subtract(amount);
        Balance converted = new Balance(amount, balance.getCurrency())
                .convertTo(recipient.balance.getCurrency());
        if (converted.getAmountInMinorUnits() == 0) {
            throw new IllegalArgumentException("Сумма слишком мала для перевода в эту валюту");
        }
        Balance newRecipientBalance = recipient.balance.add(converted.getAmountInMinorUnits());

        // Все проверки и расчёты завершены. Только теперь меняем оба счёта.
        // Если выше возникла ошибка, оба прежних баланса сохраняются.
        balance = newSenderBalance;
        recipient.balance = newRecipientBalance;
    }

    private void checkWithdrawal(long amount) {
        checkNotBlocked();
        checkPositiveAmount(amount);
        if (amount > withdrawalLimit) {
            throw new IllegalArgumentException("Превышен лимит одной операции");
        }
    }

    private void checkNotBlocked() {
        if (blocked) {
            throw new IllegalStateException("Счёт заблокирован");
        }
    }

    private static void checkPositiveAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма банковской операции должна быть больше нуля");
        }
    }

    @Override
    public String toString() {
        return "Владелец: " + owner + ", баланс: " + balance
                + ", лимит: " + new Balance(withdrawalLimit, balance.getCurrency())
                + ", заблокирован: " + blocked;
    }
}

