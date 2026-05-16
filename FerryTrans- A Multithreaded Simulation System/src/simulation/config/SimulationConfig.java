package simulation.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SimulationConfig {
    // Configurable parameters loaded at runtime instead of hardcoded static final primitives
    public static int FERRY_CAPACITY = 20;
    public static int TOTAL_CARS = 12;
    public static int TOTAL_MINIBUSES = 10;
    public static int TOTAL_TRUCKS = 8;
    public static long MAX_WAIT_TIME_MS = 5000;

    static {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
            FERRY_CAPACITY = Integer.parseInt(props.getProperty("ferry.capacity", "20"));
            TOTAL_CARS = Integer.parseInt(props.getProperty("total.cars", "12"));
            TOTAL_MINIBUSES = Integer.parseInt(props.getProperty("total.minibuses", "10"));
            TOTAL_TRUCKS = Integer.parseInt(props.getProperty("total.trucks", "8"));
            MAX_WAIT_TIME_MS = Long.parseLong(props.getProperty("max.wait.time.ms", "5000"));
            System.out.println("[CONFIG] External configuration file (config.properties) loaded successfully.");
        } catch (IOException | NumberFormatException e) {
            System.out.println("[CONFIG] config.properties not found or invalid. Generating default configuration file...");
            // Automatically creates the configuration file with assignment defaults if missing
            try (FileOutputStream out = new FileOutputStream("config.properties")) {
                props.setProperty("ferry.capacity", "20");
                props.setProperty("total.cars", "12");
                props.setProperty("total.minibuses", "10");
                props.setProperty("total.trucks", "8");
                props.setProperty("max.wait.time.ms", "5000");
                props.store(out, "Ferry Simulation Configuration Parameters");
            } catch (IOException ex) {
                System.err.println("[CONFIG] Failed to write default configuration file: " + ex.getMessage());
            }
        }
    }
}