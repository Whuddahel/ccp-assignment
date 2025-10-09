import java.util.concurrent.BlockingQueue;

public class ATC implements Runnable {
    private Runway runway;
    private Gate[] gates;
    private BlockingQueue<Airplane> landingQueue;

    // GETTERS & SETTERS
    // CONSTRUCTOR
    public ATC(Runway runway, BlockingQueue<Airplane> landingQueue, Gate[] gates) {
        this.runway = runway;
        this.landingQueue = landingQueue;
        this.gates = gates;
    }

    // METHODS

    public Gate assignGate() {
        for (Gate gate : gates) {
            if (!gate.isOccupied()) {
                gate.setOccupied(true); // If gate is marked occupied only when docked, ATC may assign the same gate to
                                        // multiple planes.
                return gate;
            }
        }
        return null; // No available gates
    }

    public void handleLandingRequest(Airplane airplane) {
        if (!runway.isRunwayAvailable()) {
            System.out.printf("[%s]: Runway occupied. Landing Permission denied to Plane %d. Re-queuing.\n",
                    Thread.currentThread().getName(),
                    airplane.getPlaneNo());
        }

        try {
            runway.acquireRunway(); // Runway permit has to be obtained here. Otherwise, a small window for a race
                                    // condition exists.

            Gate assignedGate = assignGate();
            if (assignedGate == null) {
                System.out.println("This should be unreachable, but let's see what happens.\n");
            }

            System.out.printf("[%s]: Runway free. Landing Permission granted to Plane %d. Assigned Gate: %d\n",
                    Thread.currentThread().getName(),
                    airplane.getPlaneNo(),
                    assignedGate.getGateNo());

            synchronized (airplane) {
                airplane.notify();
                airplane.setAssignedGate(assignedGate);
            }
        } catch (InterruptedException e) {
            System.out.printf("[%s]: Runway occupied. Landing Permission denied to Plane %d.\n",
                    Thread.currentThread().getName(),
                    airplane.getPlaneNo());

            Thread.currentThread().interrupt();
            System.out.printf("[%s]: ATC was interrupted while re-queuing Plane %d.\n",
                    Thread.currentThread().getName(), airplane.getPlaneNo());

        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Airplane airplane = landingQueue.take();
                handleLandingRequest(airplane);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[%s]: ATC was interrupted while waiting for landing requests.\n",
                        Thread.currentThread().getName());
            }
        }
    }

}
