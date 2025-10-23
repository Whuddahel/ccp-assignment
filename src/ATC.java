import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ATC implements Runnable {
    private Runway runway;
    private Gate[] gates;
    private List<Airplane> runwayRequestsQueue;
    private List<Airplane> waitingQueue = new ArrayList<>(); // For FCFS order
    private boolean emergencyLogged = false; // Added to prevent spamming emergency logs
    private boolean takeoffPriorityLogged = false;

    // For sanity check
    private int planesLanded = 0;
    private int planesTakenOff = 0;
    private AtomicInteger totalBoardedPassengers = new AtomicInteger(0); // Many passenger threads will edit this and
                                                                         // totalDisembarkedPassengers
    private AtomicInteger totalDisembarkedPassengers = new AtomicInteger(0);

    private final List<Long> waitingTimes = new ArrayList<>();
    private final int TOTAL_AIRPLANES;

    // GETTERS & SETTERS

    // CONSTRUCTOR
    public ATC(Runway runway, List<Airplane> runwayRequestsQueue, Gate[] gates, int totalAirplanes) {
        this.runway = runway;
        this.runwayRequestsQueue = runwayRequestsQueue;
        this.gates = gates;
        this.TOTAL_AIRPLANES = totalAirplanes;
    }

    // METHODS
    public void addBoardedPassengers(int count) {
        totalBoardedPassengers.addAndGet(count);
    }

    public void addDisembarkedPassengers(int count) {
        totalDisembarkedPassengers.addAndGet(count);
    }

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
        synchronized (runwayRequestsQueue) {
            for (Airplane airplane : runwayRequestsQueue) {
                if (airplane.getNextAction().equals("Emergency Landing")) {
                    if (!emergencyLogged) {
                        System.out.printf(
                                "[%s]: EMERGENCY LANDING DETECTED in request queue. Plane %d will be the next plane to land.\n",
                                Thread.currentThread().getName(),
                                airplane.getPlaneNo());
                        emergencyLogged = true;
                    }
                    // runwayRequestsQueue.remove(airplane);
                    requeueFront(waitingQueue, airplane);
                    return airplane;
                }
            }
        }

        synchronized (waitingQueue) {
            for (Airplane airplane : waitingQueue) { // Technically don't need this, all it ever will do it make sure
                                                     // emerg
                                                     // planes stay at the front.
                if (airplane.getNextAction().equals("Emergency Landing")) {
                    if (!emergencyLogged) {
                        System.out.printf(
                                "[%s]: EMERGENCY LANDING DETECTED in waiting queue. Plane %d will be the next plane to land.\n",
                                Thread.currentThread().getName(),
                                airplane.getPlaneNo());
                        emergencyLogged = true;
                    }
                    // waitingQueue.remove(airplane);
                    requeueFront(waitingQueue, airplane);
                    return airplane;
                }
            }
        }
        return null;
    }

    private Airplane findNextTakeoffAirplane() {
        synchronized (waitingQueue) {
            for (Airplane airplane : waitingQueue) {
                if (airplane.getNextAction().equals("Takeoff")) {
                    // System.out.println("Found takeoff airplane in waiting queue: " +
                    // airplane.getPlaneNo());
                    waitingQueue.remove(airplane);
                    requeueFront(waitingQueue, airplane);
                    return airplane;
                }
            }
        }
        synchronized (runwayRequestsQueue) {
            for (Airplane airplane : runwayRequestsQueue) {
                if (airplane.getNextAction().equals("Takeoff")) {
                    // System.out.println("Found takeoff airplane in main queue: " +
                    // airplane.getPlaneNo());
                    runwayRequestsQueue.remove(airplane);
                    requeueFront(waitingQueue, airplane);
                    return airplane;
                }
            }
        }
        return null;
    }

    private void dumpQueues(String when) {
        synchronized (runwayRequestsQueue) {
            synchronized (waitingQueue) {
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
        }
    }

    private void moveAllToWaitingQueue() { // Ensure all planes are responded to immediately upon entry
        Airplane airplane = null;
        synchronized (runwayRequestsQueue) {
            synchronized (waitingQueue) {
                while (!runwayRequestsQueue.isEmpty()) {
                    airplane = runwayRequestsQueue.remove(0);
                    if (!airplane.isQueuedLogged()) {
                        System.out.printf(
                                "[%s]: Permission denied for Plane %d. Other planes are being processed - moved to waiting queue.\n",
                                Thread.currentThread().getName(),
                                airplane.getPlaneNo());
                        airplane.setQueuedLogged(true);
                    }
                    waitingQueue.add(airplane);
                }
                runwayRequestsQueue.notifyAll();
                waitingQueue.notifyAll();
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

    private boolean allGatesFree() {
        for (Gate gate : gates) {
            if (gate.isOccupied() || gate.isReserved()) {
                return false;
            }
        }
        return true;
    }

    public boolean simulationCompleted() {
        synchronized (runwayRequestsQueue) {
            synchronized (waitingQueue) {
                return runwayRequestsQueue.isEmpty()
                        && waitingQueue.isEmpty()
                        && runway.isRunwayAvailable()
                        && allGatesFree();
            }
        }
    }

    private void requeueFront(List<Airplane> queue, Airplane airplane) { // Use to prioritize emergency planes
        synchronized (queue) {
            queue.remove(airplane);
            queue.add(0, airplane);
        }
    }

    private void processNextPlane() throws InterruptedException {
        dumpQueues("Start");
        Airplane nextAirplane = null;
        synchronized (runwayRequestsQueue) {
            while (runwayRequestsQueue.isEmpty() && waitingQueue.isEmpty()) {
                runwayRequestsQueue.wait();
            }

            nextAirplane = findEmergencyAirplane();

            if (nextAirplane == null) {
                synchronized (waitingQueue) {
                    if (!waitingQueue.isEmpty()) {
                        nextAirplane = waitingQueue.get(0); // Don't take, waiting queue order must be preserved for
                                                            // FIFS, only take when approved
                    }
                }

                if (nextAirplane == null && !runwayRequestsQueue.isEmpty()) {
                    nextAirplane = runwayRequestsQueue.get(0);
                    runwayRequestsQueue.remove(0);
                }
            }
        }

        if (nextAirplane == null) {
            return;
        }
        dumpQueues("after select");

        if (findFreeGate() == null) { // If no gates are free then no planes can land, then the program just stalls.
            if (!takeoffPriorityLogged) {
                System.out.printf("[%s]: No free gates available currently. Temporarily prioritizing takeoffs.\n",
                        Thread.currentThread().getName());
                takeoffPriorityLogged = true;
            }
            Airplane takeoffCandidate = findNextTakeoffAirplane();
            if (takeoffCandidate != null) {
                nextAirplane = takeoffCandidate;
            }
        } else {
            takeoffPriorityLogged = false; // Reset log flag when gates become available
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
            synchronized (runwayRequestsQueue) {
                runwayRequestsQueue.remove(nextAirplane);
            }
            synchronized (waitingQueue) {
                waitingQueue.remove(nextAirplane);
            }

            if (nextAirplane.getNextAction().equals("Emergency Landing")
                    || nextAirplane.getNextAction().equals("Landing")) {
                System.out.printf("[%s]: %s Permission Granted to Plane %d. Assigned Gate: %d\n",
                        Thread.currentThread().getName(),
                        nextAirplane.getNextAction(), nextAirplane.getPlaneNo(),
                        nextAirplane.getAssignedGate().getGateNo());
            } else {
                System.out.printf("[%s]: %s Permission Granted to Plane %d.\n", Thread.currentThread().getName(),
                        nextAirplane.getNextAction(), nextAirplane.getPlaneNo());
            }

            nextAirplane.markPermissionGrantedTime();

            if (nextAirplane.getNextAction().equals("Takeoff")) {
                recordServedTakeoff(nextAirplane);
            } else {
                recordServedLanding(nextAirplane);
            }

            synchronized (nextAirplane) {
                nextAirplane.notifyAll();
            }
        } else {
            if (!nextAirplane.isQueuedLogged()) {
                System.out.printf(
                        "[%s]: %s Permission Denied to Plane %d. Next in queue, but either the runway or gates are not available.\n",
                        Thread.currentThread().getName(),
                        nextAirplane.getNextAction(), nextAirplane.getPlaneNo());
                nextAirplane.setQueuedLogged(true);
            }
            if (!waitingQueue.contains(nextAirplane)) {
                waitingQueue.add(nextAirplane);
            }
            dumpQueues("After requeue");

        }

        moveAllToWaitingQueue();
        dumpQueues("After move to wait");

    }

    public synchronized void recordServedTakeoff(Airplane airplane) {
        planesTakenOff++;
        waitingTimes.add(airplane.getWaitingTime());
    }

    public synchronized void recordServedLanding(Airplane airplane) {
        planesLanded++;
        waitingTimes.add(airplane.getWaitingTime());
    }

    private void sanityCheck() {
        System.out.println(
                "**************************************************************************************************");

        System.out.printf("[%s]: Simulation completed. Performing sanity check...\n", Thread.currentThread().getName());

        System.out.printf("[%s]: Checking for occupied gates...\n", Thread.currentThread().getName());
        try {
            Thread.sleep(1000); // Simulate time taken for sanity check
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (allGatesFree()) {
            System.out.printf("[%s]: All gates are free.\n", Thread.currentThread().getName());
        } else {
            System.out.printf("[%s]: Sanity check FAILED! Some gates are still occupied or reserved.\n",
                    Thread.currentThread().getName());
        }

        System.out.printf("[%s]: Checking runway availability...\n", Thread.currentThread().getName());
        try {
            Thread.sleep(1000); // Simulate time taken for sanity check
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (runway.isRunwayAvailable()) {
            System.out.printf("[%s]: Runway is available.\n", Thread.currentThread().getName());
        } else {
            System.out.printf("[%s]: Sanity check FAILED! Runway is still occupied.\n",
                    Thread.currentThread().getName());
        }

        System.out.printf("[%s]: Checking waiting queues...\n", Thread.currentThread().getName());
        try {
            Thread.sleep(1000); // Simulate time taken for sanity check
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (waitingQueue.isEmpty() && runwayRequestsQueue.isEmpty()) {
            System.out.printf("[%s]: No pending requests in queues.\n", Thread.currentThread().getName());
        } else {
            System.out.printf("[%s]: Sanity check FAILED! There are still pending requests in queues.\n",
                    Thread.currentThread().getName());
            for (Gate gate : gates) {
                if (gate.isOccupied() || gate.isReserved()) {
                    System.out.printf("[%s]: Gate %d is still occupied or reserved.\n",
                            Thread.currentThread().getName(), gate.getGateNo());
                }
            }
        }

        System.out.println("-------------------------------- Plane Statistics --------------------------------");
        System.out.printf("[%s]: Total planes landed: %d\n", Thread.currentThread().getName(), planesLanded);
        System.out.printf("[%s]: Total planes taken off: %d\n", Thread.currentThread().getName(), planesTakenOff);

        System.out.printf("[%s]: Total passengers boarded: %d\n", Thread.currentThread().getName(),
                totalBoardedPassengers.get());
        System.out.printf("[%s]: Total passengers disembarked: %d\n", Thread.currentThread().getName(),
                totalDisembarkedPassengers.get());

        // System.out.println("Waiting times of all planes served (ms): " +
        // waitingTimes);

        waitingTimes.sort(null);
        double averageWait = 0;
        for (long wt : waitingTimes) {
            averageWait += wt;
        }
        averageWait /= waitingTimes.size();

        double maxWait = waitingTimes.get(waitingTimes.size() - 1);

        double minNonZero = 0; // One element is definitely 0 (plane 1)
        for (int i = 0; i < waitingTimes.size(); i++) {
            if (waitingTimes.get(i) > 0) {
                minNonZero = waitingTimes.get(i); // It is highly likely the next smallest is also near 0. Gates are
                                                  // full, ATC is waiting for a takeoff, so when a takeoff requests
                                                  // permission it gets it immediately.
                break;
            }
        }

        System.out.printf("[%s]: Average plane waiting time: %.2f ms\n", Thread.currentThread().getName(), averageWait);
        System.out.printf("[%s]: Maximum plane waiting time: %.2f ms\n", Thread.currentThread().getName(), maxWait);
        System.out.printf("[%s]: Minimum non-zero plane waiting time: %.2f ms\n", Thread.currentThread().getName(),
                minNonZero);

        System.out.println(
                "**************************************************************************************************");

    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000); // Initial delay to allow planes to start requesting, so sanity check doesn't
                                // trigger immediately
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(
                "----------------------------------- ATC is now operational -----------------------------------");
        while (true) {
            try {
                if (planesTakenOff >= TOTAL_AIRPLANES) {
                    try {
                        Thread.sleep(2000); // Wait a bit so planes mark gates as empty and they can print logs
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    sanityCheck();
                    return;
                } else
                    processNextPlane();
                Thread.sleep(2000); // Just to smooth console output
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }

}
