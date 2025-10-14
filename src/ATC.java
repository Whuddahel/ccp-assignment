import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ATC implements Runnable {
    private Runway runway;
    private Gate[] gates;
    private BlockingQueue<Airplane> runwayRequestsQueue;

    // GETTERS & SETTERS
    // CONSTRUCTOR
    public ATC(Runway runway, BlockingQueue<Airplane> runwayRequestsQueue, Gate[] gates) {
        this.runway = runway;
        this.runwayRequestsQueue = runwayRequestsQueue;
        this.gates = gates;
    }

    // METHODS
    public void assignGateToPlane(Airplane airplane) {
        for (Gate gate : gates) {
            if (!gate.isReserved()) {
                gate.setReserved(true); // If gate is marked occupied only when docked ATC may assign the same gate to
                                        // multiple planes.

                airplane.setAssignedGate(gate);
                return;
            }
        }
        System.out.printf("[%s]: %s Permission denied. No available gates for Plane %d. \n",
                Thread.currentThread().getName(), airplane.getNextAction(),
                airplane.getPlaneNo());
        rebuildRunwayRequestsQueue(airplane);

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
        return; // No available gates
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

    public void handleRunwayRequests() {
        // 1. TAKE FROM QUEUE
        // 2. CHECK IF RUNWAY IS AVAILABLE
        // 3. CHECK IF GATE IS AVAILABLE (IF LANDING), AND RUNWAY IS AVAILABLE
        // 4. IF BOTH AVAILABLE, GRANT PERMISSION, ASSIGN GATE, ELSE REQUEUE
        // 5. NOTIFY PLANE
        Airplane airplane = null;

        try {
            airplane = takeAirplaneFromQueue(); // Thread will be blocked here if queue is empty, so airplane will
                                                // always have a value
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        if (!runway.isRunwayAvailable()) {
            rebuildRunwayRequestsQueue(airplane); // Put airplane back to front of queue for fairness
            System.out.printf("[%s]: Runway is currently occupied. %s Permission denied to Plane %d. \n",
                    Thread.currentThread().getName(), airplane.getNextAction(), airplane.getPlaneNo());
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            return;
        }

        if (airplane.getNextAction().equals("Landing") || airplane.getNextAction().equals("Emergency Landing")) {
            assignGateToPlane(airplane);
        }

        try {
            runway.acquireRunway();

            if (airplane.getNextAction().equals("Landing") && airplane.getAssignedGate() != null) {
                System.out.printf("[%s]: Runway clear. %s Permission granted to Plane %d. Assigned Gate: %d\n",
                        Thread.currentThread().getName(), airplane.getNextAction(), airplane.getPlaneNo(),
                        airplane.getAssignedGate().getGateNo());
            } else {
                System.out.printf("[%s]: Runway clear. %s Permission granted to Plane %d.\n",
                        Thread.currentThread().getName(), airplane.getNextAction(), airplane.getPlaneNo());
            }

            synchronized (airplane) {
                airplane.notifyAll(); // Notify the airplane that it has permission to land/take off
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        while (true) {
            handleRunwayRequests();
        }
    }

}
