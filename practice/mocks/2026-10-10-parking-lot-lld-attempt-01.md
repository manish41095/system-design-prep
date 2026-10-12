# Parking Lot LLD — Mock 02 and Complete Revision

**Date:** 2026-10-10 (mock started); **revision updated:** 2026-10-12
**Target:** Schrödinger Senior Software Developer (Backend), Java 21
**Status:** Partial mock — only requirements answered independently. Sections on design, code, concurrency and edge cases are **reference material**, not independently answered or scored.

## 1. Interview prompt

Design a multi-level mall parking system with multiple entry/exit gates, Bike/Car/Truck vehicles, differently sized parking spots, nearest compatible allocation, ticketing, duration-based pricing, payment, release, and availability display. Focus on Java classes, extensibility, correctness under concurrent entry/exit and performance.

## 2. Question 1 — requirements

### Your answer (preserved)

> **Functional Requirements**
> - Support multiple floors, each with a configurable number of slots
> - Three vehicle types: Bike, Car, Truck — each requires a different slot size
> - Allocate the nearest available slot to an incoming vehicle
> - Issue a ticket on entry with timestamp and slot details
> - Calculate parking fee on exit based on duration and vehicle type
> - Free up the slot when a vehicle exits
> - Display available slot count per floor and vehicle type
>
> **Non-Functional Requirements**
> - Concurrent entry and exit must be handled safely — no double-allocation of slots
> - Adding a new vehicle type or pricing strategy must not require modifying existing classes (OCP)
> - Slot search should be O(1) or O(floors) using data structures, not a full O(n) scan

**Interviewer score:** 8.5/10.

**Missing clarification questions:**
1. Does a failed payment prevent exit and spot release? Here **yes**, except explicit authorized manual override.
2. Can the same plate have two active tickets? **No**, unique active parking session.
3. Can vehicles use larger spots? For this exercise **designated vehicle/spot type only**; a configurable compatibility policy can relax it.
4. Nearest relative to which entry gate? **Each gate**, based on travel distance.
5. Durability? **Persist ticket, spot occupancy and payments**.
6. Is spot reservation temporary before the car enters? Define a reservation TTL if the physical entry process is asynchronous.

**Improvements:** O(1) nearest-spot selection for arbitrary multi-gate proximity may be unrealistic; ordered indexes give fast candidates with update costs often O(log n).

## 3. Q2 — class design (model answer, NOT answered during mock)

| Type | Key fields / responsibilities |
|---|---|
| ParkingLot | lotId, floors, gates; top-level configuration |
| ParkingFloor | floorId, spots, availability summaries |
| ParkingSpot | spotId, floorId, type, status, gate distance/rank |
| Vehicle | plateNumber, vehicleType; prefer composition/type rather than subclass unless behavior differs |
| ParkingTicket | ticketId, plate, spotId, entryTime, exitTime, status |
| Gate | gateId, location, gateType; separate EntryGate and ExitGate only if behavior differs |
| ParkingService | coordinates entry and exit |
| SpotAllocationStrategy | chooses eligible nearest spot for gate |
| SpotRepository | atomic reservation / release |
| TicketRepository | durable ticket with active-session uniqueness |
| PricingStrategy | calculates price from session and tariff |
| PaymentService | processes payment idempotently; stores payment status |
| AvailabilityService | counts open spots by floor/type; derived/read-model data |

**Relationships:**
- ParkingLot **contains** Floors and references Gates.
- Floor **contains** Spots.
- Ticket **references** one Vehicle/plate and allocated Spot, not ownership of Spot.
- ParkingService **depends on interfaces** AllocationStrategy, PricingStrategy, repositories and PaymentGateway (DIP).
- Strategies are **composed**, not inherited by Ticket/Spot.
- Avoid a god-class ParkingLot implementing selection, payment, pricing and persistence.

### Class sketch

