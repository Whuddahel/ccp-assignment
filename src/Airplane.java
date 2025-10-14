import java.util.concurrent.BlockingQueue;

public class Airplane implements Runnable {
    private final int planeNo;
    private AirplanePassengers passengers;
    private Gate assignedGate;

    private boolean isServiced = false;
    private boolean isRefuelled = false;
    private boolean isBoarded = true;

    private final Runway runway;
    private final BlockingQueue<Airplane> runwayRequestsQueue;
    private final BlockingQueue<Airplane> refuelRequestQueue;
    private String nextAction; // Literally just used for nicer print statements, oh, and emerg landing too

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

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String status) {
        this.nextAction = status;
    }

    // CONSTRUCTOR
    public Airplane(int id, Runway runway, BlockingQueue<Airplane> runwayRequestsQueue,
            BlockingQueue<Airplane> refuelRequestQueue, String nextAction) {
        this.planeNo = id;
        this.runway = runway;
        this.runwayRequestsQueue = runwayRequestsQueue;
        this.refuelRequestQueue = refuelRequestQueue;
        this.nextAction = nextAction;

        this.passengers = new AirplanePassengers(this);
        new Thread(passengers, "Plane " + id + "'s Passengers").start();
    }

    // METHODS
    void requestLanding() {
        if (nextAction.equals("Emergency Landing")) {
            System.out.printf("[%s]: Plane %d requesting EMERGENCY landing!\n", Thread.currentThread().getName(),
                    this.planeNo);
        } else {
            System.out.printf("[%s]: Plane %d requesting to land.\n", Thread.currentThread().getName(), this.planeNo);
        }

        try {
            runwayRequestsQueue.put(this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.printf("[%s]: Plane %d was interrupted while requesting to land.\n",
                    Thread.currentThread().getName(), this.planeNo);
        }

    }

    public void land() {
        try {
            Thread.sleep(1000); // Simulate time taken to land
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.printf("[%s]: Plane %d has landed.\n", Thread.currentThread().getName(), this.planeNo);

        nextAction = "Landed";

    }

    public void coastToGate() {
        try {
            nextAction = "Docking";
            Thread.sleep(1000); // Simulate time taken to coast to gate
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.printf("[%s]: Plane %d is coasting to Gate %d.\n", Thread.currentThread().getName(), this.planeNo,
                assignedGate.getGateNo());

    }

    public void dock() {
        System.out.printf("[%s]: Plane %d has docked at Gate %d.\n", Thread.currentThread().getName(), this.planeNo,
                assignedGate.getGateNo());
        nextAction = "Idle";
        runway.releaseRunway();

        assignedGate.setDockedPlane(this);
        assignedGate.setOccupied(true);

        try {
            refuelRequestQueue.put(this);
            System.out.printf("[%s]: Plane %d at Gate %d has requested refuelling. \n",
                    Thread.currentThread().getName(),
                    this.planeNo,
                    assignedGate.getGateNo());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        synchronized (assignedGate) {
            assignedGate.notifyAll(); // wake the gate’s service crew
        }
        synchronized (this) {
            notifyAll(); // Notify passengers that the plane has docked and they can disembark
        }
    }

    public boolean isReadyForTakeoff() {
        return isServiced && isRefuelled && isBoarded;
    }

    public void waitUntilReadyForTakeoff() {
        System.out.printf("[%s]: Plane %d is waiting until it is ready for takeoff. (Service, Refuel, Boarding)\n",
                Thread.currentThread().getName(), this.planeNo);

        synchronized (this) {
            while (!isReadyForTakeoff()) {
                try {
                    wait(); // Block until ground crew signals that the plane is ready for takeoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        System.out.printf("[%s]: Plane %d is ready for takeoff!\n", Thread.currentThread().getName(), this.planeNo);
        nextAction = "Takeoff";
    }

    public void requestTakeoff() {
        try {
            runwayRequestsQueue.put(this);
            System.out.printf("[%s]: Plane %d requesting to take off\n",
                    Thread.currentThread().getName(),
                    this.planeNo);

            nextAction = "Takeoff";
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public void takeoff() {
        try {
            Thread.sleep(1000); // Simulate time taken to take off
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.printf("[%s]: Plane %d has taken off from Gate %d.\n", Thread.currentThread().getName(),
                this.planeNo,
                assignedGate.getGateNo());
        assignedGate.setReserved(false);
        assignedGate.setOccupied(false);
        assignedGate.setDockedPlane(null);
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

        requestTakeoff();
        takeoff();
        return;
    }

    // GETTERS & SETTERS
}
