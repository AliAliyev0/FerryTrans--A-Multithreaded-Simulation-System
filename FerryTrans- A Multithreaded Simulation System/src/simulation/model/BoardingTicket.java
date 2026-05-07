package simulation.model;

import simulation.core.Vehicle;
import simulation.util.StatisticsManager;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoardingTicket {
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

    public Vehicle getVehicle() {
        return vehicle;
    }

    // Vehicle Thread calls this to sleep without busy-waiting
    public void awaitBoarding() throws InterruptedException {
        // [CRITICAL LOCK]: Vehicle thread acquires lock to sleep on condition variable, avoiding busy-waiting.
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

    // Ferry Thread calls this to signal the specific vehicle to board
    public void allowBoarding() {
        // [CRITICAL LOCK]: Ferry thread acquires lock to wake the exact FIFO vehicle.
        lock.lock();
        try {
            allowedToBoard = true;
            boardingCondition.signal();
        } finally {
            lock.unlock();
        }
    }
}
