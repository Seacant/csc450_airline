package csc450.airline.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FlightPlan {
  public int id;
  public Airport origin_airport;
  public Airport destination_airport;
  public Airline airline;
  public Aircraft aircraft;
  public String departure;
  public String arrival;
  public Double price_economy;
  public Double price_business;

  public FlightPlan(ResultSet row) throws SQLException {
    this.id = row.getInt("flight_plan_id");
    this.origin_airport = new Airport(row, Airport.AirportRole.ORIGIN);
    this.destination_airport = new Airport(row, Airport.AirportRole.DESTINATION);
    this.airline = new Airline(row);
    this.aircraft = new Aircraft(row);
    this.departure = row.getString("flight_plan_departure");
    this.arrival = row.getString("flight_plan_arrival");
    this.price_business = row.getDouble("flight_plan_price_business");
    this.price_economy = row.getDouble("flight_plan_price_economy");
  }

  public static String table() {
    return "flight_schedule";
  }

  // These need to match the field names in the constructor
  public static String selects() {
    return Airport.selects(Airport.AirportRole.ORIGIN) + ", "
        + Airport.selects(Airport.AirportRole.DESTINATION) + ", "
        + Airline.selects() + ", "
        + Aircraft.selects() + ", "
        + "  flight_schedule.flight_id           AS flight_plan_id,"
        + "  flight_schedule.base_departure      AS flight_plan_departure,"
        + "  flight_schedule.base_arrival        AS flight_plan_arrival,"
        + "  flight_schedule.base_price_business AS flight_plan_price_business,"
        + "  flight_schedule.base_price_economy  AS flight_plan_price_economy ";
  }

  public static String joins() {
    return String.join(" ",
      "INNER JOIN",
      Aircraft.table(),
      "ON aircraft.aircraft_id = flight_schedule.base_aircraft_id",
      Aircraft.joins(),

      "INNER JOIN",
      Airline.table(),
      "ON airline.airline_code = flight_schedule.airline_code",
      Airline.joins(),
      
      "INNER JOIN", Airport.table(Airport.AirportRole.ORIGIN),
      "ON", Airport.getPrefixByRole(Airport.AirportRole.ORIGIN) + "_airport.airport_code = flight_schedule.origin_airport_code",
      Airport.joins(Airport.AirportRole.ORIGIN),
      
      "INNER JOIN", Airport.table(Airport.AirportRole.DESTINATION),
      "ON ", Airport.getPrefixByRole(Airport.AirportRole.DESTINATION) + "_airport.airport_code = flight_schedule.destination_airport_code",
      Airport.joins(Airport.AirportRole.DESTINATION)
    );
  }
}
