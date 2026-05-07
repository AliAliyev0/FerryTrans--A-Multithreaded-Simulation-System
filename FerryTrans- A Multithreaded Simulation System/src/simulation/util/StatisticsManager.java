package simulation.util;

import simulation.config.SimulationConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StatisticsManager {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final long startTime = System.currentTimeMillis();
    
    private static final AtomicLong totalWaitTime = new AtomicLong(0);
    private static final AtomicLong maxWaitTime = new AtomicLong(0);
    private static final AtomicInteger totalWaitEvents = new AtomicInteger(0);
    
    private static final AtomicInteger totalTrips = new AtomicInteger(0);
    private static final AtomicLong totalLoadTransported = new AtomicLong(0);
    
    private static PrintWriter logWriter;
    
    static {
        try {
            // Initialize file writer. Append=false to start fresh every simulation run.
            // autoFlush=true to ensure logs are written immediately.
            logWriter = new PrintWriter(new FileWriter("simulation_log.txt", false), true);
        } catch (IOException e) {
            System.err.println("Failed to initialize simulation_log.txt");
            e.printStackTrace();
        }
    }

    // Thread-safe writing to both console and file
    public static synchronized void log(String message) {
        String timestamp = LocalTime.now().format(formatter);
        String formattedMessage = String.format("[%s] %s", timestamp, message);
        
        System.out.println(formattedMessage);
        
        if (logWriter != null) {
            logWriter.println(formattedMessage);
        }
    }
    
    public static void recordWaitTime(long waitTimeMs) {
        totalWaitTime.addAndGet(waitTimeMs);
        totalWaitEvents.incrementAndGet();
        long currentMax;
        do {
            currentMax = maxWaitTime.get();
            if (waitTimeMs <= currentMax) {
                break;
            }
        } while (!maxWaitTime.compareAndSet(currentMax, waitTimeMs));
    }
    
    public static void recordTrip(int load) {
        totalTrips.incrementAndGet();
        totalLoadTransported.addAndGet(load);
    }
    
    // Synchronized to ensure no other logs overlap while closing the stream
    public static synchronized void printFinalStatistics() {
        long totalSimTime = System.currentTimeMillis() - startTime;
        int trips = totalTrips.get();
        int waitEvents = totalWaitEvents.get();
        long avgWait = waitEvents == 0 ? 0 : totalWaitTime.get() / waitEvents;
        
        long totalCapacityAvailable = (long) trips * SimulationConfig.FERRY_CAPACITY;
        double utilization = totalCapacityAvailable == 0 ? 0.0 : ((double) totalLoadTransported.get() / totalCapacityAvailable) * 100.0;
        
        String stats = String.format(
            "\n=================================================\n" +
            "          FINAL SIMULATION STATISTICS\n" +
            "=================================================\n" +
            "Total Simulation Time: %d ms\n" +
            "Total Ferry Trips:     %d\n" +
            "Average Wait Time:     %d ms\n" +
            "Max Wait Time:         %d ms\n" +
            "Ferry Utilization:     %.2f%%\n" +
            "=================================================\n",
            totalSimTime, trips, avgWait, maxWaitTime.get(), utilization
        );
        
        System.out.print(stats);
        
        if (logWriter != null) {
            logWriter.print(stats);
            // Resource Management: Properly close the stream when simulation finishes
            logWriter.close();
            logWriter = null;
        }
    }
}
