package simulation.model;

import simulation.core.Vehicle;
import simulation.util.StatisticsManager;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoardingTicket implements Comparable<BoardingTicket> {
    private final Vehicle vehicle;
    private final ReentrantLock lock;
    private final Condition boardingCondition;
    private boolean allowedToBoard = false;
    private final long enqueueTime;

    public BoardingTicket(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.lock = new ReentrantLock();
        this.boardingCondition = lock.newCondition();
        this.enqueueTime = System.currentTimeMillis();
    }

    public Vehicle getVehicle() { return vehicle; }
    public long getEnqueueTime() { return enqueueTime; }

    public void awaitBoarding() throws InterruptedException {
        lock.lock();
        try {
            while (!allowedToBoard) {
                boardingCondition.await();
            }
            long waitTime = System.currentTimeMillis() - enqueueTime;
            StatisticsManager.recordWaitTime(waitTime);
        } finally {
            lock.unlock();
        }
    }

    public void allowBoarding() {
        lock.lock();
        try {
            allowedToBoard = true;
            boardingCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    // [CRITICAL BONUS B LOGIC]: Priority-FIFO queue scheduling comparator algorithm
    @Override
    public int compareTo(BoardingTicket other) {
        // 1. Check priority status: EMERGENCY always overrides STANDARD vehicles
        if (this.vehicle.getPriority() != other.vehicle.getPriority()) {
            return this.vehicle.getPriority() == VehiclePriority.EMERGENCY ? -1 : 1;
        }
        // 2. If priorities match, fallback to strict arrival timestamp order (FIFO)
        return Long.compare(this.enqueueTime, other.enqueueTime);
    }
}