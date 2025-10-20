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

    private boolean isPlaneResourceFree(Airplane airplane) {
        boolean needsGate = airplane.getNextAction().equals("Landing") ||
                airplane.getNextAction().equals("Emergency Landing");
        if (needsGate) {
            return runway.isRunwayAvailable() && findFreeGate() != null;
        } else if (airplane.getNextAction().equals("Takeoff")) {
            return runway.isRunwayAvailable();
        }

        return false;
    }

    private Airplane findEmergencyAirplane() {
        for (Airplane airplane : runwayRequestsQueue) {
            if (airplane.getNextAction().equals("Emergency Landing")) {
                runwayRequestsQueue.remove(airplane);
                System.out.printf("[%s]: EMERGENCY LANDING DETECTED. Plane %d will be the next plane to land.\n",
                        Thread.currentThread().getName(),
                        airplane.getPlaneNo());
                return airplane;
            }
        }
        return null;
    }

    private void moveAllToWaitingQueue() {
        Airplane airplane = null;
        while ((airplane = runwayRequestsQueue.poll()) != null) {
            try {
                if (airplane.isQueuedLogged()) {
                    System.out.printf(
                            "[%s]: Permission denied for Plane %d. Other planes are being processed - moved to waiting queue.\n",
                            Thread.currentThread().getName(),
                            airplane.getPlaneNo());
                    airplane.setQueuedLogged(true);
                }
                waitingQueue.put(airplane);

            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }

        }
    }

    private Gate findFreeGate() {
        for (Gate gate : gates) {
            if (!gate.isReserved()) {
                return gate;
            }
        }
        return null;
    }

    private void processNextPlane() throws InterruptedException {
        Airplane nextAirplane = findEmergencyAirplane();

        if (nextAirplane == null) {
            nextAirplane = waitingQueue.poll(); 
            if (nextAirplane == null) {
                nextAirplane = runwayRequestsQueue.take(); // Block until a plane arrives
            }
        }

        if (nextAirplane == null) {
            return; // No planes to process
        }

        moveAllToWaitingQueue();

        if (isPlaneResourceFree(nextAirplane)) {
            if (nextAirplane.getNextAction().equals("Emergency Landing")
                    || nextAirplane.getNextAction().equals("Landing")) {
                Gate assignedGate = findFreeGate();
                if (assignedGate != null) {
                    assignedGate.setReserved(true);
                }
                nextAirplane.setAssignedGate(assignedGate);
            }

            runway.acquireRunway(); // Acquire runway for the plane

            System.out.printf("[%s]: %s Permission Granted to Plane %d\n", Thread.currentThread().getName(),
                    nextAirplane.getNextAction(), nextAirplane.getPlaneNo());
            synchronized (nextAirplane) {
                nextAirplane.notifyAll();
            }
        } else {
            System.out.printf("[%s]: Permission denied for Plane %d. Re-queuing.\n",
                    Thread.currentThread().getName(),
                    nextAirplane.getPlaneNo());
            waitingQueue.put(nextAirplane); // Requeue
            Thread.sleep(500); // Small delay to prevent busy waiting
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                processNextPlane();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }

}
// private Airplane takeAirplaneFromQueues() throws InterruptedException {
// Airplane nextAirplane = null;
// // Priority is emerg > waiting > entry queue

// for (Airplane airplane : runwayRequestsQueue) {
// if (airplane.getNextAction().equals("Emergency Landing")) {
// runwayRequestsQueue.remove(airplane);
// nextAirplane = airplane;
// addToWaitingQueue(airplane);
// break;
// }
// }

// Airplane airplane = null;
// while ((airplane = runwayRequestsQueue.poll()) != null) {
// waitingQueue.put(airplane);
// System.out.printf("[%s]: Plane %d moved to waiting queue. REMEMBER TO CHANGE
// THIS PRINT STATEMENT\n",
// Thread.currentThread().getName(),
// airplane.getPlaneNo());
// }

// if (nextAirplane != null) {
// nextAirplane = waitingQueue.take(); // Blocking if empty
// return nextAirplane;
// }

// if (isPlaneResourceFree(nextAirplane)) {
// System.out.printf("[%s]: %s Permission Granted to Plane %d",
// Thread.currentThread().getName(),
// nextAirplane.getNextAction(), nextAirplane.getPlaneNo());
// synchronized (nextAirplane) {
// nextAirplane.notifyAll();
// }

// }
// return nextAirplane;

// }
// private void handleLandingRequests(Airplane airplane) {
// Gate assignedGate = findFreeGate();

// if (assignedGate == null) {
// System.out.printf("[%s]: No Available Gates. %s Permission denied to Plane
// %d. Go wait lol.\n",
// Thread.currentThread().getName(),
// airplane.getNextAction(),
// airplane.getPlaneNo());
// addToWaitingQueue(airplane); // Requeue
// return;
// }

// if (!runway.isRunwayAvailable()) {
// System.out.printf("[%s]: Runway occupied. %s Permission denied to Plane %d.
// To the waiting.\n",
// Thread.currentThread().getName(),
// airplane.getNextAction(),
// airplane.getPlaneNo());
// assignedGate.setReserved(false); // Free up the gate since landing is denied
// addToWaitingQueue(airplane); // Requeue
// return;
// }

// try {
// runway.acquireRunway(); // Runway permit has to be obtained here. Otherwise,
// a small window for a race
// // condition exists.

// System.out.printf("[%s]: Runway free. %s Permission granted to Plane %d.
// Assigned Gate: %d\n",
// Thread.currentThread().getName(),
// airplane.getNextAction(),
// airplane.getPlaneNo(),
// assignedGate.getGateNo());

// synchronized (airplane) {
// airplane.setAssignedGate(assignedGate);
// airplane.notifyAll();
// }
// } catch (InterruptedException e) {
// System.out.printf("[%s]: Runway occupied. %s Permission denied to Plane
// %d.\n",
// Thread.currentThread().getName(),
// airplane.getNextAction(),
// airplane.getPlaneNo());

// e.printStackTrace();
// Thread.currentThread().interrupt();
// System.out.printf("[%s]: ATC was interrupted while re-queuing Plane %d.\n",
// Thread.currentThread().getName(), airplane.getPlaneNo());

// }
// }

// private void handleTakeoffRequests(Airplane airplane) {
// if (!runway.isRunwayAvailable()) {
// System.out.printf("[%s]: Runway occupied. %s Permission denied to Plane %d.
// Re-queuing.\n",
// Thread.currentThread().getName(),
// airplane.getNextAction(),
// airplane.getPlaneNo());
// }

// try {
// runway.acquireRunway(); // Runway permit has to be obtained here. Otherwise,
// a small window for a race
// // condition exists.

// System.out.printf("[%s]: Runway free. %s Permission granted to Plane %d.\n",
// Thread.currentThread().getName(),
// airplane.getNextAction(),
// airplane.getPlaneNo());

// } catch (InterruptedException e) {
// System.out.printf("[%s]: Runway occupied. %s Permission denied to Plane
// %d.\n",
// Thread.currentThread().getName(),
// airplane.getNextAction(),
// airplane.getPlaneNo());

// e.printStackTrace();
// Thread.currentThread().interrupt();
// System.out.printf("[%s]: ATC was interrupted while re-queuing Plane %d.\n",
// Thread.currentThread().getName(), airplane.getPlaneNo());

// }
// }
