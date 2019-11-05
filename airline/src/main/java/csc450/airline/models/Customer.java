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
    this.email = row.getString("email");
    this.first_name = row.getString("first_name");
    this.last_name = row.getString("last_name");
    this.phone = row.getString("phone");
    this.address = row.getString("address");
    this.pin = row.getString("pin");
    this.seat_preference = row.getString("seat_preference");
    this.magazine_preference = row.getString("magazine_preference");
  }

  public String name(){
    return (this.first_name + " " + this.last_name);
  }
}