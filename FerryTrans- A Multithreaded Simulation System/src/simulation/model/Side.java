package simulation.model;

public enum Side {
    MAINLAND,
    ISLAND;
    
    public Side getOpposite() {
        return this == MAINLAND ? ISLAND : MAINLAND;
    }
}
