# Parking Lot LLD — Mock 02 and Complete Revision

**Date:** 2026-10-10 (mock started); **revision updated:** 2026-10-12 (senior-level UML + deep dives)
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

### Detailed UML — entities, lifecycle, policies and infrastructure (reference)

Use this as a **logical LLD class diagram**. Interfaces are dependency boundaries; DB/adapter classes are implementations. The service orchestrates, while ticket/spot entities retain their invariants.

\`\`\`mermaid
classDiagram
direction LR
class ParkingLot {
  +UUID id
  +String name
}
class ParkingFloor {
  +UUID id
  +int floorNumber
}
class ParkingSpot {
  +UUID id
  +SpotType type
  +SpotStatus status
  +UUID currentTicketId
  +long version
}
class Gate {
  +UUID id
  +GateType type
}
class Vehicle {
  +String plateNumber
  +VehicleType type
}
class ParkingTicket {
  +UUID id
  +String plateNumber
  +UUID spotId
  +Instant enteredAt
  +TicketStatus status
  +long version
}
class Payment {
  +UUID id
  +UUID ticketId
  +String idempotencyKey
  +Money amount
  +PaymentStatus status
  +String providerReference
}
class Receipt {
  +UUID paymentId
  +UUID ticketId
  +Money paid
}
class ParkingService {
  +enter(Vehicle, UUID, String) ParkingTicket
  +requestExit(UUID, String) ExitQuote
  +finalizeExit(UUID, String) Receipt
}
class SpotAllocationStrategy {
  <<interface>>
  +rankedCandidates(UUID,VehicleType) List~UUID~
}
class NearestGateStrategy {
  +rankedCandidates(UUID,VehicleType) List~UUID~
}
class SpotCompatibilityPolicy {
  <<interface>>
  +isCompatible(VehicleType,SpotType) boolean
}
class PricingStrategy {
  <<interface>>
  +calculate(ParkingTicket,Instant) Money
}
class HourlyVehiclePricing {
  +calculate(ParkingTicket,Instant) Money
}
class SpotRepository {
  <<interface>>
  +occupyIfFree(UUID,UUID) boolean
  +releaseIfOwned(UUID,UUID) boolean
}
class TicketRepository {
  <<interface>>
  +findById(UUID) ParkingTicket
  +insert(ParkingTicket) void
}
class PaymentRepository {
  <<interface>>
  +findByKey(String) Payment
  +save(Payment) void
}
class PaymentGateway {
  <<interface>>
  +charge(String,Money) ProviderResult
  +lookup(String) ProviderResult
}
class EntryGateController {
  +admit(Vehicle,String) ParkingTicket
}
class ExitGateController {
  +exit(UUID,String) Receipt
}
class AvailabilityService {
  +getCounts(UUID,SpotType) long
}

ParkingLot "1" *-- "1..*" ParkingFloor : contains
ParkingFloor "1" *-- "1..*" ParkingSpot : contains
ParkingLot "1" o-- "1..*" Gate : serves
ParkingTicket "0..*" --> "1" ParkingSpot : assigned
ParkingTicket "0..*" --> "1" Vehicle : identifies
ParkingTicket "1" --> "0..*" Payment : attempts
Payment "0..1" --> "0..1" Receipt : settled
EntryGateController --> ParkingService
ExitGateController --> ParkingService
ParkingService --> SpotAllocationStrategy
ParkingService --> SpotCompatibilityPolicy
ParkingService --> PricingStrategy
ParkingService --> SpotRepository
ParkingService --> TicketRepository
ParkingService --> PaymentRepository
ParkingService --> PaymentGateway
AvailabilityService --> SpotRepository
SpotAllocationStrategy <|.. NearestGateStrategy
PricingStrategy <|.. HourlyVehiclePricing
\`\`\`

**Why these relationships:** ParkingLot/Floor composition represents structural ownership, while Ticket → Spot and Ticket → Vehicle are associations (a ticket does not own the shared spot). Payment is a separate aggregate to support multiple attempts and uncertain provider outcomes. Gates are edge adapters, not the owners of allocation/payment logic. The actual relational model stores foreign keys rather than serializing the entire object graph.

**Aggregate boundaries and invariants:**
- **Spot:** one active occupant at most; FREE → OCCUPIED → FREE using conditional write and \`currentTicketId\`.
- **Ticket:** one active ticket per vehicle per lot (partial unique index); ACTIVE → EXIT_AUTHORIZED → CLOSED. Optionally LOST/DISPUTED.
- **Payment:** INITIATED → SUCCEEDED/FAILED/UNKNOWN; never infer success from a timeout.
- A **DB transaction** may modify Spot + Ticket atomically for entry; payment network requests occur outside the DB transaction.
- The UML is intentionally more detailed than the 5-minute interview sketch. Start with 7–9 core entities, expand to adapters/persistence only when probed.

### Interview sketch — first 5 minutes

\`\`\`text
ParkingLot 1--* Floor 1--* Spot
Vehicle 1--* Ticket *--1 Spot
Ticket 1--* Payment
EntryGate / ExitGate -> ParkingService
ParkingService -> AllocationStrategy, PricingStrategy, Repositories, PaymentGateway
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

## 10. Senior-level interviewer deep dives — scenario, answer, trade-off and test (reference, NOT scored)

These are structured answers to give **after** presenting the basic design. A senior answer states the invariant, shows an atomic mechanism, names the failure window and explains the trade-off. Avoid merely saying “use synchronized” or “use Redis”.

### Q1. Two entry gates select the same last spot. Prove that double allocation is impossible.

**Answer:** Candidate search is advisory. The reservation decision happens in the database. Transaction T1 conditionally updates spot 42 from FREE to OCCUPIED, assigns its newly generated ticket ID, and inserts the ticket. Transaction T2's conflicting update cannot change that row from FREE and receives zero updated rows after T1 commits; it retries another candidate or returns FULL. A unique active-ticket-per-vehicle constraint independently prevents a second simultaneous entry for the same plate. Roll back both changes if ticket creation fails.

\`\`\`sql
BEGIN;
UPDATE parking_spots
   SET status='OCCUPIED', current_ticket_id=:ticketId,
       version=version+1
 WHERE id=:spotId AND status='FREE';
-- require exactly 1 updated row
INSERT INTO parking_tickets(id,lot_id,plate_number,spot_id,status,entered_at)
VALUES (:ticketId,:lotId,:plate,:spotId,'ACTIVE',now());
COMMIT;
\`\`\`

**Nuance:** Lock only the chosen rows and keep transaction short. Client timeout after commit requires an **entry request idempotency key** and a durable result lookup. A simple Java \`synchronized\` protects only one JVM and is insufficient across multiple gates/processes.

**Test:** 100 concurrent attempts for one spot → exactly one active ticket, one occupied spot, no orphan ticket.

### Q2. What if payment succeeds at UPI/card provider but our service times out?

**Answer:** A timeout is **UNKNOWN**, not FAILED. Create a persistent payment attempt with idempotency key before invoking the provider. Call provider with the same key on retry; reconcile using provider lookup/webhooks. If SUCCESS, mark payment succeeded once, authorize exit and follow through to release/receipt. If FAILED, retain active ticket/spot; if still UNKNOWN, do not initiate a new charge with a different idempotency key.

\`\`\`text
INITIATED -> provider charge -> UNKNOWN
                         |         |
                      success   lookup/webhook
                         |         |
                         +----> SUCCEEDED -> EXIT_AUTHORIZED -> CLOSED
\`\`\`

**Trade-off:** The user may need to wait for confirmation; no global transaction spans payment provider + our DB. Use provider reconciliation and reliable state transitions.

**Test:** Inject timeout **after provider charges**, replay same exit request → one provider payment, one receipt.

### Q3. How do we keep payment, ticket closure and spot release consistent?

**Answer:** Do not hold a DB transaction open while calling payment APIs. First settle/verify payment; then, in a short local DB transaction, transition the ticket from ACTIVE to EXIT_AUTHORIZED/CLOSED and conditionally release exactly the spot held by that ticket. Use DB uniqueness and state/version predicates to prevent double release. If the DB commit fails, reconciliation retries finalization using persisted provider payment reference/idempotency key. For a physical barrier, paid authorization and actual departure may be separate states.

**Invariant:** No release for an unpaid ordinary exit; no release of a different ticket's spot; at most one final receipt per successful payment/exit.

**Test:** Payment success → DB unavailable → recovery; ticket/spot eventually finalized without another payment.

### Q4. Two exit gates process the same ticket simultaneously: what happens?

**Answer:** Both may read ACTIVE, so initial reads alone are unsafe. Use conditional ticket transition (ACTIVE → EXIT_PROCESSING/EXIT_AUTHORIZED) and a unique payment idempotency key. One exit command wins; second receives existing status/result or observes in-progress payment. Avoid duplicate provider calls if possible and always send the same provider key on uncertain retries. A spot release requires the expected \`current_ticket_id\`.

**Test:** Concurrent duplicate exit calls → one payment outcome and one spot release.

### Q5. Is “nearest spot” really O(1)? Show the data structure and complexity.

**Answer:** It is nearest **to a specific gate**, not globally. Precompute distance/gate ranking per spot where layout is stable. Candidate retrieval can use a sorted index on \`(gate_id, spot_type, distance, spot_id)\` with an availability filter; maintain free-spot indexes or query a ranking table joined to current FREE spots. In-memory per-gate \`TreeSet\` gives O(log n) insert/remove and fast head selection, but updating all gate indexes is a trade-off. Atomic DB reservation is still mandatory.

**Trade-offs:** O(1) head access does not mean the full allocation is O(1); remote DB roundtrips, contention, index maintenance and fallbacks all matter. Favor an indexed DB-first implementation until scale proves a cache necessary.

### Q6. What is the aggregate boundary: ParkingLot, Spot, Ticket or Payment?

**Answer:** Avoid one enormous ParkingLot aggregate locking the entire mall. Spot is the small occupancy invariant, Ticket is the parking-session lifecycle, Payment is a separately persisted aggregate with provider retries. The entry application service coordinates a short local DB transaction across Spot/Ticket rows. This is pragmatic relational consistency, not a claim that every row is inside one DDD aggregate.

**Trade-off:** Smaller aggregates improve concurrency. Cross-aggregate workflows require explicit transactions/processes and reconciliation.

### Q7. The same car enters from two gates with the same plate. Redis lock or DB unique key?

**Answer:** **DB unique active-session constraint** is authoritative. A Redis lock may reduce duplicate work but expires, can split under failover and does not provide durable uniqueness. Capture \`entryRequestId\` as a unique idempotency key so client retries return the original ticket. The spot update and ticket insert must commit atomically.

**Test:** Same-plate requests race for two different spots → only one active ticket, only one spot remains occupied after rollback of losing transaction.

### Q8. What if the barrier fails after a successful payment?

**Answer:** Introduce separate \`EXIT_AUTHORIZED\` vs \`CLOSED/DEPARTED\` states. The system keeps a durable gate-open command with retry ID; attempts to open the barrier can be retried idempotently. Confirm actual departure via gate acknowledgment or sensor where available, then release the spot. Without physical confirmation, define a timeout/manual reconciliation policy rather than silently marking the vehicle departed. Hardware is outside the DB transaction.

### Q9. Availability says 1 FREE but allocation returns FULL. Is that a bug?

**Answer:** Not necessarily. Availability is often eventually consistent and may lag active reservations; only atomic reservation decides. Display the count as advisory and refresh after a conflict. For stricter counters, update them within the same DB transaction as occupancy or derive them periodically; cached views still need reconciliation.

### Q10. An existing Car is upgraded to an EV that needs charging; will OCP still hold?

**Answer:** Replace brittle “one enum ↔ one spot enum” logic with a \`SpotCompatibilityPolicy\` that checks dimensions, type and capabilities (\`CHARGING\`, \`ACCESSIBLE\`, etc.). New pricing/allocation policies plug in as strategies. Adding new capability data or configuration is expected; OCP doesn't prohibit all schema/config changes.

### Q11. How do we handle spot maintenance while a gate attempts allocation?

**Answer:** Model FREE, RESERVED/OCCUPIED and OUT_OF_SERVICE as distinct states. The conditional claim requires FREE, so maintenance marking and allocation race at the row. A spot currently occupied cannot be deactivated without a defined transition; maintenance request may become PENDING_MAINTENANCE until departure.

### Q12. Can an operator “just force close” a stuck ticket?

**Answer:** Provide privileged, audited overrides recording reason/operator/time and financial impact. Require explicit policy for missing payment, spot occupancy/sensor mismatch and refund. Never mutate a historical charge invisibly. An override should use the same conditional state transitions/idempotency protections as normal exit.

### Q13. What happens when a DB instance fails mid-entry?

**Answer:** If transaction did not commit, no ticket or occupancy change persists. If it committed but client didn't get response, the client reuses \`entryRequestId\` and fetches original ticket. If DB is unavailable, reject new admission rather than issuing a non-durable ticket unless an explicitly designed offline mode exists. Offline mode is a separate requirement with reconciliation and collision handling.

### Q14. How would you unit-test and integration-test this design?

**Answer:** Unit-test strategy selection/pricing using injected \`Clock\`, boundary durations, overnight stays and vehicle tariffs. Integration-test last-spot races and active-plate unique constraints against PostgreSQL (in-memory mocks cannot prove locking behavior). Add failure injection for provider charged-then-timeout, DB failure after payment, duplicate entry/exit, barrier timeout, availability drift and restart with active tickets.

### Q15. How should the solution scale if the mall grows to 100 floors and 30 gates?

**Answer:** Index candidate selection rather than scanning all spots; cache immutable gate-distance data, not authoritative occupancy; use per-lot/per-zone grouping only if contention measurements demand it; keep transactions short and maintain minimal indexes on hot occupancy rows. Profile p95 allocation latency, DB lock waits, conflicts, payment reconciliation age and available-spot counter drift. Start with one database transaction boundary before proposing sharding or distributed locks.

### Q16. What security and operations concerns would you mention?

**Answer:** Authenticate admin/gate devices, authorize overrides, do not trust plate/gate inputs blindly, encrypt sensitive payment references, use provider tokens instead of storing card credentials, retain audit trails for gate opening/refunds, add alerts for stale ACTIVE tickets, UNKNOWN payments and mismatched occupancy. Rate-limit duplicate gate attempts and reconcile external sensors.

### Q17. How would you explain inheritance vs composition here?

**Answer:** Use composition for tariff, allocation and compatibility behaviors because these vary independently. A \`VehicleType\` value suffices for Bike/Car/Truck if they only differ in data; create subclasses only when behavior really differs. EntryGate/ExitGate may share interface or Gate superclass, but gate controllers and ParkingService should not duplicate payment/allocation business rules.

### Q18. Which design choices are overengineering for a 45-minute LLD?

**Answer:** Don't introduce Kafka, service mesh, distributed locking and twenty patterns for a single-mall problem by default. Start with 7 core types, Strategy for selection/pricing, repository interfaces, and a transactional enter/exit flow. Add adapters, reconciliation, state machine and scalability only as deeper follow-ups. The strongest answer presents **correctness before infrastructure**.

### Interview-ready 90-second senior summary

> “My main invariants are one active ticket per vehicle and one occupant per spot. I would model ParkingLot, Floor, Spot, Vehicle and Ticket, while separating allocation and pricing as strategies. Entry first finds a ranked candidate for the chosen gate, but the spot is allocated only by a conditional database update in the same transaction as ticket creation. This prevents two gates from selling the last spot. On exit, pricing computes the charge and a durable idempotent payment flow handles provider timeouts. Once payment is confirmed, a short transaction authorizes exit and releases the spot at the appropriate physical-departure boundary. I would use DB uniqueness and state/version checks for duplicate requests, not Redis locks as the sole protection. The design is extensible for new vehicles and pricing policies, and I would test last-spot races, duplicate entry/exit, payment uncertainty and gate failures.”

---

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
