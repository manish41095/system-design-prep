package lld.vendingmachine;

import java.util.EnumMap;
import java.util.Map;

public final class VendingMachineTest {
    public static void main(String[] args) {
        successfulPurchaseReturnsExactChange();
        cancellationReturnsOriginalTender();
        noExactChangeDoesNotConsumeProduct();
        dispenserFailureRollsBackAndRefunds();
        multipleProductsAreSupported();
        System.out.println("All vending-machine tests passed");
    }

    private static void successfulPurchaseReturnsExactChange() {
        VendingMachine machine = machine(items -> DispenseResult.ok(), defaultCash());
        machine.insert(Denomination.TWENTY_RUPEES);
        machine.select("A1", 1);
        PurchaseResult result = machine.checkout();
        check(result.success(), "purchase should succeed");
        check(value(result.change()) == 800, "change should be 8 rupees");
        check(machine.quantity("A1") == 2, "one product should be consumed");
        check(machine.stateName().equals("IDLE"), "machine should return to idle");
    }

    private static void cancellationReturnsOriginalTender() {
        VendingMachine machine = machine(items -> DispenseResult.ok(), defaultCash());
        machine.insert(Denomination.TEN_RUPEES);
        machine.insert(Denomination.FIVE_RUPEES);
        Map<Denomination, Integer> refund = machine.cancel();
        check(value(refund) == 1500, "refund should return inserted money");
        check(machine.stateName().equals("IDLE"), "cancel should return to idle");
    }

    private static void noExactChangeDoesNotConsumeProduct() {
        VendingMachine machine = machine(items -> DispenseResult.ok(), Map.of());
        machine.insert(Denomination.TWENTY_RUPEES);
        machine.select("A1", 1);
        try {
            machine.checkout();
            throw new AssertionError("checkout should fail without exact change");
        } catch (IllegalStateException expected) {
            check(machine.quantity("A1") == 3, "failed checkout must release product reservation");
        }
    }

    private static void dispenserFailureRollsBackAndRefunds() {
        VendingMachine machine = machine(items -> DispenseResult.failed("Coil jam"), defaultCash());
        machine.insert(Denomination.TWENTY_RUPEES);
        machine.select("A1", 1);
        PurchaseResult result = machine.checkout();
        check(!result.success(), "jammed purchase should fail");
        check(value(result.refund()) == 2000, "full inserted money should be refunded");
        check(machine.quantity("A1") == 3, "jam must not commit inventory");
        check(machine.stateName().equals("OUT_OF_SERVICE"), "jam should disable machine");
    }

    private static void multipleProductsAreSupported() {
        VendingMachine machine = machine(items -> DispenseResult.ok(), defaultCash());
        machine.insert(Denomination.FIFTY_RUPEES);
        machine.select("A1", 2);
        PurchaseResult result = machine.checkout();
        check(result.success(), "multi-item purchase should succeed");
        check(value(result.change()) == 2600, "change should be 26 rupees");
        check(machine.quantity("A1") == 1, "two products should be consumed");
    }

    private static VendingMachine machine(Dispenser dispenser, Map<Denomination, Integer> cash) {
        Inventory inventory = new Inventory();
        inventory.addSlot(new ProductSlot("A1",
                new Product("P1", "Water", Money.inr(1200)), 10, 3));
        return new VendingMachine("VM-1", inventory, new CashInventory(cash), dispenser);
    }

    private static Map<Denomination, Integer> defaultCash() {
        EnumMap<Denomination, Integer> cash = new EnumMap<>(Denomination.class);
        cash.put(Denomination.ONE_RUPEE, 10);
        cash.put(Denomination.TWO_RUPEES, 10);
        cash.put(Denomination.FIVE_RUPEES, 10);
        cash.put(Denomination.TEN_RUPEES, 10);
        cash.put(Denomination.TWENTY_RUPEES, 10);
        return cash;
    }

    private static long value(Map<Denomination, Integer> cash) {
        return cash.entrySet().stream()
                .mapToLong(e -> (long) e.getKey().paise() * e.getValue()).sum();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
