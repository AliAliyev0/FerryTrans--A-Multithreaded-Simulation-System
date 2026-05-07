package simulation.config;

public class SimulationConfig {
    public static final int FERRY_CAPACITY = 20;
    
    public static final int TOTAL_CARS = 12;
    public static final int TOTAL_MINIBUSES = 10;
    public static final int TOTAL_TRUCKS = 8;
    
    public static final long MAX_WAIT_TIME_MS = 5000;
    
    // DEADLOCK RULE: Resource Acquisition Hierarchy
    // To prevent circular waits (deadlocks), resources MUST be acquired in the following strict order:
    // 1. Port/Terminal Lock (Mainland or Island) - to enter the terminal queue.
    // 2. Ferry Lock - to board the ferry and consume capacity.
    // 
    // A thread must NEVER attempt to acquire the Port Lock while holding the Ferry Lock.
    // If both are needed, the Port Lock must be acquired FIRST.
}
