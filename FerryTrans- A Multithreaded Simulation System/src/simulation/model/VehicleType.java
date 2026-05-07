package simulation.model;

public enum VehicleType {
    CAR(1),
    MINIBUS(2),
    TRUCK(3);

    private final int size;

    VehicleType(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
