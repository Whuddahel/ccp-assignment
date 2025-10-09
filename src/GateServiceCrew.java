public class GateServiceCrew implements Runnable {
    private final Gate gate;
    private Airplane assignedPlane;

    // GETTERS & SETTERS
    // CONSTRUCTOR
    public GateServiceCrew(Gate gate) {
        this.gate = gate;
    }

    // METHODS
    public void servicePlane() {
        System.out.printf("[%s]: Gate %d's service crew is servicing Plane %d. \n",
                Thread.currentThread().getName(),
                gate.getGateNo(),
                assignedPlane.getPlaneNo());

        assignedPlane.setServiced(true);
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

}
