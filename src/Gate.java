
public class Gate {
    private final int gateNo;
    private GateServiceCrew serviceCrew;
    private boolean isOccupied;


    // GETTERS & SETTERS
    public int getGateNo() {
        return gateNo;
    }


    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean isOccupied) {
        this.isOccupied = isOccupied;
    }


    // CONSTRUCTOR
    public Gate(int gateNo) {
        this.gateNo = gateNo;
        this.isOccupied = false;
    }
}
