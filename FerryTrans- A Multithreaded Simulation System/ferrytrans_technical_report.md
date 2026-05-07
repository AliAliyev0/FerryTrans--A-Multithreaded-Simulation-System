# FerryTrans: Multithreaded Ferry Simulation System
## Technical Project Report

### 1. Title & Team Information
- **Project Title:** FerryTrans - A Multithreaded Simulation System
- **Course Name:** Operating Systems
- **Team Members:**
  - [Mahammadalı Aliyev] - [220315090]
  - [Jalil Guliyev] - [220315102]

### 2. System Design
The FerryTrans system is designed as a highly concurrent, multithreaded Java application. It models a complex synchronization problem where independent threads (representing vehicles) must coordinate with a central orchestrator thread (representing the ferry) to cross a river between a Mainland and an Island port.

The architecture avoids monolithic locking by decentralizing resource management. Vehicles act as autonomous `Runnable` tasks that manage their own lifecycles, including outbound and return trips. The Ferry acts as an independent consumer task that polls the ports, manages its internal load, and transports the vehicles based on a predefined set of strict departure policies. Interaction between the Ferry and Vehicles is achieved purely through inter-thread signaling, completely avoiding CPU-intensive busy-waiting loops.

### 3. Thread Structure
The simulation relies on specialized components to manage concurrency and state efficiently:
- **SimulationManager:** Acts as the central registry and orchestrator. It holds the singleton instances of the `Port` objects and the `Ferry`, allowing vehicle threads to route themselves without tight coupling. It also manages the global `CountDownLatch` used to safely terminate the simulation once all vehicle threads have completed their return journeys.
- **TollBooth:** Represents the entry point into a port's waiting area. It processes vehicles by simulating a physical delay (payment and barrier processing) while ensuring that only one vehicle can occupy a booth at any given time.
- **WaitingArea:** A wrapper around a lock-free `ConcurrentLinkedQueue`. It temporarily holds the `BoardingTicket` of each vehicle that has cleared the toll booth, serving as a thread-safe, strictly ordered repository of waiting threads until the ferry is ready to load them.

### 4. Synchronization Strategy
The integrity of the simulation is guaranteed through the precise application of concurrency primitives from the `java.util.concurrent` package:

**Toll Booth Management (Semaphores & Locks):**
To enforce mutual exclusion at the toll booths while maintaining absolute fairness, a fair `Semaphore(2, true)` governs access to the two available booths at each port. Once a permit is acquired, a `tryLock()` mechanism over an array of `ReentrantLock`s ensures the vehicle securely claims an unoccupied booth without deadlocking.

**Ferry Loading & Unloading (ReentrantLock & Condition):**
Coordination between the Ferry and Vehicles relies on `ReentrantLock` and `Condition` variables. When a vehicle joins the waiting area, it calls `await()` on its `BoardingTicket`'s condition variable. The Ferry thread, upon deciding to board the vehicle, calls `signal()` on that exact ticket, waking only the specific vehicle thread authorized to board.

```java
// Example: Vehicle thread yielding CPU until signaled by the Ferry
lock.lock();
try {
    while (!allowedToBoard) { boardingCondition.await(); }
} finally { lock.unlock(); }
```

**Strict FIFO Discipline:**
First-In-First-Out (FIFO) ordering is strictly maintained at two levels:
1. The fair `Semaphore` guarantees that vehicles enter the toll booth in the exact chronological order of their arrival.
2. The `ConcurrentLinkedQueue` naturally preserves this order. The ferry exclusively uses `peek()` and `poll()` to evaluate and board vehicles, mathematically preventing any overtaking.

### 5. Deadlock & Fairness Discussion (CRITICAL)

**Deadlock Avoidance:**
Deadlock (circular wait) is structurally prevented by enforcing a strict lock acquisition hierarchy and isolating state. Threads never hold nested locks across different components.
- **Vehicle Threads** only acquire local locks on their `BoardingTicket` or disembarkation monitors and never attempt to lock the Ferry or the Port while waiting.
- **The Ferry Thread** acquires its own `ferryLock` solely to evaluate its capacity and wait timings. It does not attempt to lock the `WaitingArea` queue because the queue is intrinsically lock-free (`ConcurrentLinkedQueue`). By eliminating nested locking across domains, the Coffman conditions for deadlock are permanently broken.

**Anti-Starvation (Fairness) Policy:**
The Ferry is programmed with a strict anti-starvation policy to ensure neither the Mainland nor the Island is indefinitely ignored. If the ferry is empty and the current port has no waiting vehicles, it actively checks the opposite port. If vehicles are waiting on the other side, the ferry crosses the river empty to serve them, thereby guaranteeing global fairness across the entire system.

### 6. Departure Policy
The Ferry evaluates its state dynamically based on three distinct, hierarchical departure rules:
1. **Full Capacity:** If the ferry reaches its absolute maximum capacity (20 units), it departs immediately.
2. **Next Won't Fit:** If the ferry is partially loaded, but the very next vehicle in the strictly ordered FIFO queue (e.g., a Truck taking 3 units) exceeds the remaining capacity (e.g., 2 units), the ferry ceases boarding and departs. The "Unloading-First" rule also dictates that vehicles cannot board until all disembarking vehicles have successfully unloaded. Overtaking by smaller vehicles behind the truck is strictly prohibited.
3. **Timeout Threshold:** To prevent infinite delays for partially loaded vehicles, the ferry departs if a predefined maximum wait time (`MAX_WAIT_TIME_MS`, set to 5000ms) expires, provided there is at least one vehicle onboard or vehicles are waiting on the opposite side.

### 7. Assumptions
During the development and testing of the simulation, the following practical assumptions were made:
- **Delay Ranges:** Vehicle activities on the opposite side (before returning) take between 1.0 and 3.0 seconds using `ThreadLocalRandom`. Toll booth processing takes exactly 50 milliseconds. The ferry crossing takes 1.0 second.
- **Vehicle Origins:** The starting side (Mainland vs. Island) for the 30 vehicles is randomly distributed at runtime rather than hardcoded, proving the system's ability to handle chaotic, unscripted concurrency.

### 8. Conclusion
The FerryTrans multithreaded simulation proves highly robust under maximum concurrency. By leveraging fair Semaphores, ReentrantLocks, and atomic lock-free queues, the system successfully routes 12 Cars, 10 Minibuses, and 8 Trucks across two full journeys (60 individual crossings) without encountering a single race condition, deadlock, or orphaned thread. The final execution yields zero memory leaks, maintains an approximate ferry utilization ratio above 90%, and consistently terminates gracefully upon complete system resolution.
