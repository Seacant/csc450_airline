package csc450.airline;

import csc450.airline.controllers.Repository;
import csc450.airline.models.*;
import csc450.airline.utility.EntityAlreadyExistsException;
import csc450.airline.utility.EntityNotFoundException;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;


import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.sql.Date;

/**
 * Hello world!
 */
public final class App extends Application {
  private Repository repository;
  private Stage stage;

  public void start(Stage stage) throws SQLException {
    // Get our datastore
    this.repository = new Repository();
    this.stage = stage;

    var scene = new Scene(this.homePage(), 900, 900);

    stage.setTitle("Airline Reservations");
    stage.setScene(scene);
    stage.show();
  }


  public Parent homePage() {
    var reservation_button = new Button("Reserve Flight");
    reservation_button.setOnAction(value -> this.stage.getScene().setRoot(this.reservationPage()));

    var analytics_button = new Button("View Analytics");
    analytics_button.setOnAction(value -> this.stage.getScene().setRoot(this.analyticsPage()));

    var flight_management_button = new Button("Manage Flights");
    flight_management_button
        .setOnAction(value -> this.stage.getScene().setRoot(this.flightManagementPage()));

    return new VBox(reservation_button, analytics_button, flight_management_button);
  }

  public Parent reservationPage() {
    // First, we need the user to 'log in' ;)
    try {
      ObservableList<Customer> users =
          FXCollections.observableList(this.repository.listCustomers());

      users.addListener((Change<? extends Customer> c) -> System.err.println("Test?"));

      return new HBox(new ListView<Customer>(users));
    } catch (SQLException e) {
      return new HBox(new Label("Unable to connect to DB :("));
    }
  }


  @SuppressWarnings("unchecked") // Don't ask :(
  public Parent analyticsPage() {
    var query_parameter_inferface = new VBox();
    var query_results_interface = new VBox();

    var reservations_analytics_pane_button = new Button("Find Customers on a flight");
    reservations_analytics_pane_button.setOnAction(value -> {
      final var flightIdInput = new TextField();
      final var flightDateInput = new TextField();

      var submit_button = new Button("Submit");
      submit_button.setOnAction(nop -> {
        int flight_id;
        try {
          flight_id = Integer.parseInt(flightIdInput.getText());
        } catch (NumberFormatException e) {
          query_results_interface.getChildren()
              .setAll(new Label("Invalid value provided for Flight ID"));
          return;
        }


        Date flight_date;
        try {
          flight_date = Date.valueOf(flightDateInput.getText());
        } catch (IllegalArgumentException e) {
          query_results_interface.getChildren()
              .setAll(new Label("Invalid date format. Please use yyyy-mm-dd"));
          return;
        }

        FlightInstance flight_instance;
        try {
          flight_instance = repository.getFlightInstanceByIdAndDate(flight_id, flight_date);
        } catch (SQLException e) {
          e.printStackTrace();
          query_results_interface.getChildren().setAll(new Label("Database Error :("));
          return;
        } catch (EntityNotFoundException e) {
          query_results_interface.getChildren().setAll(new Label("Flight not found"));
          return;
        }

        try {
          var reservations = repository.getReservationsByFlightInstance(flight_instance);
          var table = new TableView<>(FXCollections.observableList(reservations));

          // Set the columns
          var name_column = new TableColumn<Reservation, String>("Name");
          name_column.setCellValueFactory(
              reservation -> new ReadOnlyStringWrapper(reservation.getValue().customer.name()));

          var tickets_business_column = new TableColumn<Reservation, Number>("Business Tix");
          tickets_business_column.setCellValueFactory(
              reservation -> new ReadOnlyIntegerWrapper(reservation.getValue().tickets_business));

          var tickets_economy_column = new TableColumn<Reservation, Number>("Economy Tix");
          tickets_economy_column.setCellValueFactory(
              reservation -> new ReadOnlyIntegerWrapper(reservation.getValue().tickets_economy));

          var price_column = new TableColumn<Reservation, Number>("Price Paid");
          price_column.setCellValueFactory(
              reservation -> new ReadOnlyIntegerWrapper(reservation.getValue().price));

          table.getColumns().setAll(name_column, tickets_business_column, tickets_economy_column,
              price_column);

          query_results_interface.getChildren().setAll(table);

        } catch (SQLException e) {
          e.printStackTrace();
          query_results_interface.getChildren().setAll(new Label("Database Error :("));
          return;
        }
      });

      query_parameter_inferface.getChildren().setAll(
          new HBox(new Label("Flight ID:"), flightIdInput),
          new HBox(new Label("Flight Date:"), flightDateInput), submit_button);
    });

    return new HBox(new VBox(reservations_analytics_pane_button),
        new VBox(query_parameter_inferface, query_results_interface));
  }

