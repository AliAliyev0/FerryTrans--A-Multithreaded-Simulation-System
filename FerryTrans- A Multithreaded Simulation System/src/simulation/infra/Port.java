package simulation.infra;

import simulation.core.Vehicle;
import simulation.core.Ferry;
import simulation.core.SimulationManager;
import simulation.model.BoardingTicket;
import simulation.model.Side;
import simulation.util.StatisticsManager;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class Port {
    private final Side side;
    private final TollBooth[] tollBooths;
    // Fairness is set to TRUE to ensure FIFO access (No overtaking at toll booths)
    private final Semaphore tollBoothSemaphore; 
    private final ReentrantLock[] boothLocks;
    private final WaitingArea waitingArea;

    public Port(Side side) {
        this.side = side;
        this.tollBooths = new TollBooth[] {
            new TollBooth(side.name() + "-Booth-1"),
            new TollBooth(side.name() + "-Booth-2")
        };
        this.boothLocks = new ReentrantLock[] {
            new ReentrantLock(),
            new ReentrantLock()
        };
        // 2 permits representing the 2 toll booths
        this.tollBoothSemaphore = new Semaphore(2, true); 
        this.waitingArea = new WaitingArea(side);
    }

    public void processVehicleArrival(Vehicle vehicle) throws InterruptedException {
        // 1. Wait for and acquire access to a Toll Booth (Fair Semaphore ensures FIFO)
        // [CRITICAL LOCK]: Fair Semaphore acquired to enforce FIFO and Mutual Exclusion at TollBooths
        tollBoothSemaphore.acquire();
        int assignedBoothIndex = -1;
        try {
            // Find an available booth safely using ReentrantLock
            for (int i = 0; i < boothLocks.length; i++) {
                // [CRITICAL LOCK]: Non-blocking tryLock prevents deadlock while searching for an available booth
                if (boothLocks[i].tryLock()) {
                    assignedBoothIndex = i;
                    break;
                }
            }
            if (assignedBoothIndex != -1) {
                tollBooths[assignedBoothIndex].processVehicle(vehicle);
            }
        } finally {
            if (assignedBoothIndex != -1) {
                // [CRITICAL LOCK]: Release toll booth lock
                boothLocks[assignedBoothIndex].unlock();
            }
            // [CRITICAL LOCK]: Release semaphore permit
            tollBoothSemaphore.release();
        }

        // 2. Get the ticket and join the FIFO Waiting Area
        BoardingTicket ticket = new BoardingTicket(vehicle);
        waitingArea.addVehicle(ticket);
        StatisticsManager.log("QUEUE: Vehicle " + vehicle.getId() + " is in the " + side + " Waiting Area. Queue position: " + waitingArea.size());

        // Notify ferry that a vehicle arrived!
        Ferry ferry = SimulationManager.getFerry();
        if (ferry != null) {
            ferry.notifyVehicleArrived();
        }

        // 3. Block until Ferry signals it's time to board (No busy waiting)
        ticket.awaitBoarding();
        
        // Log is handled by Ferry during boarding directly
    }

    public WaitingArea getWaitingArea() {
        return waitingArea;
    }
}
