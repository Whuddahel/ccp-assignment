import java.util.concurrent.BlockingQueue;

public class Airplane implements Runnable {
    private final int planeNo;
    private AirplanePassengers passengers;
    private Gate assignedGate;

    private boolean isServiced = false;
    private boolean isRefuelled = false;
    private boolean isBoarded = true;

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

    public boolean isServiced() {
        return isServiced;
    }

    public void setServiced(boolean isServiced) {
        this.isServiced = isServiced;
    }

    public boolean isRefuelled() {
        return isRefuelled;
    }

    public void setRefuelled(boolean isRefuelled) {
        this.isRefuelled = isRefuelled;
    }

    public boolean isBoarded() {
        return isBoarded;
    }

    public void setBoarded(boolean isBoarded) {
        this.isBoarded = isBoarded;
    }

    // CONSTRUCTOR
    public Airplane(int id, Runway runway, BlockingQueue<Airplane> landingQueue) {
        this.planeNo = id;
        this.runway = runway;
        this.landingQueue = landingQueue;

        this.passengers = new AirplanePassengers(this);
        new Thread(passengers, "Plane " + id + "'s Passengers").start();
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

        assignedGate.setDockedPlane(this);
        assignedGate.setOccupied(true);

        synchronized (this) {
            notifyAll(); // Notify passengers that the plane has docked and they can disembark
        }
    }

    public boolean isReadyForTakeoff() {
        return isServiced && isRefuelled && isBoarded;
    }

    public void waitUntilReadyForTakeoff() {
        synchronized (this) {
            while (!isReadyForTakeoff()) {
                try {
                    wait(); // Block until ground crew signals that the plane is ready for takeoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void run() {
        requestLanding(); // Ask ATC

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        land();
        coastToGate();
        dock();

        waitUntilReadyForTakeoff();
    }

    // GETTERS & SETTERS
}
