import java.util.List;
import java.util.concurrent.BlockingQueue;

public class Airplane implements Runnable {
    private final int planeNo;
    private AirplanePassengers passengers;
    private Gate assignedGate;

    private boolean isServiced = false;
    private boolean isRefuelled = false;
    private boolean isBoarded = true;

    private final Runway runway;
    private final ATC atc;
    private final List<Airplane> runwayRequestsQueue;
    private final BlockingQueue<Airplane> refuelRequestQueue;
    private String nextAction; // Literally just used for nicer print statements
    private boolean isQueuedLogged = false;

    // Sanity check variables
    private long landingRequestTime;
    private long permissionGrantedTime;

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

    public boolean isQueuedLogged() {
        return isQueuedLogged;
    }

    public void setQueuedLogged(boolean isQueuedLogged) {
        this.isQueuedLogged = isQueuedLogged;
    }

    // CONSTRUCTOR
    public Airplane(int id, Runway runway, List<Airplane> runwayRequestsQueue,
            BlockingQueue<Airplane> refuelRequestQueue, String nextAction, ATC atc) {
        this.planeNo = id;
        this.runway = runway;
        this.runwayRequestsQueue = runwayRequestsQueue;
        this.refuelRequestQueue = refuelRequestQueue;
        this.nextAction = nextAction;
        this.atc = atc;

        this.passengers = new AirplanePassengers(this, atc);
        new Thread(passengers, "Plane " + id + "'s Passengers").start();
    }

    // METHODS
    public void markRequestTime() {
        this.landingRequestTime = System.currentTimeMillis();
    }

    public void markPermissionGrantedTime() {
        this.permissionGrantedTime = System.currentTimeMillis();
    }

    public long getWaitingTime() {
        return permissionGrantedTime - landingRequestTime;
    }

    void requestLanding() {
        if (nextAction.equals("Emergency Landing")) {
            System.out.printf("[%s]: Plane %d requesting EMERGENCY landing!\n", Thread.currentThread().getName(),
                    this.planeNo);
        } else {
            System.out.printf("[%s]: Plane %d requesting to land.\n", Thread.currentThread().getName(), this.planeNo);
        }

        markRequestTime();
        synchronized (runwayRequestsQueue) {
            runwayRequestsQueue.add(this);
            runwayRequestsQueue.notifyAll(); // Notify ATC that a plane is requesting to land
        }
    }

    public void land() {
        try {
            Thread.sleep(1000);
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
            Thread.sleep(1000); // Simulate time taken to dock
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
        synchronized (atc) {
            atc.notifyAll(); // Notify ATC that the runway is free
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
                    wait(); // Either Passengers/ServiceCrew/RefuelTruck will notify when done
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
            System.out.printf("[%s]: Plane %d requesting to take off\n",
                    Thread.currentThread().getName(),
                    this.planeNo);

            markRequestTime();
            synchronized (runwayRequestsQueue) {
                runwayRequestsQueue.add(this);
                runwayRequestsQueue.notifyAll(); // Notify ATC that a plane is requesting to take off
            }

            nextAction = "Takeoff";

            synchronized (this) {
                wait(); // Wait until ATC grants permission to take off
            }
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

        runway.releaseRunway();
        assignedGate.setReserved(false);
        assignedGate.setOccupied(false);
        assignedGate.setDockedPlane(null);

        synchronized (atc) {
            atc.notifyAll(); // Notify ATC that the plane has taken off
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

        requestTakeoff();
        takeoff();
        return;
    }
}
