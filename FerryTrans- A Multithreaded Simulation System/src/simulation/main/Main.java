package simulation.main;

import simulation.core.Ferry;
import simulation.core.Vehicle;
import simulation.core.SimulationManager;
import simulation.model.VehicleType;
import simulation.config.SimulationConfig;
import simulation.util.StatisticsManager;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        StatisticsManager.log("Starting Ferry Trans Simulation...");
        
        Ferry ferry = new Ferry();
        Thread ferryThread = new Thread(ferry, "Ferry-Thread");
        ferryThread.start();
        
        List<Thread> vehicleThreads = new ArrayList<>();
        
        for (int i = 0; i < SimulationConfig.TOTAL_CARS; i++) {
            Thread t = new Thread(new Vehicle(VehicleType.CAR), "Car-" + (i + 1));
            vehicleThreads.add(t);
            t.start();
        }
        
        for (int i = 0; i < SimulationConfig.TOTAL_MINIBUSES; i++) {
            Thread t = new Thread(new Vehicle(VehicleType.MINIBUS), "Minibus-" + (i + 1));
            vehicleThreads.add(t);
            t.start();
        }
        
        for (int i = 0; i < SimulationConfig.TOTAL_TRUCKS; i++) {
            Thread t = new Thread(new Vehicle(VehicleType.TRUCK), "Truck-" + (i + 1));
            vehicleThreads.add(t);
            t.start();
        }
        
        try {
            SimulationManager.awaitAllVehicles();
            
            // Dynamically calculate the total number of vehicles
            int totalVehicles = SimulationConfig.TOTAL_CARS
                    + SimulationConfig.TOTAL_MINIBUSES
                    + SimulationConfig.TOTAL_TRUCKS;
            
            StatisticsManager.log(
                    "\n--- All " + totalVehicles +
                    " vehicles have completed their round trips! ---"
            );
            
            ferry.terminate();
            
            for (Thread t : vehicleThreads) {
                t.join();
            }
            
            ferryThread.join();
            
            StatisticsManager.log(
                    "Simulation fully terminated successfully with zero orphan threads."
            );
            
            StatisticsManager.printFinalStatistics();
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}