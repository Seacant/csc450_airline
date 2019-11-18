package csc450.airline.controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;

import oracle.jdbc.pool.OracleDataSource;

import csc450.airline.models.*;
import csc450.airline.utility.EntityAlreadyExistsException;
import csc450.airline.utility.EntityNotFoundException;

// TODO: Setup proper async/await support
public class Repository {
  Connection connection;

  public Repository() throws SQLException {
    var db = new OracleDataSource();
    db.setURL("jdbc:oracle:thin:@10.2.3.9:1521:csc450");
    this.connection = db.getConnection("FLETCHERT1", "csc152");
  };

  public ArrayList<Customer> listCustomers() throws SQLException {
    var statement = this.connection.createStatement();
    var rows = statement.executeQuery(
        String.join(" ", "SELECT", Customer.selects(), "FROM", Customer.table(), Customer.joins()));

    var results = new ArrayList<Customer>();
    while (rows.next()) {
      results.add(new Customer(rows));
    }

    statement.close();

    return results;
  }

  public ArrayList<FlightPlan> listFlightPlans() throws SQLException {
    var statement = this.connection.createStatement();
    var rows = statement.executeQuery(String.join(" ", "SELECT", FlightPlan.selects(), "FROM",
        FlightPlan.table(), FlightPlan.joins()));

    var results = new ArrayList<FlightPlan>();
    while (rows.next()) {
      results.add(new FlightPlan(rows));
    }

    statement.close();

    return results;
  }

  public void createFlightPlanFromData(
    Airport origin,
    Airport destination,
    Duration depart_time,
    Duration arrival_time,
    double price_economy,
    double price_business,
    Airline airline
  ) throws SQLException, EntityAlreadyExistsException {

    var statement = this.connection.prepareStatement(
      "INSERT INTO flight_schedule " 
      + "(origin_airport_code, destination_airport_code, base_departure, base_arrival, base_price_economy, base_price_business, airline_code)"
      + "values('LAX','LUK',INTERVAL '0 8:30:00' DAY TO SECOND, INTERVAL '0 12:30:00' DAY TO SECOND, 105,350,'DL')"
    );

  }

  public void createFlightInstanceFromFlightPlanAndDate(FlightPlan flight_plan, Date date)
      throws SQLException {
    var statement = this.connection.prepareStatement("INSERT INTO flight values(?, ?, ?, ?, ?, ?)");

    statement.setInt(1, flight_plan.id);
    statement.setDate(2, date);

    // TODO: Fix aircraft column
    statement.setInt(3, flight_plan.aircraft.id);

    statement.setDouble(4, flight_plan.price_economy);
    statement.setDouble(5, flight_plan.price_business);

    // can_book
    statement.setString(6, "Y");

    try {
      statement.executeUpdate();
    } catch (SQLException e) {
      // Duplicate. Provide a custome exception
      if (e.getErrorCode() == 1) {
        throw new EntityAlreadyExistsException();
      } else {
        throw e;
      }
    } finally {
      statement.close();
    }
  }

  public FlightInstance getFlightInstanceByIdAndDate(int id, Date date)
      throws SQLException, EntityNotFoundException {

    var statement = this.connection.prepareStatement(
        String.join(" ", "SELECT ", FlightInstance.selects(), "FROM", FlightInstance.table(),
            FlightInstance.joins(), "WHERE flight.flight_id = ? ", "AND flight.flight_date = ?"));

    statement.setInt(1, id);
    statement.setDate(2, date);

    var rows = statement.executeQuery();
    if (rows.next()) {
      return new FlightInstance(rows);
    } else {
      throw new EntityNotFoundException();
    }

  }

  public Airport getAirportByCode(String code) throws SQLException, EntityNotFoundException {
    var statement = this.connection.prepareStatement(String.join(" ",
      "SELECT",
      Airport.selects(Airport.AirportRole.EITHER),
      "FROM",
      Airport.table(Airport.AirportRole.EITHER),
      Airport.joins(Airport.AirportRole.EITHER),
      "WHERE airport_code = ?" 
    ));

    statement.setString(1, code);

    var rows = statement.executeQuery();

    if (rows.next()) {
      return new Airport(rows, Airport.AirportRole.EITHER);
    } else {
      throw new EntityNotFoundException();
    }
  }

  public Airline getAirlineByCode(String code) throws SQLException, EntityNotFoundException {
    var statement = this.connection.prepareStatement(String.join(" ", 
      "SELECT",
      Airline.selects(),
      "FROM",
      Airline.table(),
      Airline.joins(),
      "WHERE airline_code = ?"
    ));

    statement.setString(1, code);

    var rows = statement.executeQuery();

    if(rows.next()){
      return new Airline(rows);
    }
    else{
      throw new EntityNotFoundException();
    }
  }

  public ArrayList<Reservation> getReservationsByFlightInstance(FlightInstance flight)
      throws SQLException {
    var statement = this.connection.prepareStatement(String.join(" ", "SELECT ",
        Reservation.selects(), "FROM", Reservation.table(), Reservation.joins(),
        "WHERE (reservation.tickets_economy > 0 or reservation.tickets_business > 0) ",
        "  AND flight_schedule.flight_id = ? ", "  AND flight.flight_date = ? "));

    statement.setInt(1, flight.flight_plan.id);
    statement.setDate(2, flight.date);

    var rows = statement.executeQuery();

    var results = new ArrayList<Reservation>();
    while (rows.next()) {
      results.add(new Reservation(rows));
    }

    statement.close();

    return results;
  }
}
