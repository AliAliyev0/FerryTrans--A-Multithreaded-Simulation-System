package simulation.core;

import simulation.model.Side;
import simulation.infra.Port;
import simulation.config.SimulationConfig;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class SimulationManager {
    private static final Map<Side, Port> ports = new EnumMap<>(Side.class);
    private static Ferry ferry;
    
    // Total 30 vehicles as per prompt (12 Cars, 10 Minibuses, 8 Trucks)
    private static final CountDownLatch vehicleCompletionLatch = new CountDownLatch(SimulationConfig.TOTAL_CARS + SimulationConfig.TOTAL_MINIBUSES + SimulationConfig.TOTAL_TRUCKS);

    static {
        ports.put(Side.MAINLAND, new Port(Side.MAINLAND));
        ports.put(Side.ISLAND, new Port(Side.ISLAND));
    }

    public static Port getPort(Side side) {
        return ports.get(side);
    }
    
    public static void setFerry(Ferry f) {
        ferry = f;
    }
    
    public static Ferry getFerry() {
        return ferry;
    }
    
    public static void markVehicleCompleted() {
        vehicleCompletionLatch.countDown();
    }
    
    public static void awaitAllVehicles() throws InterruptedException {
        vehicleCompletionLatch.await();
    }
}