  public Parent flightManagementPage() {
    var query_parameter_inferface = new VBox();
    var query_results_interface = new VBox();

    var new_flight_button = new Button("Add new flight");
    var fill_flights_button = new Button("Fill flights using plan");

    new_flight_button.setOnAction(new_flignt_event -> {
      final var origin_airport_code_input = new TextField();
      final var destination_airport_code_input = new TextField();
      final var depart_time_input = new TextField();
      final var arrival_time_input = new TextField();
      final var economy_price_input = new TextField();
      final var business_price_input = new TextField();
      final var airline_code_input = new TextField();

      var submit_button = new Button("submit");

      submit_button.setOnAction(submit_event -> {
        var origin_airport_code = origin_airport_code_input.getText();
        var destination_airport_code = destination_airport_code_input.getText();
        var depart_time_str = depart_time_input.getText();
        var arrival_time_str = arrival_time_input.getText();
        var economy_price_str = economy_price_input.getText();
        var business_price_str = business_price_input.getText();
        var airline_code = airline_code_input.getText();

        try {
          Airport origin;
          try {
            repository.getAirportByCode(origin_airport_code);
          }
          catch(EntityNotFoundException e){
            query_results_interface.getChildren().setAll(
              new Label("Origin airport does not exist")
            );
            return;
          }

          Airport destination;
          try {
            repository.getAirportByCode(destination_airport_code);
          }
          catch(EntityNotFoundException e){
            query_results_interface.getChildren().setAll(
              new Label("Destination airport does not exist")
            );
            return;
          }

          Airline airline;
          try {
            repository.getAirlineByCode(airline_code);
          }
          catch(EntityNotFoundException e){
            query_results_interface.getChildren().setAll(
              new Label("Airline does not exist")
            );
            return;
          }

          Duration depart_time;
          try{
            var depart_parts = depart_time_str.split(":");

            if(depart_parts.length != 3) {
              throw new NumberFormatException();
            }

            var hours = Integer.parseInt(depart_parts[0]);
            var minutes = Integer.parseInt(depart_parts[1]);
            var seconds = Integer.parseInt(depart_parts[2]);

            depart_time = Duration.ZERO
              .plusHours(hours)
              .plusMinutes(minutes)
              .plusSeconds(seconds);
          }
          catch (NumberFormatException e){
            query_results_interface.getChildren().setAll(
              new Label("Invalid depart_time. Please format using HH:MM:SS")
            );
          }

          Duration arrival_time;
          try{
            var arrival_parts = arrival_time_str.split(":");

            if(arrival_parts.length != 3) {
              throw new NumberFormatException();
            }

            var hours = Integer.parseInt(arrival_parts[0]);
            var minutes = Integer.parseInt(arrival_parts[1]);
            var seconds = Integer.parseInt(arrival_parts[2]);

            arrival_time = Duration.ZERO
              .plusHours(hours)
              .plusMinutes(minutes)
              .plusSeconds(seconds);
          }
          catch (NumberFormatException e){
            query_results_interface.getChildren().setAll(
              new Label("Invalid arrival_time. Please format using HH:MM:SS")
            );
          }

          double economy_price;
          try {
            economy_price = Double.parseDouble(economy_price_str);
          }
          catch(IllegalArgumentException e){
            query_results_interface.getChildren().setAll(
              new Label("Economy price must be a number")
            );
            return;
          }

          double business_price;
          try {
            business_price = Double.parseDouble(business_price_str);
          }
          catch(IllegalArgumentException e){
            query_results_interface.getChildren().setAll(
              new Label("Business price must be a number")
            );
            return;
          }
        }
        catch(SQLException e){
          e.printStackTrace();
          query_results_interface.getChildren().setAll(
            new Label("Problem with database :(")
          );
          return;
        }
      });

      query_parameter_inferface.getChildren().setAll(
          new HBox(new Label("Airline"), airline_code_input),
          new HBox(new Label("Origin Airport"), origin_airport_code_input),
          new HBox(new Label("Destination Airport"), destination_airport_code_input),
          new HBox(new Label("Departure time"), depart_time_input),
          new HBox(new Label("Arrival Time"), arrival_time_input),
          new HBox(new Label("Price economy"), economy_price_input),
          new HBox(new Label("Price business"), business_price_input), submit_button);
    });

    fill_flights_button.setOnAction(value -> {
      try {
        var flight_plans = repository.listFlightPlans();

        System.err.println(flight_plans.get(0).arrival);

        var start = LocalDate.now();
        var end = LocalDate.now().plusDays(30);

        start.datesUntil(end).forEach(date -> {
          for (var flight_plan : flight_plans) {
            try {
              repository.createFlightInstanceFromFlightPlanAndDate(flight_plan, Date.valueOf(date));
            } catch (EntityAlreadyExistsException e) {
              // This is expected. Do nothing.
            } catch (SQLException e) {
              e.printStackTrace();
              query_results_interface.getChildren()
                  .setAll(new Label("Unknown error inserting flight instance. Bailing"));
            }
          }
        });

        query_results_interface.getChildren()
            .setAll(new Label("Successfully inserted new flights"));
      } catch (SQLException e) {
        e.printStackTrace();
        query_results_interface.getChildren().setAll(new Label("Database Error :("));
      }
    });

    return new HBox(new VBox(new_flight_button, fill_flights_button),
        new VBox(query_parameter_inferface, query_results_interface));
  }
}