\`\`\`mermaid
classDiagram
 class ParkingService {
   +enter(Vehicle,GateId) ParkingTicket
   +exit(TicketId,PaymentRequest) Receipt
 }
 class ParkingLot
 class ParkingFloor
 class ParkingSpot {
   +UUID id
   +SpotType type
   +SpotStatus status
 }
 class Vehicle {
   +String plate
   +VehicleType type
 }
 class ParkingTicket {
   +UUID id
   +UUID spotId
   +Instant enteredAt
   +TicketStatus status
 }
 class SpotAllocationStrategy {
   <<interface>>
   +candidates(GateId,VehicleType) List~UUID~
 }
 class PricingStrategy {
   <<interface>>
   +calculate(ParkingTicket,Instant) Money
 }
 class SpotRepository {
   <<interface>>
   +tryReserve(SpotId,TicketId) boolean
   +release(SpotId,TicketId) boolean
 }
 class TicketRepository {
   <<interface>>
 }
 class PaymentGateway {
   <<interface>>
 }
 ParkingLot "1" *-- "*" ParkingFloor
 ParkingFloor "1" *-- "*" ParkingSpot
 ParkingTicket --> Vehicle
 ParkingTicket --> ParkingSpot
 ParkingService --> SpotAllocationStrategy
 ParkingService --> PricingStrategy
 ParkingService --> SpotRepository
 ParkingService --> TicketRepository
 ParkingService --> PaymentGateway
\`\`\`

## 4. Q3 — patterns and SOLID (reference)

- **Strategy:** \`SpotAllocationStrategy\` nearest-to-gate vs lowest-floor; \`PricingStrategy\` hourly, flat, weekend, vehicle-based.
- **Repository:** abstract persistence and atomic transitions.
- **Adapter:** wrap Cash/Card/UPI gateways under a common payment interface.
- **State:** optional encapsulation of ticket lifecycle; an enum plus transition service is sufficient for this scope.
- **SRP:** allocation selects, pricing computes, payment collects, ticket tracks session, service orchestrates.
- **OCP:** add a new strategy/vehicle compatibility rule through polymorphism or configuration; note that enums/switches can still require registration updates.
- **DIP:** high-level ParkingService depends on interfaces.
- **LSP/ISP:** implementations obey candidate/price contracts and interfaces are narrow.

## 5. Entry workflow and concurrency — Q4 model answer

1. Validate plate and ensure no active session already exists.
2. Get ordered eligible candidate spots **for the given gate**.
3. In a short DB transaction, atomically change the chosen spot FREE → RESERVED/OCCUPIED and create a ticket, with unique active-session constraint.
4. If another gate wins the reservation, try next candidate; do not issue two tickets.
5. Commit before confirming successful entry. If a physical barrier is involved, model OPEN_GATE and hardware failure recovery separately.
6. Keep authoritative occupancy in DB; caches and availability counters are derived.

**Important:** candidate selection alone is never an atomic reservation.

\`\`\`sql
UPDATE parking_spots
SET status='OCCUPIED', current_ticket_id=:ticketId
WHERE spot_id=:spotId AND status='FREE';
-- require affected rows = 1
\`\`\`

Make spot update and ticket insert one transaction. Example PostgreSQL safeguard for active tickets:

\`\`\`sql
CREATE UNIQUE INDEX one_active_ticket_per_plate
ON parking_tickets(plate_number)
WHERE status = 'ACTIVE';
\`\`\`

A relational database with row-level locking is a reasonable first implementation; Redis SETNX alone is not sufficient durable safety.

### Java service sketch (reference, schematic)

\`\`\`java
public record Vehicle(String plate, VehicleType type) {}
public enum VehicleType { BIKE, CAR, TRUCK }
public enum SpotType { BIKE, CAR, TRUCK }

public interface SpotAllocationStrategy {
    List<UUID> orderedCandidates(UUID entryGateId, VehicleType type);
}
public interface PricingStrategy {
    BigDecimal calculate(ParkingTicket ticket, Instant exitTime);
}
public interface SpotRepository {
    boolean occupyIfFree(UUID spotId, UUID ticketId);
    boolean releaseIfOwned(UUID spotId, UUID ticketId);
}
public interface TicketRepository {
    void insert(ParkingTicket ticket);
    Optional<ParkingTicket> findById(UUID ticketId);
}

public class ParkingService {
    private final SpotAllocationStrategy allocation;
    private final SpotRepository spots;
    private final TicketRepository tickets;
    // Inject collaborators through constructor.

    @Transactional
    public ParkingTicket enter(Vehicle vehicle, UUID gateId) {
        // Validate plate and rely on a DB unique active-ticket constraint.
        for (UUID spotId : allocation.orderedCandidates(gateId, vehicle.type())) {
            UUID ticketId = UUID.randomUUID();
            if (!spots.occupyIfFree(spotId, ticketId)) continue;
            ParkingTicket ticket =
                ParkingTicket.active(ticketId, vehicle, spotId, Instant.now());
            tickets.insert(ticket);  // Same DB transaction as spot occupancy.
            return ticket;
        }
        throw new NoParkingSpotAvailableException();
    }
}
\`\`\`

**Important refinement to this schematic:** In many JPA configurations each row update participates in the outer transaction, so competing transactions can wait; choose small batches and enforce uniqueness. For safe candidate iteration, use DB claim with \`FOR UPDATE SKIP LOCKED\` or conditional updates, and keep only the winning reservation. In production ensure a newly allocated ticket ID is not accidentally shared across retries, and choose a stable request idempotency key if gate retries an entry request. A transaction spanning payment-provider network calls is **not** recommended.

## 6. Nearest allocation — Q5 model answer

“Nearest” is gate-specific: distance(gate,spot). Precompute gate-to-spot distances when physical topology is relatively static. Maintain indexed FREE spots for each (gate,spotType), or query indexed eligible spots ordered by precomputed rank/distance, claiming first available in a short transaction.

- A TreeSet / indexed ordered structure gives O(log n) updates; head candidate is O(1) in memory but maintaining multiple-gate indexes requires coordination.
- A PriorityQueue alone cannot efficiently remove arbitrary occupied entries; lazy stale entries require validation and periodic compaction.
- Under contention candidate selection must retry with a new candidate and a max retry bound.
- Don't promise strict O(1) global nearest in a dynamically changing multi-gate lot without explaining the data structure.

## 7. Payment and exit flow — Q6 model answer

1. Look up active ticket; compute fee from entry time/exit time and vehicle policy.
2. Create a payment attempt with a unique idempotency key (ticketId + payment request id).
3. Call payment provider **outside** long-held DB locks; success response alone is not the sole trusted source if provider outcome is uncertain.
4. Persist payment outcome and exit/spot-release transition in a short transaction. Ensure a spot is released only once with \`WHERE current_ticket_id=:ticketId\`.
5. Issue receipt, mark ticket CLOSED and update availability (or reconcile derived count asynchronously).
6. If payment fails, keep ticket active and the spot occupied; if provider outcome is UNKNOWN, query/reconcile before charging again.

\`\`\`sql
UPDATE parking_spots SET status='FREE', current_ticket_id=NULL
WHERE spot_id=:spotId AND current_ticket_id=:ticketId AND status='OCCUPIED';
\`\`\`

Keep a durable payment state such as INITIATED/SUCCESS/FAILED/UNKNOWN and a separate exit-state transition. Repeated exit calls must return the same terminal receipt, not charge twice.

**Gate/hardware caveat:** Payment/spot release and physical car departure are not truly one transaction. If the barrier fails after payment, retain an exit-authorized state and retry hardware operation; physical sensor confirmation may determine final release.

## 8. Q7 — unit/integration tests (reference)

- Two gates concurrently request the only spot: exactly one ticket and occupancy.
- Same plate sends duplicate entry requests: only one active ticket.
- Different vehicle type asks incompatible spot: reject safely.
- Two requests exit one ticket concurrently: one payment/closure, one spot release.
- Payment failure: ticket remains active, spot remains occupied.
- Payment timeout after provider charged: idempotent reconciliation, no double charge.
- Crash after DB spot allocation before response: retry with entry request key returns original ticket.
- Clock boundary: partial-hour billing policy, free grace, overnight and timezone.
- Last spot on different floors; nearest-gate ranking tie.
- Availability counter drift: reconciliation restores correct result.
- Spot under maintenance must not be allocated.
- System restarts with active tickets: no loss.
- Lost ticket / manual override: auditable authorization and fee policy.

## 9. Missing scenarios and interviewer answers (unattempted)

**What if a car never parks after ticket issuance?** Model RESERVED with expiry and explicit physical occupancy confirmation; release only after safe reconciliation with sensors/gate state, not blindly on timer if a car is physically parked.

**What if vehicle leaves without paying?** Gate control normally prevents normal exit; manual overrides are privileged/audited and mark unpaid debt, not silent completed payment.

**How to support EV spots?** Add an attribute/capability plus \`SpotCompatibilityPolicy\` (vehicle required facilities, dimensions), not dozens of subclasses for every combination.

**Can Bike fit Car spot?** This mock assumes no; with flexible compatibility the strategy must assess type, dimensions, availability and business priority.

**How to handle full lot?** Return explicit NO_SPOT_AVAILABLE, don't issue a ticket, optionally offer waitlist/alternate floor.

**How to ensure availability counts?** DB is source of truth; maintain a materialized or cached count updated transactionally or via reliable events with periodic reconciliation. Near-real-time counters can be stale.

**How to support dynamic pricing?** Version tariff rules and store the charged tariff/reference on payment or ticket, so recalculation later doesn't silently change historical bills.

**How to scale to many floors/gates?** Index by spot type/status/gate rank, batch candidate search, short transactions; partition by lot/location if scale warrants. Avoid distributed coordination until needed.

**What if two ticket IDs are generated during concurrent retry?** Use a stable entry-request idempotency key and unique active-plate constraint; only commit the winning spot+ticket transaction.

**What if payment succeeds and DB fails?** Mark payment UNKNOWN until reconciliation; use gateway idempotency key to retrieve outcome and finalize exit without duplicate charge.

## 10. Interview timing and 2-minute summary

0–5: clarify types, gates, compatibility, payments, concurrency and nearest meaning.
5–15: ParkingLot/Floor/Spot/Ticket/Vehicle; service vs strategies vs repositories.
15–25: draw UML and interfaces.
25–40: implement atomic entry flow and payment/exit lifecycle.
40–50: concurrency races, duplicate entry/exit and recovery.
50–60: pricing extensibility, data structures, tests, tradeoffs.

> “I model ParkingLot with floors and spots, and every vehicle entry creates one durable active ticket associated with one occupied spot. I separate selection and pricing behind Strategy interfaces, with ParkingService orchestrating transactions through repositories. Since multiple gates act concurrently, choosing a candidate is separate from atomically reserving it; I require a conditional DB update and a unique active ticket per vehicle. For exit, I compute fees using a pricing strategy, process payments idempotently, and release the spot only after the appropriate paid or authorized-exit state. Duplicate requests, payment uncertainty, hardware failures and counter drift are handled through persisted states and reconciliation. Adding new vehicle capabilities, tariffs or allocation strategies should not change the core entry/exit flow.”

## 11. Assessment and next practice

**Scored:** Q1 requirements **8.5/10**. Q2+ were not answered in this conversation and have **no score**. This is a reference revision pack, not evidence of completing a whole independent Parking Lot LLD interview.

**Before interview:** explain class responsibilities without notes; write the \`enter()\` critical transaction; explain concurrent last-spot allocation; explain payment-success/DB-failure and retry; deliver a 2-minute summary.
