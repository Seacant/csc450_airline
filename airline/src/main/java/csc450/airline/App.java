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
import java.time.LocalDate;
import java.sql.Date;

/**
 * Hello world!
 */
public final class App extends Application {
    private Repository repository;
    private Stage stage;

    public void start(Stage stage) throws SQLException{
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
        flight_management_button.setOnAction(value -> this.stage.getScene().setRoot(this.flightManagementPage()));

        return new VBox(
            reservation_button,
            analytics_button,
            flight_management_button
        );
    }

    public Parent reservationPage(){
        // First, we need the user to 'log in' ;)
        try {
            ObservableList<Customer> users = FXCollections.observableList(
                this.repository.listCustomers()
            );

            users.addListener((Change<? extends Customer> c) -> System.err.println("Test?"));

            return new HBox(new ListView<Customer>(users));
        }
        catch(SQLException e){
            return new HBox(new Label("Unable to connect to DB :("));
        }
    }


    @SuppressWarnings("unchecked") // Don't ask :(
    public Parent analyticsPage(){
        var query_parameter_inferface = new VBox();
        var query_results_interface = new VBox();

        var reservations_analytics_pane_button = new Button("Find Customers on a flight");
        reservations_analytics_pane_button.setOnAction(value -> {
            final var flightIdInput = new TextField();
            final var flightDateInput = new TextField();

            var submitButton = new Button("Submit");
            submitButton.setOnAction(nop -> {
                int flight_id;
                try {
                    flight_id = Integer.parseInt(flightIdInput.getText());
                }
                catch(NumberFormatException e){
                    query_results_interface.getChildren().setAll(
                        new Label("Invalid value provided for Flight ID")
                    );
                    return;
                }


                Date flight_date;
                try {
                    flight_date = Date.valueOf(flightDateInput.getText());
                }
                catch(IllegalArgumentException e){
                    query_results_interface.getChildren().setAll(
                        new Label("Invalid date format. Please use yyyy-mm-dd")
                    );
                    return;
                }

                FlightInstance flight_instance;
                try {
                    flight_instance = repository.getFlightInstanceByIdAndDate(flight_id, flight_date);
                }
                catch(SQLException e){
                    e.printStackTrace();
                    query_results_interface.getChildren().setAll(
                        new Label("Database Error :(")
                    );
                    return;
                }
                catch(EntityNotFoundException e){
                    query_results_interface.getChildren().setAll(
                        new Label("Flight not found")
                    );
                    return;
                }

                try {
                    var reservations = repository.getReservationsByFlightInstance(flight_instance);
                    var table = new TableView<>(FXCollections.observableList(reservations));

                    // Set the columns
                    var name_column = new TableColumn<Reservation, String>("Name");
                    name_column.setCellValueFactory(reservation -> new ReadOnlyStringWrapper(reservation.getValue().customer.name()));

                    var tickets_business_column = new TableColumn<Reservation, Number>("Business Tix");
                    tickets_business_column.setCellValueFactory(reservation -> new ReadOnlyIntegerWrapper(reservation.getValue().tickets_business));

                    var tickets_economy_column = new TableColumn<Reservation, Number>("Economy Tix");
                    tickets_economy_column.setCellValueFactory(reservation -> new ReadOnlyIntegerWrapper(reservation.getValue().tickets_economy));

                    var price_column = new TableColumn<Reservation, Number>("Price Paid");
                    price_column.setCellValueFactory(reservation -> new ReadOnlyIntegerWrapper(reservation.getValue().price));

                    table.getColumns().setAll(
                        name_column,
                        tickets_business_column,
                        tickets_economy_column,
                        price_column
                    );

                    query_results_interface.getChildren().setAll(table);

                }
                catch(SQLException e){
                    e.printStackTrace();
                    query_results_interface.getChildren().setAll(
                        new Label("Database Error :(")
                    );
                    return;
                }
            });

            query_parameter_inferface.getChildren().setAll(
                new HBox(
                    new Label("Flight ID:"),
                    flightIdInput
                ),
                new HBox(
                    new Label("Flight Date:"),
                    flightDateInput
                ),
                submitButton
            );
        });

        return new HBox(
            new VBox(
                reservations_analytics_pane_button
            ),
            new VBox(query_parameter_inferface, query_results_interface)
        );
    }

    public Parent flightManagementPage(){
        var query_parameter_inferface = new VBox();
        var query_results_interface = new VBox();

        var new_flight_button = new Button("Add new flight");
        var fill_flights_button = new Button("Fill flights using plan");

        new_flight_button.setOnAction(value -> {
            final var origin_airport_code_input = new TextField();
            query_parameter_inferface.getChildren().setAll();
        });

        fill_flights_button.setOnAction(value -> {
            try {
                var flight_plans = repository.listFlightPlans();

                var start = LocalDate.now();
                var end = LocalDate.now().plusDays(30);

                start.datesUntil(end).forEach(date -> {
                    for (var flight_plan: flight_plans){
                        try{
                            repository.createFlightInstanceFromFlightPlanAndDate(flight_plan, Date.valueOf(date));
                        }
                        catch(EntityAlreadyExistsException e){
                            // This is expected. Do nothing.
                        }
                        catch(SQLException e){
                            e.printStackTrace();
                            query_results_interface.getChildren().setAll(
                                new Label("Unknown error inserting flight instance. Bailing")
                            );
                        }
                    }
                });

                query_results_interface.getChildren().setAll(
                    new Label("Successfully inserted new flights")
                );
            }
            catch(SQLException e){
                e.printStackTrace();
                query_results_interface.getChildren().setAll(
                    new Label("Database Error :(")
                );
            }
        });

        return new HBox(
            new VBox(new_flight_button, fill_flights_button),
            new VBox(query_parameter_inferface, query_results_interface)
        );
    }
}
