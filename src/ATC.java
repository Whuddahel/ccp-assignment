import java.util.LinkedList;
import java.util.List;
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
    public Gate getAvailableGate() {
        for (Gate gate : gates) {
            if (!gate.isReserved()) {
                gate.setReserved(true); // If gate is marked occupied only when docked ATC may assign the same gate to
                                        // multiple planes.

                return gate;
            }
        }

        return null; // No available gates
    }

    private Airplane takeAirplaneFromQueue() throws InterruptedException {
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
        return airplane;
    }

    public void rebuildRunwayRequestsQueue(Airplane airplane) {
        synchronized (runwayRequestsQueue) {
            // List<Airplane> tempList = new ArrayList<>(runwayRequestsQueue); // To
            // LinkedList or not to, that is the question.
            List<Airplane> tempList = new LinkedList<>(runwayRequestsQueue); // You know I learned LinkedList, I'm gonna
                                                                             // use it.
            tempList.addFirst(airplane);
            // Wow I sure did save so much time *sarcasm*.

            runwayRequestsQueue.clear();
            runwayRequestsQueue.addAll(tempList);
        }
    }

    public void handleRunwayRequests(Airplane airplane) {
        // 1. TAKE FROM QUEUE
        // 2. CHECK IF RUNWAY IS AVAILABLE
        // 3. CHECK IF GATE IS AVAILABLE (IF LANDING), AND RUNWAY IS AVAILABLE
        // 4. IF BOTH AVAILABLE, GRANT PERMISSION, ASSIGN GATE, ELSE REQUEUE
        // 5. NOTIFY PLANE

        Gate assignedGate = null;

        if (airplane.getNextAction().equals("Landing")) {
            assignedGate = getAvailableGate();
            airplane.setAssignedGate(assignedGate);

            if (assignedGate == null) {
                System.out.printf("[%s]: No available Gates. %s Permission denied to Plane %d. Re-queuing.\n",
                        Thread.currentThread().getName(),
                        airplane.getNextAction(),
                        airplane.getPlaneNo());

                

                try {
                    rebuildRunwayRequestsQueue(airplane);
                    synchronized (this) {
                        this.wait();
                    }
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

            rebuildRunwayRequestsQueue(airplane);
        }

        try {
            runway.acquireRunway(); // Runway permit has to be obtained here. Otherwise, a small window for a race
                                    // condition exists.
            if (airplane.getNextAction().equals("Landing")) {
                System.out.printf("[%s]: Runway free. %s Permission granted to Plane %d. Assigned Gate: %d\n",
                        Thread.currentThread().getName(),
                        airplane.getNextAction(),
                        airplane.getPlaneNo(),
                        assignedGate.getGateNo());
            } else {
                System.out.printf("[%s]: Runway free. %s Permission granted to Plane %d.\n",
                        Thread.currentThread().getName(),
                        airplane.getNextAction(),
                        airplane.getPlaneNo());
            }

            synchronized (airplane) {
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
                Airplane airplane = takeAirplaneFromQueue();

                handleRunwayRequests(airplane);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[%s]: ATC was interrupted while waiting for landing requests.\n",
                        Thread.currentThread().getName());
            }
        }
    }

}
