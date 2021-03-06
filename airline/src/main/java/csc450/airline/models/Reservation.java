package csc450.airline.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Reservation {
  public int id;
  public Customer customer;
  public FlightInstance flight;
  public int tickets_business;
  public int tickets_economy;
  public int price;
  public boolean has_paid;
  public String billing_address;

  public Reservation(ResultSet row) throws SQLException {
    this.customer = new Customer(row);
    this.flight = new FlightInstance(row);
    this.tickets_business = row.getInt("reservation_tickets_business");
    this.tickets_economy = row.getInt("reservation_tickets_economy");
    this.price = row.getInt("reservation_price");
    this.has_paid = "Y".equals(row.getString("reservation_has_paid")) ? true : false;
    this.billing_address = row.getString("reservation_billing_address");
  }

  public static String table() {
    return "reservation";
  }

  // These need to match the field names in the constructor
  public static String selects() {
    return Customer.selects() + ", " + FlightInstance.selects() + ", "
        + "  reservation.tickets_business AS reservation_tickets_business,"
        + "  reservation.tickets_economy  AS reservation_tickets_economy,"
        + "  reservation.price            AS reservation_price,"
        + "  reservation.has_paid         AS reservation_has_paid,"
        + "  reservation.billing_address  AS reservation_billing_address ";
  }

  public static String joins() {
    return 
      "INNER JOIN customer " +
      "  ON customer.customer_id = reservation.customer_id " +
      Customer.joins() +
      "INNER JOIN flight " +
      "  ON flight.flight_id = reservation.flight_id " +
      "  AND flight.flight_date = reservation.flight_date " + 
      FlightInstance.joins();
  }

  @Override
  public String toString(){
    return 
      (
        this.has_paid 
          ? ""
          : "(X) "
      ) + 
      this.flight.flight_plan.origin_airport.city +
      " -> " +
      this.flight.flight_plan.destination_airport.city + 
      " (" + this.flight.date + ")"
    ;
  }
}
