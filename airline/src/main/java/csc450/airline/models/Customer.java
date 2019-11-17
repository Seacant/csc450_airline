package csc450.airline.models;

import java.sql.ResultSet;
import java.sql.SQLException;


public class Customer {
  public int id;
  public String email;
  public String first_name;
  public String last_name;
  public String phone;
  public String address;
  public String pin;
  public String seat_preference;
  public String magazine_preference;

  public Customer(ResultSet row) throws SQLException{
    this.id = row.getInt("customer_id");
    this.email = row.getString("customer_email");
    this.first_name = row.getString("customer_first_name");
    this.last_name = row.getString("customer_last_name");
    this.phone = row.getString("customer_phone");
    this.address = row.getString("customer_address");
    this.pin = row.getString("customer_pin");
    this.seat_preference = row.getString("customer_seat_preference");
    this.magazine_preference = row.getString("customer_magazine_preference");
  }

  public static String selects(){
    return
        "  customer.customer_id         AS customer_id,"
      + "  customer.email               AS customer_email,"
      + "  customer.first_name          AS customer_first_name,"
      + "  customer.last_name           AS customer_last_name,"
      + "  customer.phone               AS customer_phone,"
      + "  customer.address             AS customer_address,"
      + "  customer.pin                 AS customer_pin,"
      + "  customer.seat_preference     AS customer_seat_preference,"
      + "  customer.magazine_preference AS customer_magazine_preference ";
  }

  public static String joins(){return " ";}

  public String name(){
    return (this.first_name + " " + this.last_name);
  }

  @Override
  public String toString(){
    return this.name();
  }
}