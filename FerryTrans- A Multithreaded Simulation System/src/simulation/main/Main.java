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
        
        // Start Ferry
        Ferry ferry = new Ferry();
        Thread ferryThread = new Thread(ferry, "Ferry-Thread");
        ferryThread.start();
        
        List<Thread> vehicleThreads = new ArrayList<>();
        
        // Start 12 Cars
        for (int i = 0; i < SimulationConfig.TOTAL_CARS; i++) {
            Thread t = new Thread(new Vehicle(VehicleType.CAR), "Car-" + (i+1));
            vehicleThreads.add(t);
            t.start();
        }
        
        // Start 10 Minibuses
        for (int i = 0; i < SimulationConfig.TOTAL_MINIBUSES; i++) {
            Thread t = new Thread(new Vehicle(VehicleType.MINIBUS), "Minibus-" + (i+1));
            vehicleThreads.add(t);
            t.start();
        }
        
        // Start 8 Trucks
        for (int i = 0; i < SimulationConfig.TOTAL_TRUCKS; i++) {
            Thread t = new Thread(new Vehicle(VehicleType.TRUCK), "Truck-" + (i+1));
            vehicleThreads.add(t);
            t.start();
        }
        
        try {
            // Wait for all vehicles to complete their round trips
            SimulationManager.awaitAllVehicles();
            StatisticsManager.log("\n--- All 30 vehicles have completed their round trips! ---");
            
            // Terminate Ferry
            ferry.terminate();
            
            // Join all vehicle threads
            for (Thread t : vehicleThreads) {
                t.join();
            }
            
            // Join ferry thread
            ferryThread.join();
            
            StatisticsManager.log("Simulation fully terminated successfully with zero orphan threads.");
            
            // Print Final Stats
            StatisticsManager.printFinalStatistics();
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
