import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Airport {

    public static void main(String[] args) {
        Runway runway = new Runway();
        BlockingQueue<Airplane> landingQueue = new ArrayBlockingQueue<>(10);
        Gate gate1 = new Gate(1);
        Gate gate2 = new Gate(2);
        Gate gate3 = new Gate(3);

        Gate[] gates = {gate1, gate2, gate3};

        // Start ATC
        Thread atcThread = new Thread(new ATC(runway, landingQueue, gates), "ATC");
        atcThread.start();

        // Start 3 planes
        for (int i = 1; i <= 2; i++) {
            new Thread(new Airplane(i, runway, landingQueue), "Plane-" + i).start();
        }
    }
}
