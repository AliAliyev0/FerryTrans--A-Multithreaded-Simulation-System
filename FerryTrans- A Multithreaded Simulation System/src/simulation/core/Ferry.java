package simulation.core;

import simulation.infra.Port;
import simulation.infra.WaitingArea;
import simulation.model.BoardingTicket;
import simulation.model.Side;
import simulation.config.SimulationConfig;
import simulation.util.StatisticsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Ferry implements Runnable {
    private Side currentSide;
    private int currentLoad;
    private final List<Vehicle> onboardVehicles;
    
    private volatile boolean isRunning = true;
    
    private final ReentrantLock ferryLock = new ReentrantLock();
    private final Condition vehicleArrived = ferryLock.newCondition();

    public Ferry() {
        this.currentSide = Side.MAINLAND;
        this.currentLoad = 0;
        this.onboardVehicles = new ArrayList<>();
    }

    public void unloadVehicles() {
        if (onboardVehicles.isEmpty()) return;
        
        // [CRITICAL LOGIC]: Unloading-First rule enforced. Unloading loop runs to completion before any boarding logic starts.
        StatisticsManager.log("ARRIVAL: Ferry arrived at " + currentSide + ". Unloading " + onboardVehicles.size() + " vehicles...");
        for (Vehicle v : onboardVehicles) {
            StatisticsManager.log("UNLOADING: Vehicle " + v.getId() + " disembarked at " + currentSide);
            v.notifyDisembark(); // Wake up the vehicle thread so it can finish
        }
        onboardVehicles.clear();
        currentLoad = 0;
    }

    public void notifyVehicleArrived() {
        ferryLock.lock();
        try {
            vehicleArrived.signal();
        } finally {
            ferryLock.unlock();
        }
    }
    
    public void terminate() {
        isRunning = false;
        ferryLock.lock();
        try {
            vehicleArrived.signalAll(); // Wake up Ferry if suspended
        } finally {
            ferryLock.unlock();
        }
    }

    public void loadVehicles() {
        Port currentPort = SimulationManager.getPort(currentSide);
        WaitingArea waitingArea = currentPort.getWaitingArea();
        
        // Load one by one based on capacity
        while (isRunning) {
            BoardingTicket nextTicket = waitingArea.peekNextVehicle();
            if (nextTicket == null) {
                break; // No more vehicles
            }
            
            Vehicle nextVehicle = nextTicket.getVehicle();
            int requiredSpace = nextVehicle.getType().getSize();
            
            // Capacity check
            if (currentLoad + requiredSpace <= SimulationConfig.FERRY_CAPACITY) {
                // Remove from queue
                waitingArea.getNextVehicle();
                
                // Add to ferry
                currentLoad += requiredSpace;
                onboardVehicles.add(nextVehicle);
                
                StatisticsManager.log("BOARDING: Ferry loaded Vehicle " + nextVehicle.getId() + " (" + nextVehicle.getType() + "). Load: " + currentLoad + "/" + SimulationConfig.FERRY_CAPACITY);
                
                // Signal waiting vehicle using Condition variable
                nextTicket.allowBoarding(); 
            } else {
                // [CRITICAL LOGIC]: Strict FIFO rule check - If the next vehicle cannot fit, boarding MUST stop! No overtaking allowed.
                StatisticsManager.log("CRITICAL CHECK: Vehicle " + nextVehicle.getId() + " cannot fit. Boarding STOPS for this trip.");
                break;
            }
        }
    }

    @Override
    public void run() {
        SimulationManager.setFerry(this);
        
        while (isRunning) {
            StatisticsManager.log("\n--- FERRY DOCKED AT " + currentSide + " ---");
            unloadVehicles();
            
            if (!isRunning && currentLoad == 0) break;
            
            long deadline = System.currentTimeMillis() + SimulationConfig.MAX_WAIT_TIME_MS;
            
            // [CRITICAL LOCK]: Acquired ferryLock to safely evaluate departure conditions and suspend if needed.
            ferryLock.lock();
            try {
                while (isRunning) {
                    loadVehicles(); // Loads everything it can
                    
                    if (currentLoad == SimulationConfig.FERRY_CAPACITY) {
                        // [CRITICAL LOGIC]: Departure Policy - Ferry is perfectly full (20 units).
                        StatisticsManager.log("DEPARTURE CONDITION MET: Ferry is FULL (20 units).");
                        break;
                    }
                    
                    Port currentPort = SimulationManager.getPort(currentSide);
                    if (!currentPort.getWaitingArea().isEmpty()) {
                        // [CRITICAL LOGIC]: Departure Policy - Queue has vehicles, but they don't fit. Must depart.
                        StatisticsManager.log("DEPARTURE CONDITION MET: Next waiting vehicle cannot fit.");
                        break;
                    }
                    
                    long remainingTime = deadline - System.currentTimeMillis();
                    if (remainingTime <= 0) {
                        if (currentLoad > 0) {
                            // [CRITICAL LOGIC]: Departure Policy - Wait threshold reached with partial load.
                            StatisticsManager.log("DEPARTURE CONDITION MET: Max wait time reached with load " + currentLoad);
                            break;
                        } else {
                            Port otherPort = SimulationManager.getPort(currentSide.getOpposite());
                            if (!otherPort.getWaitingArea().isEmpty()) {
                                // [CRITICAL LOGIC]: Fairness & Starvation Avoidance - Depart empty to serve the opposite side.
                                StatisticsManager.log("DEPARTURE CONDITION MET: Empty, but other side has vehicles (Starvation Avoidance).");
                                break;
                            } else {
                                // [CRITICAL LOGIC]: Deadlock Prevention - Ferry suspends completely to avoid busy-waiting when both sides are empty.
                                StatisticsManager.log("WAIT: Ferry is empty and no vehicles waiting. Suspending...");
                                vehicleArrived.await(); // wait indefinitely
                                if (!isRunning) break;
                                deadline = System.currentTimeMillis() + SimulationConfig.MAX_WAIT_TIME_MS;
                                continue;
                            }
                        }
                    }
                    
                    // Wait until time expires or a new vehicle arrives
                    vehicleArrived.await(remainingTime, TimeUnit.MILLISECONDS);
                    if (!isRunning) break;
                }
            } catch (InterruptedException e) {
                StatisticsManager.log("INTERRUPT: Ferry interrupted.");
                Thread.currentThread().interrupt();
                break;
            } finally {
                ferryLock.unlock();
            }
            
            if (!isRunning && currentLoad == 0) break;
            
            crossRiver();
        }
        StatisticsManager.log("SHUTDOWN: Ferry thread terminating cleanly.");
    }

    private void crossRiver() {
        StatisticsManager.log("DEPARTURE: Ferry departing " + currentSide + " and crossing the river...");
        StatisticsManager.recordTrip(currentLoad); // Record trip logic
        try {
            Thread.sleep(1000); // Simulate crossing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        currentSide = currentSide.getOpposite();
    }
}
