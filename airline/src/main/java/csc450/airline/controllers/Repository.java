package csc450.airline.controllers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import oracle.jdbc.pool.OracleDataSource;

import csc450.airline.models.*;

// TODO: Setup proper async/await support
public class Repository {
  Connection connection;

  public Repository() throws SQLException {
    var db = new OracleDataSource();
    db.setURL("jdbc:oracle:thin:@citdb.nku.edu:1521:csc450");
    this.connection = db.getConnection("FLETCHERT1", "csc152");
  };

  public ArrayList<Customer> listCustomers() throws SQLException {
    var statement = this.connection.createStatement();
    var rows = statement.executeQuery("SELECT * FROM customer");

    var results = new ArrayList<Customer>(); 
    while(rows.next()){
      results.add(new Customer(rows));
    }

    return results;
  }
}