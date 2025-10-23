import java.util.List;
import java.util.concurrent.Semaphore;

public class RefuellingTruck implements Runnable {
    private final Semaphore refuellingTruckLock = new Semaphore(1);
    private Gate currentGate;
    private List<Airplane> refuelRequestQueue;
    private Airplane airplaneToRefuel;

    private volatile boolean running = true;

    // GETTERS & SETTERS
    // CONSTRUCTOR
    public RefuellingTruck(List<Airplane> refuelRequestQueue, Gate currentGate) {
        this.refuelRequestQueue = refuelRequestQueue;
        this.currentGate = currentGate;
    }

    // METHODS
    public void killMyself() {
        running = false;
        synchronized (refuelRequestQueue) {
            refuelRequestQueue.notifyAll(); // Notify in case the truck is waiting for requests
        }
    }
    private Airplane nextAirplaneToRefuel() throws InterruptedException {
        synchronized (refuelRequestQueue) {
            while (refuelRequestQueue.isEmpty() && running) {
                refuelRequestQueue.wait();
            }
            if (!running) {
                throw new InterruptedException("Refuelling Truck is terminating.");
            }
            return refuelRequestQueue.remove(0);
        }
    }
    public void moveToGate() {

        if (currentGate != airplaneToRefuel.getAssignedGate()) {
            try {
                // Simulate time taken to move to gate
                System.out.printf("[%s @ Gate %d]: Refuelling truck is moving to Gate %d to refuel Plane %d. \n",
                        Thread.currentThread().getName(),
                        currentGate.getGateNo(),
                        airplaneToRefuel.getAssignedGate().getGateNo(),
                        airplaneToRefuel.getPlaneNo());

                Thread.sleep(1000);
                currentGate = airplaneToRefuel.getAssignedGate();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
        System.out.printf("[%s @ Gate %d]: Refuelling truck is at Gate %d. Proceeding with refuelling.\n",
                Thread.currentThread().getName(),
                currentGate.getGateNo(),
                currentGate.getGateNo());

    }

    public void refuelPlane() {
        System.out.printf("[%s @ Gate %d]: Refuelling truck is refuelling Plane %d at Gate %d. \n",
                Thread.currentThread().getName(),
                currentGate.getGateNo(),
                currentGate.getDockedPlane().getPlaneNo(),
                currentGate.getGateNo());

        try {
            Thread.sleep(2000); // Simulate time taken to refuel
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.printf("[%s @ Gate %d]: Refuelling truck has finished refuelling Plane %d at Gate %d. \n",
                Thread.currentThread().getName(),
                currentGate.getGateNo(),
                currentGate.getDockedPlane().getPlaneNo(),
                currentGate.getGateNo());

        currentGate.getDockedPlane().setRefuelled(true);
        synchronized (airplaneToRefuel) {
            airplaneToRefuel.notifyAll(); // Notify plane that refuelling is complete
        }
    }

    public void acquireRefuellingTruck() throws InterruptedException {
        refuellingTruckLock.acquire();
    }

    public void releaseRefuellingTruck() {
        refuellingTruckLock.release();

    }

    @Override
    public void run() {
        while (true) {
            try {
                airplaneToRefuel = nextAirplaneToRefuel();

                acquireRefuellingTruck();
                moveToGate();
                refuelPlane();
                releaseRefuellingTruck();
            } catch (InterruptedException e) {
                if (!running) {
                    break;
                }
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Refuelling Truck is terminating.");
        return;
    }
}
