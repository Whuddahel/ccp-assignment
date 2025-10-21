import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ATC implements Runnable {
    private Runway runway;
    private Gate[] gates;
    private BlockingQueue<Airplane> runwayRequestsQueue;
    private BlockingQueue<Airplane> waitingQueue = new ArrayBlockingQueue<>(10); // For FCFS order
    private boolean emergencyLogged = false; // Added to prevent spamming emergency logs

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
                if (!emergencyLogged) {
                    System.out.printf("[%s]: EMERGENCY LANDING DETECTED. Plane %d will be the next plane to land.\n",
                            Thread.currentThread().getName(),
                            airplane.getPlaneNo());
                    emergencyLogged = true;
                }
                runwayRequestsQueue.remove(airplane);
                requeueFront(waitingQueue, airplane);
                return airplane;
            }
        }
        for (Airplane airplane : waitingQueue) { // Technically don't need this, all it ever will do it make sure emerg
                                                 // planes stay at the front.
            if (airplane.getNextAction().equals("Emergency Landing")) {
                if (!emergencyLogged) {
                    System.out.printf("[%s]: EMERGENCY LANDING DETECTED. Plane %d will be the next plane to land.\n",
                            Thread.currentThread().getName(),
                            airplane.getPlaneNo());
                    emergencyLogged = true;
                }
                waitingQueue.remove(airplane);
                requeueFront(waitingQueue, airplane);
                return airplane;
            }
        }
        return null;
    }

    private Airplane findNextTakeoffAirplane() {
        for (Airplane airplane : waitingQueue) {
            if (airplane.getNextAction().equals("Takeoff")) {
                System.out.println("Found takeoff airplane in waiting queue: " + airplane.getPlaneNo());
                waitingQueue.remove(airplane);
                requeueFront(waitingQueue, airplane);
                return airplane;
            }
        }
        return null;
    }

    private void dumpQueues(String when) {
        // snapshot arrays to avoid ConcurrentModification noise
        Object[] mainArr = runwayRequestsQueue.toArray();
        Object[] waitArr = waitingQueue.toArray();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s]: QUEUE DUMP (%s) — MainQueue(size=%d): ",
                Thread.currentThread().getName(), when, mainArr.length));
        for (Object o : mainArr) {
            Airplane p = (Airplane) o;
            sb.append(String.format("%d(%s), ", p.getPlaneNo(), p.getNextAction()));
        }

        sb.append(String.format(" || WaitingQueue(size=%d): ",
                waitArr.length));
        for (Object o : waitArr) {
            Airplane p = (Airplane) o;
            sb.append(String.format("%d(%s), ", p.getPlaneNo(), p.getNextAction()));
        }

        System.out.println(sb.toString());
    }

    private void moveAllToWaitingQueue() { // Ensure all planes are responded to immediately upon entry
        Airplane airplane = null;
        while ((airplane = runwayRequestsQueue.poll()) != null) {
            try {
                if (!airplane.isQueuedLogged()) {
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

    private void requeueFront(BlockingQueue<Airplane> queue, Airplane airplane) { // Use to prioritise emergency planes
        synchronized (queue) {
            List<Airplane> temp = new LinkedList<>(); // I learnt linked list in DSTR, so imma gon use it
            queue.drainTo(temp);
            queue.clear();
            queue.add(airplane);
            queue.addAll(temp);
        }
    }

    private void processNextPlane() throws InterruptedException {
        // dumpQueues("Start");
        Airplane nextAirplane = findEmergencyAirplane();

        if (nextAirplane == null) {
            nextAirplane = waitingQueue.peek(); // Don't take, waiting queue order must be preserved for FIFS, only take
                                                // when approved
            if (nextAirplane == null) {
                nextAirplane = runwayRequestsQueue.take(); // Block until a plane arrives, if main queue is empty that
                                                           // means no planes.
            }
        }

        if (nextAirplane == null) {
            return;
        }
        // dumpQueues("after select");

        if (findFreeGate() == null) { // If no gates are free then no planes can land, then the program just stalls.
            System.out.printf("[%s]: No free gates available currently. Temporarily prioritizing takeoffs.\n",
                    Thread.currentThread().getName());
            if (findNextTakeoffAirplane() != null) {
                nextAirplane = findNextTakeoffAirplane();
            }
        }

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

            waitingQueue.remove(nextAirplane); // Remove from waiting queue if present
            System.out.printf("[%s]: %s Permission Granted to Plane %d.\n", Thread.currentThread().getName(), // TODO
                    nextAirplane.getNextAction(), nextAirplane.getPlaneNo());
            synchronized (nextAirplane) {
                nextAirplane.notifyAll();
            }
        } else {
            if (!nextAirplane.isQueuedLogged()) {
                System.out.printf(
                        "[%s]: %s Permission Denied to Plane %d. Next in Queue, but resources(runway, gates) are not available.\n",
                        Thread.currentThread().getName(),
                        nextAirplane.getNextAction(), nextAirplane.getPlaneNo());
                nextAirplane.setQueuedLogged(true);
            }
            if (!waitingQueue.contains(nextAirplane)) {
                waitingQueue.put(nextAirplane);
            }
            // dumpQueues("After requeue");

        }

        moveAllToWaitingQueue();
        // dumpQueues("After move to wait");

    }

    @Override
    public void run() {
        while (true) {
            try {
                processNextPlane();
                // Thread.sleep(2000); // Just to smooth console output
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }

}
