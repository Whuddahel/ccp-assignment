import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Airport {

    public static void main(String[] args) {
        Random rand = new Random();
        int airplaneCount = 6;

        Runway runway = new Runway();
        List<Airplane> runwayRequestsQueue = new ArrayList<>();
        List<Airplane> refuelRequestQueue = new ArrayList<>();

        Gate gate1 = new Gate(1);
        Gate gate2 = new Gate(2);
        Gate gate3 = new Gate(3);

        Gate[] gates = { gate1, gate2, gate3 };

        ATC atc = new ATC(runway, runwayRequestsQueue, gates, airplaneCount);
        Thread atcThread = new Thread(atc, "ATC");
        atcThread.start();

        RefuellingTruck refuellingTruck = new RefuellingTruck(refuelRequestQueue, gate1);
        Thread refuellingTruckThread = new Thread(refuellingTruck, "Refuelling Truck");
        refuellingTruckThread.start();

        // Start 6 planes, 5th is emerg
        for (int i = 1; i <= airplaneCount; i++) {
            try {
                Thread.sleep(rand.nextInt(2001)); // Random delay between 0 to 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (i == 5) {
                new Thread(new Airplane(i, runway, runwayRequestsQueue, refuelRequestQueue, "Emergency Landing", atc),
                        "Plane-" + i).start();
            } else {
                new Thread(new Airplane(i, runway, runwayRequestsQueue, refuelRequestQueue, "Landing", atc), "Plane-" + i)
                        .start();
            }
        }

        try {
            atcThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        gate1.getServiceCrew().killMyself();
        gate2.getServiceCrew().killMyself();
        gate3.getServiceCrew().killMyself();

        refuellingTruck.killMyself();
        refuellingTruckThread.interrupt();

    }
}
