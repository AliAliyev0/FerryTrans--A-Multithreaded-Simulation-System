package simulation.core;

import simulation.model.Side;
import simulation.model.VehicleType;
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
    
    // Synch variables to coordinate disembarkation
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition disembarkCondition = lock.newCondition();
    private boolean hasDisembarked = false;

    public Vehicle(VehicleType type) {
        this.id = idGenerator.incrementAndGet();
        this.type = type;
        // Randomly assign the starting side
        this.startingSide = ThreadLocalRandom.current().nextBoolean() ? Side.MAINLAND : Side.ISLAND;
        StatisticsManager.log("CREATION: Vehicle " + id + " (" + type + ") created at " + startingSide);
    }

    public int getId() {
        return id;
    }

    public VehicleType getType() {
        return type;
    }

    public Side getStartingSide() {
        return startingSide;
    }
    
    public void resetDisembarkState() {
        lock.lock();
        try {
            hasDisembarked = false;
        } finally {
            lock.unlock();
        }
    }

    public void notifyDisembark() {
        // [CRITICAL LOCK]: Ferry thread signals vehicle that it has safely reached the other side.
        lock.lock();
        try {
            hasDisembarked = true;
            disembarkCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void waitForDisembark() throws InterruptedException {
        // [CRITICAL LOCK]: Vehicle thread locks to wait for ferry unloading broadcast.
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
            // ----- FIRST TRIP (Outbound) -----
            Port startingPort = SimulationManager.getPort(startingSide);
            startingPort.processVehicleArrival(this);
            waitForDisembark();
            
            StatisticsManager.log("ARRIVAL: Vehicle " + id + " has reached " + startingSide.getOpposite() + ".");
            
            // Random delay (activities on the other side)
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 3000));
            
            // Prepare for return trip
            resetDisembarkState();
            
            // ----- SECOND TRIP (Return) -----
            Port returnPort = SimulationManager.getPort(startingSide.getOpposite());
            StatisticsManager.log("RETURN: Vehicle " + id + " is entering " + startingSide.getOpposite() + " port for return trip.");
            returnPort.processVehicleArrival(this);
            waitForDisembark();
            
            StatisticsManager.log("COMPLETION: Vehicle " + id + " has returned to " + startingSide + " and finished its round trip!");
            
            // Signal Completion
            SimulationManager.markVehicleCompleted();
            
        } catch (InterruptedException e) {
            StatisticsManager.log("INTERRUPT: Vehicle " + id + " was interrupted.");
            Thread.currentThread().interrupt();
        }
    }
}
