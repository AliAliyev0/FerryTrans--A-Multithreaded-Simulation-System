package simulation.core;

import simulation.model.Side;
import simulation.model.VehicleType;
import simulation.model.VehiclePriority;
import simulation.infra.Port;
import simulation.util.StatisticsManager;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Vehicle implements Runnable {
    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    private final int id;
    private final VehicleType type;
    private final Side startingSide;
    private final VehiclePriority priority;
    
    // Locks and synchronization primitives to safely control cross-river disembarkation
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition disembarkCondition = lock.newCondition();
    private boolean hasDisembarked = false;

    public Vehicle(VehicleType type) {
        this.id = idGenerator.incrementAndGet();
        this.type = type;
        this.startingSide = ThreadLocalRandom.current().nextBoolean() ? Side.MAINLAND : Side.ISLAND;
        
        // 15% probability assigned as an EMERGENCY priority vehicle, otherwise STANDARD
        this.priority = (ThreadLocalRandom.current().nextInt(100) < 15) ? VehiclePriority.EMERGENCY : VehiclePriority.STANDARD;
        
        StatisticsManager.log("CREATION: Vehicle " + id + " (" + type + " - " + priority + ") created at " + startingSide);
    }

    public int getId() { return id; }
    public VehicleType getType() { return type; }
    public Side getStartingSide() { return startingSide; }
    public VehiclePriority getPriority() { return priority; }
    
    public void resetDisembarkState() {
        lock.lock();
        try {
            hasDisembarked = false;
        } finally {
            lock.unlock();
        }
    }

    public void notifyDisembark() {
        lock.lock();
        try {
            hasDisembarked = true;
            disembarkCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void waitForDisembark() throws InterruptedException {
        lock.lock();
        try {
            while (!hasDisembarked) {
                disembarkCondition.await();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        try {
            // ----- OUTBOUND JOURNEY (First Trip) -----
            Port startingPort = SimulationManager.getPort(startingSide);
            startingPort.processVehicleArrival(this);
            waitForDisembark();
            
            StatisticsManager.log("ARRIVAL: Vehicle " + id + " has reached " + startingSide.getOpposite() + ".");
            
            // Simulating business activities on the destination island/mainland side
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3000));
            
            resetDisembarkState();
            
            // ----- RETURN JOURNEY (Second Trip) -----
            Port returnPort = SimulationManager.getPort(startingSide.getOpposite());
            StatisticsManager.log("RETURN: Vehicle " + id + " (" + priority + ") is entering " + startingSide.getOpposite() + " port for return trip.");
            returnPort.processVehicleArrival(this);
            waitForDisembark();
            
            StatisticsManager.log("COMPLETION: Vehicle " + id + " has returned to " + startingSide + " and finished its round trip!");
            
            SimulationManager.markVehicleCompleted();
            
        } catch (InterruptedException e) {
            StatisticsManager.log("INTERRUPT: Vehicle " + id + " was interrupted.");
            Thread.currentThread().interrupt();
        }
    }
}