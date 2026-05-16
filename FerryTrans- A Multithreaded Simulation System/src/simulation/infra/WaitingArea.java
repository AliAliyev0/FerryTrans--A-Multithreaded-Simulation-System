package simulation.infra;

import java.util.concurrent.PriorityBlockingQueue;
import simulation.model.BoardingTicket;
import simulation.model.Side;

public class WaitingArea {
    // Thread-safe lock-free data structure designed for custom sorted elements
    private final PriorityBlockingQueue<BoardingTicket> queue;

    public WaitingArea(Side side) {
        this.queue = new PriorityBlockingQueue<>();
    }

    public void addVehicle(BoardingTicket ticket) {
        queue.add(ticket);
    }

    public BoardingTicket getNextVehicle() {
        return queue.poll();
    }

    public BoardingTicket peekNextVehicle() {
        return queue.peek();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    public int size() {
        return queue.size();
    }
}