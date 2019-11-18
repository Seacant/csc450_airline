package csc450.airline.models;

import java.sql.ResultSet;
import java.sql.SQLException;


public class Airline {
  public String code;
  public String name;
  public byte[] logo;

  public Airline(ResultSet row) throws SQLException {
    this.code = row.getString("airline_code");
    this.name = row.getString("airline_name");
    this.logo = row.getBytes("airline_logo");
  }

  public static String table() {
    return "airline";
  }

  public static String selects() {
    return " airline.airline_code AS airline_code,"
        + "  airline.name         AS airline_name,"
        + "  airline.logo         AS airline_logo ";
  }

  public static String joins() {
    return " ";
  }
}
