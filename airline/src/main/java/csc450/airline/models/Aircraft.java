package csc450.airline.models;

import java.sql.ResultSet;
import java.sql.SQLException;


public class Aircraft {
  public int id;
  public String name;
  public int seats_available_economy;
  public int seats_available_business;

  public Aircraft(ResultSet row) throws SQLException{
    this.id = row.getInt("aircraft_id");
    this.name = row.getString("aircraft_name");
    this.seats_available_business = row.getInt("aircraft_seats_business");
    this.seats_available_economy = row.getInt("aircraft_seats_economy");
  }

  public static String selects(){
    return
        "  aircraft.aircraft_id              AS aircraft_id,"
      + "  aircraft.name                     AS aircraft_name,"
      + "  aircraft.seats_available_economy  AS aircraft_seats_economy,"
      + "  aircraft.seats_available_business AS aircraft_seats_business "
    ;
  }

  public static String joins(){return " ";}
}