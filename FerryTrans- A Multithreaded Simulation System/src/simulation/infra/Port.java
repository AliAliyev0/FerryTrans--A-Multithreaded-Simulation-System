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
        // Explicit fairness flag set to true to force FIFO processing rules across toll entries
        this.tollBoothSemaphore = new Semaphore(2, true); 
        this.waitingArea = new WaitingArea(side);
    }

    public void processVehicleArrival(Vehicle vehicle) throws InterruptedException {
        tollBoothSemaphore.acquire();
        int assignedBoothIndex = -1;
        try {
            // [BUGFIX RESOLVED]: Solves an underlying thread scheduling vulnerability. 
            // If booth 0 is held, the semaphore permit rules guarantee that booth 1 is available 
            // or will soon release. Using blocking lock() instead of tryLock() protects transaction integrity.
            if (boothLocks[0].tryLock()) {
                assignedBoothIndex = 0;
            } else {
                boothLocks[1].lock();
                assignedBoothIndex = 1;
            }
            
            tollBooths[assignedBoothIndex].processVehicle(vehicle);
        } finally {
            if (assignedBoothIndex != -1) {
                boothLocks[assignedBoothIndex].unlock();
            }
            tollBoothSemaphore.release();
        }

        // Generate ticket and append to our priority blocking scheduler collection
        BoardingTicket ticket = new BoardingTicket(vehicle);
        waitingArea.addVehicle(ticket);
        StatisticsManager.log("QUEUE: Vehicle " + vehicle.getId() + " (" + vehicle.getType() + " - " + vehicle.getPriority() + ") is in the " + side + " Waiting Area. Queue position: " + waitingArea.size());

        Ferry ferry = SimulationManager.getFerry();
        if (ferry != null) {
            ferry.notifyVehicleArrived();
        }

        // Thread suspended efficiently without relying on infinite loops or busy-waiting flags
        ticket.awaitBoarding();
    }

    public WaitingArea getWaitingArea() {
        return waitingArea;
    }
}