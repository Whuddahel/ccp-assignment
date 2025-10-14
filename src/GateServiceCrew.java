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

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.printf("[%s]: Gate %d's service crew has finished servicing Plane %d. \n",
                Thread.currentThread().getName(),
                gate.getGateNo(),
                assignedPlane.getPlaneNo());
        assignedPlane.setServiced(true);

        synchronized (assignedPlane) {
            assignedPlane.notifyAll(); // Notify the airplane that servicing is complete
        }
    }

    @Override
    public void run() {
        synchronized (gate) {
            while (true) {
                assignedPlane = gate.getDockedPlane();
                if (assignedPlane != null && gate.isOccupied() && !assignedPlane.isServiced()) {
                    servicePlane();
                }
                try {
                    gate.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
