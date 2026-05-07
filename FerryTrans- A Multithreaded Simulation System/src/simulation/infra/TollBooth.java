package simulation.infra;

import simulation.core.Vehicle;
import simulation.util.StatisticsManager;

public class TollBooth {
    private final String id;

    public TollBooth(String id) {
        this.id = id;
    }

    public void processVehicle(Vehicle vehicle) throws InterruptedException {
        StatisticsManager.log("TOLL: Vehicle " + vehicle.getId() + " (" + vehicle.getType() + ") entering " + id);
        Thread.sleep(50); // Simulating toll payment and barrier processing
        StatisticsManager.log("TOLL: Vehicle " + vehicle.getId() + " cleared " + id);
    }
}
