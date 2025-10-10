import java.util.concurrent.BlockingQueue;

public class Gate {
    private final int gateNo;
    private Airplane dockedPlane;

    private boolean isReserved;

    private boolean isOccupied;

    private BlockingQueue<Gate> refuelRequestQueue;
    private GateServiceCrew serviceCrew;

    // GETTERS & SETTERS
    public int getGateNo() {
        return gateNo;
    }

    public boolean isReserved() {
        return isReserved;
    }

    public void setReserved(boolean isReserved) {
        this.isReserved = isReserved;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean isOccupied) {
        this.isOccupied = isOccupied;
    }

    public Airplane getDockedPlane() {
        return dockedPlane;
    }

    public void setDockedPlane(Airplane dockedPlane) {
        this.dockedPlane = dockedPlane;
    }

    // CONSTRUCTOR
    public Gate(int gateNo, BlockingQueue<Gate> refuelRequestQueue) {
        this.gateNo = gateNo;
        this.isOccupied = false;
        this.refuelRequestQueue = refuelRequestQueue;
    }

    // METHODS
    public void requestRefuel() {
        try {
            refuelRequestQueue.put(this);
            System.out.printf("[%s]: Gate %d has requested refuelling for the docked Plane %d. \n",
                    Thread.currentThread().getName(),
                    gateNo,
                    dockedPlane.getPlaneNo());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
