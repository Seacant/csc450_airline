package csc450.airline.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Airport {

  // This is a huge hack
  public static enum AirportRole {
    ORIGIN, DESTINATION, EITHER
  }

  public String code;
  public Double latitude;
  public Double longitude;
  public String city;
  public String region;
  public String country;

  public Airport(ResultSet row, AirportRole role) throws SQLException {
    String prefix = getPrefixByRole(role);

    this.code = row.getString(prefix + "_airport_code");
    this.latitude = row.getDouble(prefix + "_airport_latitude");
    this.longitude = row.getDouble(prefix + "_airport_longitude");
    this.city = row.getString(prefix + "_airport_city");
    this.region = row.getString(prefix + "_airport_region");
    this.country = row.getString(prefix + "_airport_country");
  }

  public static String table(AirportRole role) {
    String prefix = getPrefixByRole(role);

    return "airport " + prefix + "_airport";
  }

  public static String selects(AirportRole role) {
    String prefix = getPrefixByRole(role);

    return 
      prefix + "_airport.airport_code AS " + prefix + "_airport_code, " +
      prefix + "_airport.latitude     AS " + prefix + "_airport_latitude, " +
      prefix + "_airport.longitude    AS " + prefix + "_airport_longitude, " +
      prefix + "_airport.city         AS " + prefix + "_airport_city, " +
      prefix + "_airport.region       AS " + prefix + "_airport_region, " +
      prefix + "_airport.country      AS " + prefix + "_airport_country "
    ;
  }

  public static String joins(AirportRole role) {
    return " ";
  }

  public static String getPrefixByRole(AirportRole role) {
    String prefix = "some";
    switch (role) {
      case ORIGIN:
        prefix = "origin";
        break;
      case DESTINATION:
        prefix = "destination";
        break;
      case EITHER:
    }
    return prefix;
  }
}
