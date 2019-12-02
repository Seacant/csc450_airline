package csc450.airline;

import csc450.airline.controllers.Repository;
import csc450.airline.models.*;
import csc450.airline.utility.EntityAlreadyExistsException;
import csc450.airline.utility.EntityNotFoundException;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.collections.FXCollections;


import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.sql.Date;
import java.time.Month;

public final class App extends Application {

  private Repository repository;
  private Stage stage;
  private Customer currentCustomer;

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
    reservation_button
        .setOnAction(value -> this.stage.getScene().setRoot(this.wrapper(this.reservationPage())));

    var buy_button = new Button("Pay for reserved Flight");
    buy_button.setOnAction(value -> this.stage.getScene().setRoot(this.wrapper(this.buyPage())));

    var analytics_button = new Button("View Analytics");
    analytics_button
        .setOnAction(value -> this.stage.getScene().setRoot(this.wrapper(this.analyticsPage())));

    var flight_management_button = new Button("Manage Flights");
    flight_management_button.setOnAction(
        value -> this.stage.getScene().setRoot(this.wrapper(this.flightManagementPage())));

    return new VBox(reservation_button, buy_button, analytics_button, flight_management_button);
  }

  public Parent wrapper(Parent inner) {
    var home_button = new Button("<- Go Home");

    home_button.setOnAction(value -> {
      this.stage.getScene().setRoot(this.homePage());
    });

    return new VBox(new HBox(home_button), new Separator(), inner);
  }

  public Parent reservationPage() {
    // First, we need the user to 'log in' ;)
    var flight_search_params = new VBox();
    var flight_results_pane = new VBox();
    var flight_reservation_error_pane = new VBox();
    try {
      ListView<Customer> users =
          new ListView<Customer>(FXCollections.observableList(this.repository.listCustomers()));

      users.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Customer>() {

        @Override
        public void changed(ObservableValue<? extends Customer> change, Customer oldValue,
            Customer newValue) {
          App.this.currentCustomer = newValue;

          var origin_input = new TextField();
          var destination_input = new TextField();
          var date_input = new TextField();
          var tix_business_input = new TextField();
          var tix_economy_input = new TextField();

          var submit_button = new Button("Submit");

          flight_search_params.getChildren().setAll(new Label("Search"),
              new HBox(new Label("Origin airport"), origin_input),
              new HBox(new Label("Destination airport"), destination_input),
              new HBox(new Label("Flight date"), date_input),
              new HBox(new Label("Business tickets amount"), tix_business_input),
              new HBox(new Label("Economy tickets amount"), tix_economy_input), submit_button);
          flight_results_pane.getChildren().clear();

          submit_button.setOnAction(submit_change -> {
            var origin_str = origin_input.getText();
            var destination_str = destination_input.getText();
            var date_str = date_input.getText();
            var tix_business_str = tix_business_input.getText();
            var tix_economy_str = tix_economy_input.getText();

            try {
              Airport origin;
              try {
                origin = repository.getAirportByCode(origin_str);
              } catch (EntityNotFoundException e) {
                flight_results_pane.getChildren().setAll(new Label("Origin airport not found"));
                return;
              }

              Airport destination;
              try {
                destination = repository.getAirportByCode(destination_str);
              } catch (EntityNotFoundException e) {
                flight_results_pane.getChildren()
                    .setAll(new Label("Destination airport not found"));
                return;
              }

              Date date;
              try {
                date = Date.valueOf(date_str);
              } catch (IllegalArgumentException e) {
                flight_results_pane.getChildren().setAll(new Label("Invalid date. Use yyyy-mm-dd"));
                return;
              }

              Integer tix_business;
              try {
                if (tix_business_str == "") {
                  tix_business_str = "0";
                }
                tix_business = Integer.parseInt(tix_business_str);
              } catch (NumberFormatException e) {
                flight_results_pane.getChildren()
                    .setAll(new Label("Please provide a numebr for business tickets"));
                return;
              }

              Integer tix_economy;
              try {
                if (tix_economy_str == "") {
                  tix_economy_str = "0";
                }
                tix_economy = Integer.parseInt(tix_economy_str);
              } catch (NumberFormatException e) {
                flight_results_pane.getChildren()
                    .setAll(new Label("Please provide a numebr for economy tickets"));
                return;
              }

              var flights =
                  repository.findFlightInstanceByAirportsAndDate(origin, destination, date);

              var table = new TableView<>(FXCollections.observableList(flights));

              // Set the columns
              var name_column = new TableColumn<FlightInstance, String>("Airline");
              name_column.setCellValueFactory(reservation -> new ReadOnlyStringWrapper(
                  reservation.getValue().flight_plan.airline.name));

              var id_column = new TableColumn<FlightInstance, Number>("Flight ID");
              id_column.setCellValueFactory(
                  reservation -> new ReadOnlyIntegerWrapper(reservation.getValue().flight_plan.id));

              var price_column = new TableColumn<FlightInstance, Number>("Price");
              price_column.setCellValueFactory(reservation -> new ReadOnlyIntegerWrapper(
                  reservation.getValue().price_business * tix_business
                      + reservation.getValue().price_economy * tix_economy));

              table.getColumns().setAll(name_column, id_column, price_column);

              table.getSelectionModel().selectedItemProperty()
                  .addListener(new ChangeListener<FlightInstance>() {
                    @Override
                    public void changed(ObservableValue<? extends FlightInstance> change,
                        FlightInstance oldValue, FlightInstance newValue) {
                      try {
                        repository.createReservationFromData(App.this.currentCustomer, newValue,
                            tix_business, tix_economy);
                        flight_reservation_error_pane.getChildren()
                            .setAll(new Label("Flight booked!"));
                      } catch (SQLException e) {
                        e.printStackTrace();
                        flight_reservation_error_pane.getChildren()
                            .setAll(new Label("Probelm with Database"));
                      }
                    }
                  });

              flight_results_pane.getChildren().setAll(table);
            } catch (SQLException e) {
              e.printStackTrace();
              flight_results_pane.getChildren().setAll(new Label("Problem with the DB :("));
            }
          });
        }
      });

      return new HBox(users,
          new VBox(flight_search_params, flight_results_pane, flight_reservation_error_pane));
    } catch (SQLException e) {
      return new HBox(new Label("Unable to connect to DB :("));
    }
  }

  public Parent buyPage() {
    try {
      var output = new VBox();
      var users =
          new ListView<Customer>(FXCollections.observableList(this.repository.listCustomers()));
      var reservations = new ListView<Reservation>();
      var billing_address_input = new TextField();
      var submit = new Button("Submit");
      submit.setDisable(true);

      users.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Customer>() {
        @Override
        public void changed(ObservableValue<? extends Customer> observable, Customer oldValue,
            Customer newValue) {
          if (oldValue == null || oldValue.id != newValue.id) {
            try {
              reservations.setItems(
                  FXCollections.observableList(repository.findReservationsByCustomer(newValue)));
              reservations.setDisable(false);
              submit.setDisable(false);
            } catch (SQLException e) {
              // Clear items and disable
              reservations.setItems(FXCollections.observableList(new ArrayList<Reservation>()));;
              reservations.setDisable(true);
              submit.setDisable(true);
            }
          }
        }
      });

      submit.setOnAction(submit_action -> {
        var reservation = reservations.getSelectionModel().getSelectedItem();
        var billing_address = billing_address_input.getText();

        try {
          repository.payForReservation(reservation, billing_address);
        } catch (SQLException e) {
          e.printStackTrace();
          output.getChildren().setAll(new Label("Reservation update failed"));
          return;
        }

        output.getChildren().setAll(new Label("Reservation updated"));
      });

      return new HBox(users, reservations,
          new VBox(new HBox(new Label("Billing Address"), billing_address_input), submit, output));
    } catch (SQLException e) {
      e.printStackTrace();
      return new HBox(new Label("Error with database :("));
    }
  }

  @SuppressWarnings("unchecked") // Don't ask :(
  public Parent analyticsPage() {
    var query_parameter_inferface = new VBox();
    var query_results_interface = new VBox();

    // 1
    var reservations_analytics_pane_button = new Button("Find Customers on a flight");
    reservations_analytics_pane_button.setOnAction(value -> {
      query_results_interface.getChildren().clear();
      query_parameter_inferface.getChildren().clear();
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
          var table = new TableView<Reservation>(FXCollections.observableList(reservations));

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

    // 2
    var sales_trends_analytics_pane_button = new Button("Sales trends");
    sales_trends_analytics_pane_button.setOnAction(value -> {
      try {
        var results = repository.getAirlineAnalytics();
        var table = new TableView<AnalyticResult<Airline>>(FXCollections.observableList(results));

        // Set the columns
        var name_column = new TableColumn<AnalyticResult<Airline>, String>("Name");
        name_column.setCellValueFactory(
          reservation -> new ReadOnlyStringWrapper(reservation.getValue().target.name)
        );
        /*
          result.put("airline_total_price",    Integer.toString(rows.getInt("airline_Total_price")));
          result.put("business_total_tickets", Integer.toString(rows.getInt("business_Total_tickets")));
          result.put("economy_total_tickets",  Integer.toString(rows.getInt("economy_Total_tickets")));
          result.put("price_2019",             Integer.toString(rows.getInt("Price_2019")));
          result.put("price_2018",             Integer.toString(rows.getInt("Price_2018")));
          result.put("price_2017",             Integer.toString(rows.getInt("Price_2017")));
        */

        var airline_total_price = new TableColumn<AnalyticResult<Airline>, String>("Airline Total Price");
        airline_total_price.setCellValueFactory(
          reservation -> new ReadOnlyStringWrapper(reservation.getValue().results.get("airline_total_price"))
        );

        var business_total_tickets = new TableColumn<AnalyticResult<Airline>, String>("Business Total Tickets");
        business_total_tickets.setCellValueFactory(
          reservation -> new ReadOnlyStringWrapper(reservation.getValue().results.get("business_total_tickets"))
        );

        var economy_total_tickets = new TableColumn<AnalyticResult<Airline>, String>("Economy Total Tickets");
        economy_total_tickets.setCellValueFactory(
          reservation -> new ReadOnlyStringWrapper(reservation.getValue().results.get("economy_total_tickets"))
        );

        var price_2019 = new TableColumn<AnalyticResult<Airline>, String>("2019 Price");
        price_2019.setCellValueFactory(
          reservation -> new ReadOnlyStringWrapper(reservation.getValue().results.get("price_2019"))
        );

        var price_2018 = new TableColumn<AnalyticResult<Airline>, String>("2018 Price");
        price_2018.setCellValueFactory(
          reservation -> new ReadOnlyStringWrapper(reservation.getValue().results.get("price_2018"))
        );

        var price_2017 = new TableColumn<AnalyticResult<Airline>, String>("2017 Price");
        price_2017.setCellValueFactory(
          reservation -> new ReadOnlyStringWrapper(reservation.getValue().results.get("price_2017"))
        );

        table.getColumns().setAll(
          name_column, 
          airline_total_price, 
          business_total_tickets,
          economy_total_tickets,
          price_2019,
          price_2018,
          price_2017
        );

        query_results_interface.getChildren().setAll(table);
      }
      catch(SQLException e){

      }
    });

    // 3
    var top_airline_analytics_pane_button = new Button("Find Top Airline Company");
    top_airline_analytics_pane_button.setOnAction(value -> {
      try {
        query_parameter_inferface.getChildren().clear();
        query_results_interface.getChildren().clear();
        var airlines = repository.getTopAirlineBySales();
        var table = new TableView<Airline>(FXCollections.observableList(airlines));

        // Set the columns
        var name_column = new TableColumn<Airline, String>("Name");
        name_column
            .setCellValueFactory(airline -> new ReadOnlyStringWrapper(airline.getValue().name));

        table.getColumns().setAll(name_column);

        query_results_interface.getChildren().setAll(table);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });


    // 4
    var top_airline_companies_anaiytics_pane_button = new Button("Top Airline company ");
    top_airline_companies_anaiytics_pane_button.setOnAction(value -> {
      try {
        query_parameter_inferface.getChildren().clear();
        query_results_interface.getChildren().clear();
        var airlines = repository.getTopAirlineCompany();
        var table = new TableView<Airline>(FXCollections.observableList(airlines));

        // Set the columns
        var name_column = new TableColumn<Airline, String>("Name");
        name_column
            .setCellValueFactory(airline -> new ReadOnlyStringWrapper(airline.getValue().name));

        table.getColumns().setAll(name_column);

        query_results_interface.getChildren().setAll(table);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });

    // 5
    var top_frequent_flyer_analytics_pane_button = new Button("Top Frequent Flyer");
    top_frequent_flyer_analytics_pane_button.setOnAction(value -> {
      try {
        query_parameter_inferface.getChildren().clear();
        query_results_interface.getChildren().clear();
        var customers = repository.getFrequentFlyer();
        var table = new TableView<Customer>(FXCollections.observableList(customers));

        // Set the columns
        var name_column = new TableColumn<Customer, String>("Name");
        name_column
            .setCellValueFactory(customer -> new ReadOnlyStringWrapper(customer.getValue().name()));

        table.getColumns().setAll(name_column);

        query_results_interface.getChildren().setAll(table);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });

    // 6
    var platinum_customer_frequent_flyer_month_analytics_pane_button =
        new Button("Frequent Flying Month for Platinum Customers");
    platinum_customer_frequent_flyer_month_analytics_pane_button.setOnAction(value -> {
      try {
        query_parameter_inferface.getChildren().clear();
        query_results_interface.getChildren().clear();
        var months = new ArrayList<Month>();
        months.add(repository.getFrequentFlyerMonth());
        var table = new TableView<Month>(FXCollections.observableList((months)));

        // Set the columns
        var name_column = new TableColumn<Month, String>("Month");
        name_column
            .setCellValueFactory(Month -> new ReadOnlyStringWrapper(Month.getValue().name()));

        table.getColumns().setAll(name_column);

        query_results_interface.getChildren().setAll(table);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });

    // 7
    var most_reservation_analytics_pane_button = new Button("Customers who made most unpaid reservation");
    most_reservation_analytics_pane_button.setOnAction(value -> {
      try {
        query_parameter_inferface.getChildren().clear();
        query_results_interface.getChildren().clear();
        var customers = repository.getCustomerNotPaid();
        var table = new TableView<Customer>(FXCollections.observableList(customers));

        // Set the columns
        var name_column = new TableColumn<Customer, String>("Name");
        name_column
            .setCellValueFactory(customer -> new ReadOnlyStringWrapper(customer.getValue().name()));

        table.getColumns().setAll(name_column);

        query_results_interface.getChildren().setAll(table);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });

    return new HBox(
        new VBox(
          reservations_analytics_pane_button,
          sales_trends_analytics_pane_button,
          top_airline_analytics_pane_button,
          top_frequent_flyer_analytics_pane_button,
          top_airline_companies_anaiytics_pane_button,
          platinum_customer_frequent_flyer_month_analytics_pane_button,
          most_reservation_analytics_pane_button
        ),
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
            origin = repository.getAirportByCode(origin_airport_code);
          } catch (EntityNotFoundException e) {
            query_results_interface.getChildren()
                .setAll(new Label("Origin airport does not exist"));
            return;
          }


          Airport destination;
          try {
            destination = repository.getAirportByCode(destination_airport_code);
          } catch (EntityNotFoundException e) {
            query_results_interface.getChildren()
                .setAll(new Label("Destination airport does not exist"));
            return;
          }

          Airline airline;
          try {
            airline = repository.getAirlineByCode(airline_code);
          } catch (EntityNotFoundException e) {
            query_results_interface.getChildren().setAll(new Label("Airline does not exist"));
            return;
          }

          Duration depart_time;
          try {
            var depart_parts = depart_time_str.split(":");

            if (depart_parts.length != 3) {
              throw new NumberFormatException();
            }

            var hours = Integer.parseInt(depart_parts[0]);
            var minutes = Integer.parseInt(depart_parts[1]);
            var seconds = Integer.parseInt(depart_parts[2]);

            depart_time = Duration.ZERO.plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
          } catch (NumberFormatException e) {
            query_results_interface.getChildren()
                .setAll(new Label("Invalid depart_time. Please format using HH:MM:SS"));
            return;
          }

          Duration arrival_time;
          try {
            var arrival_parts = arrival_time_str.split(":");

            if (arrival_parts.length != 3) {
              throw new NumberFormatException();
            }

            var hours = Integer.parseInt(arrival_parts[0]);
            var minutes = Integer.parseInt(arrival_parts[1]);
            var seconds = Integer.parseInt(arrival_parts[2]);

            arrival_time = Duration.ZERO.plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
          } catch (NumberFormatException e) {
            query_results_interface.getChildren()
                .setAll(new Label("Invalid arrival_time. Please format using HH:MM:SS"));
            return;
          }

          double price_economy;
          try {
            price_economy = Double.parseDouble(economy_price_str);
          } catch (IllegalArgumentException e) {
            query_results_interface.getChildren()
                .setAll(new Label("Economy price must be a number"));
            return;
          }

          double price_business;
          try {
            price_business = Double.parseDouble(business_price_str);
          } catch (IllegalArgumentException e) {
            query_results_interface.getChildren()
                .setAll(new Label("Business price must be a number"));
            return;
          }

          try {
            repository.createFlightPlanFromData(origin, destination, depart_time, arrival_time,
                price_economy, price_business, airline);
          } catch (EntityAlreadyExistsException e) {
            e.printStackTrace();
            query_results_interface.getChildren()
                .setAll(new Label("A Flight Plan already exists with that data"));
          }

          query_results_interface.getChildren().setAll(new Label("Flight Plan Created"));

        } catch (SQLException e) {
          e.printStackTrace();
          query_results_interface.getChildren().setAll(new Label("Problem with database :("));
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
