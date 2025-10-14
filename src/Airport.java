import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Airport {

    public static void main(String[] args) {
        Random rand = new Random();

        Runway runway = new Runway();
        BlockingQueue<Airplane> landingQueue = new ArrayBlockingQueue<>(10);
        BlockingQueue<Airplane> refuelRequestQueue = new ArrayBlockingQueue<>(10);

        Gate gate1 = new Gate(1);
        Gate gate2 = new Gate(2);
        Gate gate3 = new Gate(3);

        Gate[] gates = { gate1, gate2, gate3 };

        ATC atc = new ATC(runway, landingQueue, gates);

        Thread atcThread = new Thread(atc, "ATC");
        atcThread.start();

        RefuellingTruck refuellingTruck = new RefuellingTruck(refuelRequestQueue, gate1);
        Thread refuellingTruckThread = new Thread(refuellingTruck, "Refuelling Truck");
        refuellingTruckThread.start();

        // Start 6 planes
        for (int i = 1; i <= 5; i++) {
            try {
                Thread.sleep(rand.nextInt(2001)); // Random delay between 0 to 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (i==5){
                new Thread(new Airplane(i, runway, landingQueue, refuelRequestQueue, "Emergency Landing", atc), "Plane-" + i).start();
            }else {
                new Thread(new Airplane(i, runway, landingQueue, refuelRequestQueue, "Landing", atc), "Plane-" + i).start();
            }
        }
    }
}
