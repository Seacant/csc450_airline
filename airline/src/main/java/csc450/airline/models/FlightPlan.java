package csc450.airline.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FlightPlan {
  public int id;
  //public Airport origin_airport;
  //public Airport destination_airport;
  //public Airline airline;
  public Aircraft aircraft;
  public String departure;
  public String arrival;
  public Double price_economy;
  public Double price_business;

  public FlightPlan(ResultSet row) throws SQLException{
    this.id = row.getInt("flight_plan_id");
    // this.origin_airport = new Airport(row, AIRPORT.ORIGIN);
    // this.origin_airport = new Airport(row, AIRPORT.DESTINATION);
    // this.airline = new Airline(row);
    this.aircraft = new Aircraft(row);
    this.departure = row.getString("flight_plan_departure");
    this.arrival = row.getString("flight_plan_arrival");
    this.price_business = row.getDouble("flight_plan_price_business");
    this.price_economy = row.getDouble("flight_plan_price_economy");
  }

  // These need to match the field names in the constructor
  public static String selects(){
    return
      // Airport.selects(AIRPORT.ORIGIN) + ", " +
      // Airport.selects(AIRPORT.DESTINATION) + ", " +
      // Airline.selects() + ", " +
      Aircraft.selects() + ", " +
      "  flight_schedule.flight_id           AS flight_plan_id," +
      "  flight_schedule.base_departure      AS flight_plan_departure," +
      "  flight_schedule.base_arrival        AS flight_plan_arrival," +
      "  flight_schedule.base_price_business AS flight_plan_price_business," +
      "  flight_schedule.base_price_economy  AS flight_plan_price_economy "
    ;
  }

  public static String joins(){
    return
      " INNER JOIN aircraft " +
      "   ON aircraft.aircraft_id = flight_schedule.base_aircraft_id" +
      " " + Aircraft.joins() + " "
    ;
  }
}