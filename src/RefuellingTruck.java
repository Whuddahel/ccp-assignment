import java.util.concurrent.Semaphore;

public class RefuellingTruck {
    private final Semaphore refuellingTruckLock = new Semaphore(1);
    private Gate currentGate;

    // GETTERS & SETTERS
    // CONSTRUCTOR
    // METHODS
    public void refuelPlane() {
        System.out.printf("[%s]: Refuelling truck is refuelling Plane %d at Gate %d. \n",
                Thread.currentThread().getName(),
                currentGate.getDockedPlane().getPlaneNo(),
                currentGate.getGateNo());

        try {
            Thread.sleep(2000); // Simulate time taken to refuel
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.printf("[%s]: Refuelling truck has finished refuelling Plane %d at Gate %d. \n",
                Thread.currentThread().getName(),
                currentGate.getDockedPlane().getPlaneNo(),
                currentGate.getGateNo());

        currentGate.getDockedPlane().setRefuelled(true);
    }

    public void acquireRefuellingTruck(Gate gate) throws InterruptedException {
        refuellingTruckLock.acquire();
        this.currentGate = gate;
    }

    public void releaseRefuellingTruck() {
        refuellingTruckLock.release();
        this.currentGate = null;
    }
}
