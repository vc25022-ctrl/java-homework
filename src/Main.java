public class Main {
    public static void main(String[] args) {
        System.out.println("=== Неизменяемость Balance ===");
        Balance original = new Balance(10_000); // 100 рублей
        Balance increased = original.add(5_000);
        Balance decreased = increased.subtract(2_000);
        System.out.println("Исходный: " + original);
        System.out.println("После add: " + increased);
        System.out.println("После subtract: " + decreased);

        System.out.println("\n=== Конвертация по курсам из задания ===");
        Balance rubles = new Balance(960_000); // 9 600 рублей
        System.out.println(rubles + " = " + rubles.convertTo(Currency.USD));
        System.out.println(rubles + " = " + rubles.convertTo(Currency.EUR));
        System.out.println(rubles + " = " + rubles.convertTo(Currency.CNY));

        System.out.println("\n=== Банковские операции ===");
        BankAccount vlad = new BankAccount("Чернов Владислав",
                new Balance(2_000_000), 1_000_000);
        BankAccount anna = new BankAccount("Анна",
                new Balance(0, Currency.USD), 50_000);
        BankAccount ivan = new BankAccount("Иван", new Balance(0), 100_000);
        System.out.println(vlad);
        vlad.deposit(100_000); // +1 000 рублей
        vlad.withdraw(50_000); // -500 рублей
        vlad.transferTo(anna, 960_000); // 9 600 рублей -> 100 долларов
        vlad.transferTo(ivan, 10_000); // 100 рублей -> 100 рублей
        System.out.println(vlad);
        System.out.println(anna);
        System.out.println(ivan);

        System.out.println("\n=== Ожидаемые ошибки ===");
        try {
            vlad.deposit(-100);
        } catch (IllegalArgumentException e) {
            System.out.println("Отрицательная сумма: " + e.getMessage());
        }
        try {
            vlad.transferTo(anna, 1_000_001);
        } catch (IllegalArgumentException e) {
            System.out.println("Лимит: " + e.getMessage());
        }
        try {
            ivan.withdraw(10_001);
        } catch (IllegalArgumentException e) {
            System.out.println("Нехватка денег: " + e.getMessage());
        }
        anna.block();
        try {
            vlad.transferTo(anna, 9_600);
        } catch (IllegalStateException e) {
            System.out.println("Блокировка получателя: " + e.getMessage());
        }
        anna.unblock();
        vlad.transferTo(anna, 9_600);
        System.out.println("\n=== Итог ===");
        System.out.println(vlad);
        System.out.println(anna);
        System.out.println(ivan);
    }
}
