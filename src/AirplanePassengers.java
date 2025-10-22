import java.util.Random;

public class AirplanePassengers implements Runnable {
    Random rand = new Random();

    private final Airplane airplane;
    private int passengerCount;
    private final ATC atc;

    // GETTERS & SETTERS
    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }

    // CONSTRUCTOR
    public AirplanePassengers(Airplane airplane, ATC atc) {
        this.airplane = airplane;
        this.atc = atc;
    }

    // METHODS
    public void disembarkAirplane() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        passengerCount = rand.nextInt(1, 50);

        System.out.printf("[%s]: %d Passengers are disembarking from Plane %d at Gate %d. \n",
                Thread.currentThread().getName(),
                passengerCount,
                airplane.getPlaneNo(),
                airplane.getAssignedGate().getGateNo());

        atc.addDisembarkedPassengers(passengerCount);
        airplane.setBoarded(false);
    }

    public void boardAirplane() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        passengerCount = rand.nextInt(1, 50);

        System.out.printf("[%s]: %d Passengers are boarding Plane %d at Gate %d. \n",
                Thread.currentThread().getName(),
                passengerCount,
                airplane.getPlaneNo(),
                airplane.getAssignedGate().getGateNo());

        atc.addBoardedPassengers(passengerCount);
        airplane.setBoarded(true);
        synchronized (airplane) {
            airplane.notifyAll(); // Notify the airplane that boarding is complete
        }
    }

    @Override
    public void run() {
        synchronized (airplane) {
            while (airplane.getAssignedGate() == null || !airplane.getAssignedGate().isOccupied()) {
                try {
                    airplane.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            }
        }

        disembarkAirplane();

        boardAirplane();
        return;
    }

}
