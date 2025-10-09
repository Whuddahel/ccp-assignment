public class AirplanePassengers implements Runnable {
    private final Airplane airplane;
    private int passengerCount;

    // GETTERS & SETTERS
    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }

    // CONSTRUCTOR
    public AirplanePassengers(Airplane airplane) {
        this.airplane = airplane;
    }

    // METHODS
    public void disembarkAirplane() {
        System.out.printf("[%s]: %d passengers are disembarking from Plane %d at Gate %d. \n",
                Thread.currentThread().getName(),
                passengerCount,
                airplane.getPlaneNo(),
                airplane.getAssignedGate().getGateNo());

        airplane.setBoarded(false);
    }

    public void boardAirplane() {
        System.out.printf("[%s]: %d passengers are boarding Plane %d at Gate %d. \n",
                Thread.currentThread().getName(),
                passengerCount,
                airplane.getPlaneNo(),
                airplane.getAssignedGate().getGateNo());

        airplane.setBoarded(true);
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

}
