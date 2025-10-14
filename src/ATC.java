import java.util.concurrent.BlockingQueue;

public class ATC implements Runnable {
    private Runway runway;
    private Gate[] gates;
    private BlockingQueue<Airplane> runwayRequestsQueue;
    private BlockingQueue<Airplane> takeoffQueue;

    // GETTERS & SETTERS
    // CONSTRUCTOR
    public ATC(Runway runway, BlockingQueue<Airplane> runwayRequestsQueue, Gate[] gates) {
        this.runway = runway;
        this.runwayRequestsQueue = runwayRequestsQueue;
        this.gates = gates;
    }

    // METHODS

    public Gate assignGate() {
        for (Gate gate : gates) {
            if (!gate.isReserved()) {
                gate.setReserved(true); // If gate is marked occupied only when docked ATC may assign the same gate to
                                        // multiple planes.

                return gate;
            }
        }

        return null; // No available gates
    }

    public void handleRunwayRequests(Airplane airplane) {
        if (airplane.getNextAction().equals("Takeoff")) {
            Gate assignedGate = assignGate();

            if (assignedGate == null) {
                System.out.printf("[%s]: No available Gates. %s Permission denied to Plane %d. Re-queuing.\n",
                        Thread.currentThread().getName(),
                        airplane.getNextAction(),
                        airplane.getPlaneNo());

                try {
                    runwayRequestsQueue.put(airplane);
                    Thread.sleep(1000); // Small delay to prevent busy waiting
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return;
            }
        }

        if (!runway.isRunwayAvailable()) {
            System.out.printf("[%s]: Runway occupied. %s Permission denied to Plane %d. Re-queuing.\n",
                    Thread.currentThread().getName(),
                    airplane.getNextAction(),
                    airplane.getPlaneNo());
            return;
        }

        try {
            runway.acquireRunway(); // Runway permit has to be obtained here. Otherwise, a small window for a race
                                    // condition exists.

            System.out.printf("[%s]: Runway free. %s Permission granted to Plane %d. Assigned Gate: %d\n",
                    Thread.currentThread().getName(),
                    airplane.getNextAction(),
                    airplane.getPlaneNo(),
                    assignedGate.getGateNo());

            synchronized (airplane) {
                airplane.setAssignedGate(assignedGate);
                airplane.notifyAll();
            }
        } catch (InterruptedException e) {
            System.out.printf("[%s]: Runway occupied. %s Permission denied to Plane %d.\n",
                    Thread.currentThread().getName(),
                    airplane.getNextAction(),
                    airplane.getPlaneNo());

            e.printStackTrace();
            Thread.currentThread().interrupt();
            System.out.printf("[%s]: ATC was interrupted while re-queuing Plane %d.\n",
                    Thread.currentThread().getName(), airplane.getPlaneNo());

        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Airplane emergPlane = null;
                Airplane airplane = null;

                synchronized (runwayRequestsQueue) {
                    for (Airplane plane : runwayRequestsQueue) {
                        if (plane.getNextAction().equals("Emergency Landing")) {
                            emergPlane = plane;
                            break;
                        }
                    }
                    if (emergPlane != null) {
                        runwayRequestsQueue.remove(emergPlane);
                    }
                }

                if (emergPlane != null) {
                    airplane = emergPlane;
                } else {
                    airplane = runwayRequestsQueue.take(); // Blocking call
                }

                handleRunwayRequests(airplane);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[%s]: ATC was interrupted while waiting for landing requests.\n",
                        Thread.currentThread().getName());
            }
        }
    }

}
