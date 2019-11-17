package csc450.airline.controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
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
      "SELECT" + 
        Customer.selects() +
      "FROM customer " +
        Customer.joins()

    );

    var results = new ArrayList<Customer>(); 
    while(rows.next()){
      results.add(new Customer(rows));
    }

    statement.close();

    return results;
  }

  public ArrayList<FlightPlan> listFlightPlans() throws SQLException {
    var statement = this.connection.createStatement();
    var rows = statement.executeQuery(
      "SELECT" + 
        FlightPlan.selects() +
      "FROM flight_schedule " +
        FlightPlan.joins()

    );

    var results = new ArrayList<FlightPlan>(); 
    while(rows.next()){
      results.add(new FlightPlan(rows));
    }

    statement.close();

    return results;
  }

  public void createFlightInstanceFromFlightPlanAndDate(FlightPlan flight_plan, Date date) throws SQLException {
      var statement = this.connection.prepareStatement(
          "INSERT INTO flight values(?, ?, ?, ?, ?, ?)"
      );

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
      }
      catch(SQLException e){
            // Duplicate. Provide a custome exception
            if(e.getErrorCode() == 1){
                throw new EntityAlreadyExistsException();
            }
            else {
                throw e;
            }
      }
      finally {
        statement.close();
      }
  }

  public FlightInstance getFlightInstanceByIdAndDate(int id, Date date) throws SQLException, EntityNotFoundException{
    var statement = this.connection.prepareStatement(
      "SELECT " +
        FlightInstance.selects() +
      "FROM flight " +
      FlightInstance.joins() +
      "WHERE flight.flight_id = ? " +
      "  AND flight.flight_date = ?"
    );

    statement.setInt(1, id);
    statement.setDate(2, date);

    var rows = statement.executeQuery();
    if(rows.next()){
      return new FlightInstance(rows);
    }
    else {
      throw new EntityNotFoundException();
    }

  }

  public ArrayList<Reservation> getReservationsByFlightInstance(FlightInstance flight) throws SQLException{
    var statement = this.connection.prepareStatement(
      "SELECT " +
        Reservation.selects() + 
      "FROM customer " +
      "INNER JOIN reservation" +
      "  ON customer.customer_id = reservation.customer_id " +
      Reservation.joins() +
      "WHERE (reservation.tickets_economy > 0 or reservation.tickets_business > 0) " +
      "  AND flight_schedule.flight_id = ? " + 
      "  AND flight.flight_date = ? "
    );

    statement.setInt(1, flight.flight_plan.id);
    statement.setDate(2, flight.date);

    var rows = statement.executeQuery();

    var results = new ArrayList<Reservation>();
    while(rows.next()){
      results.add(new Reservation(rows));
    }

    statement.close();

    return results;
  }
}