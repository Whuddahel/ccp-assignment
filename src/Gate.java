public class Gate {
    private final int gateNo;
    private Airplane dockedPlane;

    private boolean isReserved;

    private boolean isOccupied;

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
    public Gate(int gateNo) {
        this.gateNo = gateNo;
        this.isOccupied = false;

        this.serviceCrew = new GateServiceCrew(this);
        new Thread(serviceCrew, "Gate " + gateNo + "'s Service Crew").start();
    }

    // METHODS
    // public void requestRefuel() {
    // try {
    // refuelRequestQueue.put(this);
    // System.out.printf("[%s]: Gate %d has requested refuelling for the docked
    // Plane %d. \n",
    // Thread.currentThread().getName(),
    // gateNo,
    // dockedPlane.getPlaneNo());
    // } catch (InterruptedException e) {
    // Thread.currentThread().interrupt();
    // }
    // }

}
