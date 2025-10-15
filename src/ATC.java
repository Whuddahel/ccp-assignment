import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ATC implements Runnable {
    private Runway runway;
    private Gate[] gates;
    private BlockingQueue<Airplane> runwayRequestsQueue;
    private BlockingQueue<Airplane> waitingQueue = new ArrayBlockingQueue<>(10); // For FCFS order

    // GETTERS & SETTERS
    // CONSTRUCTOR
    public ATC(Runway runway, BlockingQueue<Airplane> runwayRequestsQueue, Gate[] gates) {
        this.runway = runway;
        this.runwayRequestsQueue = runwayRequestsQueue;
        this.gates = gates;
    }

    // METHODS
    public Gate findFreeGate() {
        for (Gate gate : gates) {
            if (!gate.isReserved()) {
                gate.setReserved(true); // If gate is marked occupied only when docked ATC may assign the same gate to
                                        // multiple planes.
                return gate;
            }
        }
        return null;
    }

    private Airplane takeAirplaneFromQueues() throws InterruptedException {
        // Priority is emerg > waiting > entry queue
        Airplane airplane = null;

        for (Airplane plane : waitingQueue) { // Check for waiting emergency planes first (if its not waiting then its
                                              // already processed after all)
            if (plane.getNextAction().equals("Emergency Landing")) {
                waitingQueue.remove(plane);
                return plane;
            }
        }

        airplane = waitingQueue.poll(); // Waiting queue, no need for sync because waitingQueue is private to ATC, and
                                        // there's only one ATC thread
        if (airplane != null) {
            return airplane;
        }

        airplane = runwayRequestsQueue.take(); // Entry queue (blocking, cuz if there is then there's really nothing
                                               // else to do and you don't wanna waste performance).
                                               // Also, there's no need to sync because BlockingQueue is thread-safe,
                                               // unless you're iterating.

        return airplane;
    }

    public void addToWaitingQueue(Airplane airplane) {
        // If there were more than one ATC then this would be synced on the queue.
        // Also both offer() and put() can be used here.
        try {
            waitingQueue.put(airplane); // Blocking if full, but should never be full in this simulation
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
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
            airplane = takeAirplaneFromQueues(); // Thread will be blocked here if queue is empty, so airplane will
                                                 // always have a value
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        if (airplane.getNextAction().equals("Landing") || airplane.getNextAction().equals("Emergency Landing")) {
            handleLandingRequests(airplane);
        } else {
            handleTakeoffRequests(airplane);
        }

    }

    private void handleLandingRequests(Airplane airplane) {
        Gate assignedGate = findFreeGate();

        if (assignedGate == null) {
            System.out.printf("[%s]: No Available Gates. %s Permission denied to Plane %d. Go wait lol.\n",
                    Thread.currentThread().getName(),
                    airplane.getNextAction(),
                    airplane.getPlaneNo());
            addToWaitingQueue(airplane); // Requeue
            return;
        }

        if (!runway.isRunwayAvailable()) {
            System.out.printf("[%s]: Runway occupied. %s Permission denied to Plane %d. To the waiting queue.\n",
                    Thread.currentThread().getName(),
                    airplane.getNextAction(),
                    airplane.getPlaneNo());
            assignedGate.setReserved(false); // Free up the gate since landing is denied
            addToWaitingQueue(airplane); // Requeue
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

    private void handleTakeoffRequests(Airplane airplane) {
        if (!runway.isRunwayAvailable()) {
            System.out.printf("[%s]: Runway occupied. %s Permission denied to Plane %d. Re-queuing.\n",
                    Thread.currentThread().getName(),
                    airplane.getNextAction(),
                    airplane.getPlaneNo());
        }

        try {
            runway.acquireRunway(); // Runway permit has to be obtained here. Otherwise, a small window for a race
                                    // condition exists.

            System.out.printf("[%s]: Runway free. %s Permission granted to Plane %d.\n",
                    Thread.currentThread().getName(),
                    airplane.getNextAction(),
                    airplane.getPlaneNo());

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
            handleRunwayRequests();
            try {
                Thread.sleep(1000); // Small delay to prevent busy waiting
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

}
