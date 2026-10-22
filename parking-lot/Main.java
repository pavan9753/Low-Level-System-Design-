import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        ParkingFloor groundFloor = new ParkingFloor("G");
        groundFloor.addSpot(new ParkingSpot("G-1", SpotType.MOTORCYCLE));
        groundFloor.addSpot(new ParkingSpot("G-2", SpotType.CAR));

        ParkingLot lot = new ParkingLot("City Center", groundFloor);
        Vehicle car = new Vehicle("ABC-123", VehicleType.CAR);

        Optional<ParkingTicket> ticket = lot.park(car);
        if (!ticket.isPresent()) {
            System.out.println("No parking spot is available.");
            return;
        }

        System.out.println("Parked at spot " + ticket.get().getSpot().getId());
        int fee = lot.unpark(ticket.get().getId());
        System.out.println("Parking fee: $" + fee);
    }
}

enum VehicleType {
    MOTORCYCLE,
    CAR
}

enum SpotType {
    MOTORCYCLE,
    CAR
}

class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    String getLicensePlate() { return licensePlate; }
    VehicleType getType() { return type; }
}

class ParkingSpot {
    private final String id;
    private final SpotType type;
    private Vehicle parkedVehicle;

    ParkingSpot(String id, SpotType type) {
        this.id = id;
        this.type = type;
    }

    boolean canFit(Vehicle vehicle) {
        if (parkedVehicle != null) return false;
        return type == SpotType.CAR || vehicle.getType() == VehicleType.MOTORCYCLE;
    }

    void park(Vehicle vehicle) {
        if (!canFit(vehicle)) throw new IllegalStateException("Spot cannot fit this vehicle.");
        parkedVehicle = vehicle;
    }

    void release() {
        if (parkedVehicle == null) throw new IllegalStateException("Spot is already empty.");
        parkedVehicle = null;
    }

    String getId() { return id; }
}

class ParkingFloor {
    private final String name;
    private final List<ParkingSpot> spots = new ArrayList<ParkingSpot>();

    ParkingFloor(String name) { this.name = name; }

    void addSpot(ParkingSpot spot) { spots.add(spot); }

    ParkingSpot findSpot(Vehicle vehicle) {
        for (ParkingSpot spot : spots) {
            if (spot.canFit(vehicle)) return spot;
        }
        return null;
    }

    String getName() { return name; }
}

class ParkingTicket {
    private final String id;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final long entryTimeMillis;
    private boolean closed;

    ParkingTicket(String id, Vehicle vehicle, ParkingSpot spot) {
        this.id = id;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTimeMillis = System.currentTimeMillis();
    }

    int closeAndCalculateFee() {
        if (closed) throw new IllegalStateException("Ticket is already closed.");
        long elapsedMillis = System.currentTimeMillis() - entryTimeMillis;
        long startedHours = Math.max(1, (elapsedMillis + 3_600_000 - 1) / 3_600_000);
        int hourlyRate = vehicle.getType() == VehicleType.CAR ? 5 : 2;
        closed = true;
        return (int) startedHours * hourlyRate;
    }

    String getId() { return id; }
    ParkingSpot getSpot() { return spot; }
    boolean isClosed() { return closed; }
}

class ParkingLot {
    private final String name;
    private final List<ParkingFloor> floors = new ArrayList<ParkingFloor>();
    private final Map<String, ParkingTicket> activeTickets = new HashMap<String, ParkingTicket>();
    private int nextTicketNumber = 1;

    ParkingLot(String name, ParkingFloor... floors) {
        this.name = name;
        for (ParkingFloor floor : floors) this.floors.add(floor);
    }

    Optional<ParkingTicket> park(Vehicle vehicle) {
        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.findSpot(vehicle);
            if (spot != null) {
                spot.park(vehicle);
                String ticketId = name + "-" + nextTicketNumber++;
                ParkingTicket ticket = new ParkingTicket(ticketId, vehicle, spot);
                activeTickets.put(ticketId, ticket);
                return Optional.of(ticket);
            }
        }
        return Optional.empty();
    }

    int unpark(String ticketId) {
        ParkingTicket ticket = activeTickets.remove(ticketId);
        if (ticket == null || ticket.isClosed()) {
            throw new IllegalArgumentException("Unknown or already closed ticket: " + ticketId);
        }
        int fee = ticket.closeAndCalculateFee();
        ticket.getSpot().release();
        return fee;
    }
}
