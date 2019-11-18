package csc450.airline.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class FlightInstance {
  public FlightPlan flight_plan;
  public Date date;
  public Aircraft aircraft;
  public int price_business;
  public int price_economy;
  public boolean can_book;
  public double distance;
  public Date departure_time;
  public Date arrival_time;

  public FlightInstance(ResultSet row) throws SQLException {
    this.flight_plan = new FlightPlan(row);
    this.date = row.getDate("flight_date");
    this.aircraft = new Aircraft(row);
    this.price_economy = row.getInt("flight_price_economy");
    this.price_business = row.getInt("flight_price_business");
    this.can_book = row.getString("flight_can_book") == "Y" ? true : false;
    this.distance = row.getInt("flight_distance");
    this.departure_time = row.getDate("flight_departure_time");
    this.arrival_time = row.getDate("flight_arrival_time");
  }


  public static String table() {
    return "flight";
  }

  // These need to match the field names in the constructor
  public static String selects() {
    return Aircraft.selects() + ", "
      + FlightPlan.selects() + ", "
      + "  flight.flight_date         AS flight_date,"
      + "  flight.price_economy       AS flight_price_economy,"
      + "  flight.price_business      AS flight_price_business,"
      + "  flight.can_book            AS flight_can_book,"
      + "  flight_view.distance       AS flight_distance,"
      + "  flight_view.departure_time AS flight_departure_time,"
      + "  flight_view.arrival_time   AS flight_arrival_time";
  }

  public static String joins() {
    return String.join(" ", "INNER JOIN", FlightPlan.table(),
        "ON flight_schedule.flight_id = flight.flight_id", FlightPlan.joins(),
        "INNER JOIN flight_view", "ON flight_view.flight_id = flight.flight_id",
        "AND flight_view.flight_date = flight.flight_date");
  }
}
