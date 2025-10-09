import java.util.concurrent.Semaphore;

public class Runway {
    private final Semaphore runwayLock = new Semaphore(1);

    // GETTERS & SETTERS
    
    // CONSTRUCTOR

    // METHODS
    public void acquireRunway() throws InterruptedException {

        runwayLock.acquire();

    }

    public boolean tryAcquireRunway() { 
        return runwayLock.tryAcquire();
    }

    public void releaseRunway() {
        runwayLock.release();
    }

    public boolean isRunwayAvailable() {
        return runwayLock.availablePermits() > 0;
    }
}
