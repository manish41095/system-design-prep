package lld.vendingmachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public final class VendingMachine {
    private final String machineId;
    private final Inventory inventory;
    private final CashInventory cashInventory;
    private final ChangeCalculator changeCalculator;
    private final Dispenser dispenser;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final MachineContext context = new Context();

    private MachineState state = IdleState.INSTANCE;
    private PurchaseSession session;

    public VendingMachine(String machineId, Inventory inventory,
                          CashInventory cashInventory, Dispenser dispenser) {
        this.machineId = Objects.requireNonNull(machineId);
        this.inventory = Objects.requireNonNull(inventory);
        this.cashInventory = Objects.requireNonNull(cashInventory);
        this.dispenser = Objects.requireNonNull(dispenser);
        this.changeCalculator = new ExactChangeCalculator();
    }

    public void insert(Denomination denomination) {
        execute(() -> state.insert(context, denomination));
    }

    public void select(String slotCode, int quantity) {
        execute(() -> state.select(context, slotCode, quantity));
    }

    public PurchaseResult checkout() {
        return executeWithResult(() -> state.checkout(context));
    }

    public Map<Denomination, Integer> cancel() {
        return executeWithResult(() -> state.cancel(context));
    }

    public void restock(String slotCode, int quantity) {
        execute(() -> {
            if (state != IdleState.INSTANCE) {
                throw new IllegalStateException("Restock is allowed only while idle");
            }
            inventory.restock(slotCode, quantity);
        });
    }

    public String stateName() {
        return executeWithResult(state::name);
    }

    public int quantity(String slotCode) {
        return executeWithResult(() -> inventory.quantity(slotCode));
    }

    public String machineId() {
        return machineId;
    }

    private void execute(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    private <T> T executeWithResult(SupplierWithException<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private final class Context implements MachineContext {
        @Override
        public void startSession() {
            if (session == null) session = new PurchaseSession();
        }

        @Override
        public void addTender(Denomination denomination) {
            session.addTender(denomination);
        }

        @Override
        public void addSelection(String slotCode, int quantity) {
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
            inventory.requireAvailable(slotCode, quantity);
            session.addSelection(slotCode, quantity);
        }

        @Override
        public PurchaseResult completePurchase() {
            if (session == null || session.selections().isEmpty()) {
                throw new IllegalStateException("Select at least one product");
            }

            long price = inventory.totalPrice(session.selections());
            long inserted = session.insertedPaise();
            if (inserted < price) {
                throw new IllegalStateException("Insufficient balance: need " + (price - inserted));
            }

            inventory.reserve(session.selections());
            long changeAmount = inserted - price;
            Map<Denomination, Integer> available = cashInventory.previewAfterDeposit(session.escrow());
            Optional<Map<Denomination, Integer>> change =
                    changeCalculator.calculate(changeAmount, available);
            if (change.isEmpty()) {
                inventory.release(session.selections());
                throw new IllegalStateException("Exact change is not available");
            }

            state = DispensingState.INSTANCE;
            DispenseResult result = dispenser.dispense(inventory.dispenseItems(session.selections()));
            if (!result.success()) {
                inventory.release(session.selections());
                Map<Denomination, Integer> refund = session.refund();
                session = null;
                state = OutOfServiceState.INSTANCE;
                return PurchaseResult.failed(result.message(), refund);
            }

            inventory.commit(session.selections());
            cashInventory.deposit(session.escrow());
            cashInventory.withdraw(change.get());
            Map<Denomination, Integer> returnedChange = change.get();
            session = null;
            state = IdleState.INSTANCE;
            return PurchaseResult.success(returnedChange);
        }

        @Override
        public Map<Denomination, Integer> refundAndReset() {
            if (session == null) return Map.of();
            Map<Denomination, Integer> refund = session.refund();
            session = null;
            state = IdleState.INSTANCE;
            return refund;
        }

        @Override
        public void transitionTo(MachineState newState) {
            state = Objects.requireNonNull(newState);
        }
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get();
    }
}

record Money(long paise, Currency currency) {
    Money {
        if (paise < 0) throw new IllegalArgumentException("Money cannot be negative");
        Objects.requireNonNull(currency);
    }

    static Money inr(long paise) {
        return new Money(paise, Currency.getInstance("INR"));
    }
}

enum Denomination {
    ONE_RUPEE(100), TWO_RUPEES(200), FIVE_RUPEES(500),
    TEN_RUPEES(1_000), TWENTY_RUPEES(2_000), FIFTY_RUPEES(5_000);

    private final int paise;

    Denomination(int paise) { this.paise = paise; }
    int paise() { return paise; }
}

record Product(String id, String name, Money price) {
    Product {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(price);
    }
}

final class ProductSlot {
    private final String code;
    private final Product product;
    private final int capacity;
    private int quantity;
    private int reserved;

    ProductSlot(String code, Product product, int capacity, int quantity) {
        if (capacity <= 0 || quantity < 0 || quantity > capacity) {
            throw new IllegalArgumentException("Invalid capacity or quantity");
        }
        this.code = Objects.requireNonNull(code);
        this.product = Objects.requireNonNull(product);
        this.capacity = capacity;
        this.quantity = quantity;
    }

    String code() { return code; }
    Product product() { return product; }
    int quantity() { return quantity; }
    int available() { return quantity - reserved; }

    void reserve(int count) {
        if (count <= 0 || available() < count) throw new IllegalStateException("Out of stock: " + code);
        reserved += count;
    }

    void commit(int count) {
        if (count <= 0 || reserved < count) throw new IllegalStateException("Invalid reservation");
        reserved -= count;
        quantity -= count;
    }

    void release(int count) {
        if (count <= 0 || reserved < count) throw new IllegalStateException("Invalid reservation");
        reserved -= count;
    }

    void restock(int count) {
        if (count <= 0 || quantity + count > capacity) throw new IllegalArgumentException("Slot capacity exceeded");
        quantity += count;
    }
}

final class Inventory {
    private final Map<String, ProductSlot> slots = new LinkedHashMap<>();

    void addSlot(ProductSlot slot) {
        if (slots.putIfAbsent(slot.code(), slot) != null) throw new IllegalArgumentException("Duplicate slot");
    }

    void requireAvailable(String code, int quantity) {
        ProductSlot slot = slot(code);
        if (slot.available() < quantity) throw new IllegalStateException("Out of stock: " + code);
    }

    long totalPrice(Map<String, Integer> selections) {
        long total = 0;
        for (Map.Entry<String, Integer> entry : selections.entrySet()) {
            long unit = slot(entry.getKey()).product().price().paise();
            total = Math.addExact(total, Math.multiplyExact(unit, entry.getValue()));
        }
        return total;
    }

    void reserve(Map<String, Integer> selections) {
        List<Map.Entry<String, Integer>> completed = new ArrayList<>();
        try {
            for (Map.Entry<String, Integer> entry : selections.entrySet()) {
                slot(entry.getKey()).reserve(entry.getValue());
                completed.add(entry);
            }
        } catch (RuntimeException ex) {
            for (Map.Entry<String, Integer> entry : completed) slot(entry.getKey()).release(entry.getValue());
            throw ex;
        }
    }

    void commit(Map<String, Integer> selections) {
        selections.forEach((code, count) -> slot(code).commit(count));
    }

    void release(Map<String, Integer> selections) {
        selections.forEach((code, count) -> slot(code).release(count));
    }

    List<DispenseItem> dispenseItems(Map<String, Integer> selections) {
        List<DispenseItem> items = new ArrayList<>();
        selections.forEach((code, count) -> items.add(new DispenseItem(code, count)));
        return List.copyOf(items);
    }

    void restock(String code, int count) { slot(code).restock(count); }
    int quantity(String code) { return slot(code).quantity(); }

    private ProductSlot slot(String code) {
        ProductSlot slot = slots.get(code);
        if (slot == null) throw new IllegalArgumentException("Unknown slot: " + code);
        return slot;
    }
}

final class PurchaseSession {
    private final EnumMap<Denomination, Integer> escrow = new EnumMap<>(Denomination.class);
    private final Map<String, Integer> selections = new LinkedHashMap<>();

    void addTender(Denomination denomination) { escrow.merge(denomination, 1, Integer::sum); }
    void addSelection(String code, int quantity) { selections.merge(code, quantity, Integer::sum); }

    long insertedPaise() {
        return escrow.entrySet().stream()
                .mapToLong(e -> (long) e.getKey().paise() * e.getValue()).sum();
    }

    Map<Denomination, Integer> escrow() { return Collections.unmodifiableMap(escrow); }
    Map<String, Integer> selections() { return Collections.unmodifiableMap(selections); }
    Map<Denomination, Integer> refund() { return Collections.unmodifiableMap(new EnumMap<>(escrow)); }
}

final class CashInventory {
    private final EnumMap<Denomination, Integer> counts = new EnumMap<>(Denomination.class);

    CashInventory(Map<Denomination, Integer> initial) { deposit(initial); }

    Map<Denomination, Integer> previewAfterDeposit(Map<Denomination, Integer> incoming) {
        EnumMap<Denomination, Integer> preview = new EnumMap<>(counts);
        incoming.forEach((d, c) -> preview.merge(d, c, Integer::sum));
        return preview;
    }

    void deposit(Map<Denomination, Integer> incoming) {
        incoming.forEach((d, c) -> {
            if (c < 0) throw new IllegalArgumentException("Negative cash count");
            counts.merge(d, c, Integer::sum);
        });
    }

    void withdraw(Map<Denomination, Integer> outgoing) {
        outgoing.forEach((d, c) -> {
            if (counts.getOrDefault(d, 0) < c) throw new IllegalStateException("Cash inventory changed");
        });
        outgoing.forEach((d, c) -> counts.merge(d, -c, Integer::sum));
    }
}

interface ChangeCalculator {
    Optional<Map<Denomination, Integer>> calculate(long amountPaise,
                                                    Map<Denomination, Integer> available);
}

final class ExactChangeCalculator implements ChangeCalculator {
    @Override
    public Optional<Map<Denomination, Integer>> calculate(long amount,
                                                           Map<Denomination, Integer> available) {
        if (amount < 0) throw new IllegalArgumentException("Negative change");
        List<Denomination> values = new ArrayList<>(List.of(Denomination.values()));
        values.sort(Comparator.comparingInt(Denomination::paise).reversed());
        EnumMap<Denomination, Integer> answer = new EnumMap<>(Denomination.class);
        return search(values, 0, amount, available, answer)
                ? Optional.of(Collections.unmodifiableMap(new EnumMap<>(answer)))
                : Optional.empty();
    }

    private boolean search(List<Denomination> values, int index, long remaining,
                           Map<Denomination, Integer> available,
                           EnumMap<Denomination, Integer> answer) {
        if (remaining == 0) return true;
        if (index == values.size()) return false;

        Denomination denomination = values.get(index);
        int max = (int) Math.min(available.getOrDefault(denomination, 0),
                                 remaining / denomination.paise());
        for (int count = max; count >= 0; count--) {
            if (count == 0) answer.remove(denomination); else answer.put(denomination, count);
            if (search(values, index + 1,
                       remaining - (long) count * denomination.paise(), available, answer)) return true;
        }
        answer.remove(denomination);
        return false;
    }
}

interface Dispenser {
    DispenseResult dispense(List<DispenseItem> items);
}

record DispenseItem(String slotCode, int quantity) {}
record DispenseResult(boolean success, String message) {
    static DispenseResult ok() { return new DispenseResult(true, "Dispensed"); }
    static DispenseResult failed(String message) { return new DispenseResult(false, message); }
}

record PurchaseResult(boolean success, String message,
                      Map<Denomination, Integer> change,
                      Map<Denomination, Integer> refund) {
    static PurchaseResult success(Map<Denomination, Integer> change) {
        return new PurchaseResult(true, "Purchase complete", Map.copyOf(change), Map.of());
    }
    static PurchaseResult failed(String message, Map<Denomination, Integer> refund) {
        return new PurchaseResult(false, message, Map.of(), Map.copyOf(refund));
    }
}

interface MachineContext {
    void startSession();
    void addTender(Denomination denomination);
    void addSelection(String slotCode, int quantity);
    PurchaseResult completePurchase();
    Map<Denomination, Integer> refundAndReset();
    void transitionTo(MachineState state);
}

interface MachineState {
    String name();
    default void insert(MachineContext context, Denomination denomination) { throw invalid("insert money"); }
    default void select(MachineContext context, String slotCode, int quantity) { throw invalid("select product"); }
    default PurchaseResult checkout(MachineContext context) { throw invalid("checkout"); }
    default Map<Denomination, Integer> cancel(MachineContext context) { throw invalid("cancel"); }
    private IllegalStateException invalid(String operation) {
        return new IllegalStateException(operation + " is invalid in " + name());
    }
}

enum IdleState implements MachineState {
    INSTANCE;
    public String name() { return "IDLE"; }
    public void insert(MachineContext context, Denomination denomination) {
        context.startSession();
        context.addTender(denomination);
        context.transitionTo(HasMoneyState.INSTANCE);
    }
}

enum HasMoneyState implements MachineState {
    INSTANCE;
    public String name() { return "HAS_MONEY"; }
    public void insert(MachineContext context, Denomination denomination) { context.addTender(denomination); }
    public void select(MachineContext context, String slotCode, int quantity) {
        context.addSelection(slotCode, quantity);
    }
    public PurchaseResult checkout(MachineContext context) { return context.completePurchase(); }
    public Map<Denomination, Integer> cancel(MachineContext context) { return context.refundAndReset(); }
}

enum DispensingState implements MachineState {
    INSTANCE;
    public String name() { return "DISPENSING"; }
}

enum OutOfServiceState implements MachineState {
    INSTANCE;
    public String name() { return "OUT_OF_SERVICE"; }
}
