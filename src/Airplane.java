import java.util.concurrent.BlockingQueue;

public class Airplane implements Runnable {
    private final int planeNo;
    private AirplanePassengers passengers;
    private Gate assignedGate;

    private boolean isServiced;
    private boolean isRefuelled;
    private boolean isCleaned;

    private final Runway runway;
    private final BlockingQueue<Airplane> landingQueue;

    // GETTERS & SETTERS
    public int getPlaneNo() {
        return planeNo;
    }

    public Gate getAssignedGate() {
        return assignedGate;
    }

    public void setAssignedGate(Gate assignedGate) {
        this.assignedGate = assignedGate;
    }

    // CONSTRUCTOR
    public Airplane(int id, Runway runway, BlockingQueue<Airplane> landingQueue) {
        this.planeNo = id;
        this.runway = runway;
        this.landingQueue = landingQueue;
    }

    // METHODS
    void requestLanding() {
        System.out.printf("[%s]: Plane %d requesting to land.\n", Thread.currentThread().getName(), this.planeNo);
        try {
            landingQueue.put(this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.printf("[%s]: Plane %d was interrupted while requesting to land.\n",
                    Thread.currentThread().getName(), this.planeNo);
        }

    }

    public void land() {
        System.out.printf("[%s]: Plane %d has landed.\n", Thread.currentThread().getName(), this.planeNo);
    }

    public void coastToGate() {
        System.out.printf("[%s]: Plane %d is coasting to Gate %d.\n", Thread.currentThread().getName(), this.planeNo,
                assignedGate.getGateNo());
    }

    public void dock() {
        System.out.printf("[%s]: Plane %d has docked at Gate %d.\n", Thread.currentThread().getName(), this.planeNo,
                assignedGate.getGateNo());
        runway.releaseRunway();

    }

    public boolean isReadyForTakeoff() {
        return isServiced && isRefuelled && isCleaned;
    }

    

    @Override
    public void run() {
        requestLanding(); // Ask ATC

        synchronized (this) {
            try {
                wait(); // Block until ATC grants clearance
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        land();
        coastToGate();
        dock();
    }

    // GETTERS & SETTERS
}
