package csc450.airline.controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    }

    ;

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

  public ArrayList<Reservation> findReservationsByCustomer(Customer customer) throws SQLException{
    var statement = this.connection.prepareStatement(String.join(" ",
      "SELECT",
      Reservation.selects(),
      "FROM",
      Reservation.table(),
      Reservation.joins(),
      "WHERE reservation.customer_id = ?"
    ));

    statement.setInt(1, customer.id);
    var rows = statement.executeQuery() ;

    var results = new ArrayList<Reservation>();
    while (rows.next()) {
      results.add(new Reservation(rows));
    }

    statement.close();

    return results;   
  }

  public void payForReservation(Reservation reservation, String billingAddress) throws SQLException{
    var statement = this.connection.prepareStatement(String.join(" ", 
      "UPDATE",
      Reservation.table(),
      "SET",
      "billing_address = ?,",
      "has_paid = 'Y'",
      "WHERE reservation_id = ?"
    ));

    statement.setString(1, billingAddress);
    statement.setInt(2, reservation.id);

    statement.executeUpdate();
  }

  public void createReservationFromData(Customer customer, FlightInstance flightInstance, Integer tix_business, Integer tix_economy)
  throws SQLException, EntityAlreadyExistsException {
    
    var statement = this.connection.prepareStatement(String.join(" ",
      "INSERT INTO reservation",
      "(flight_id, flight_date, customer_id, tickets_business, tickets_economy, price, has_paid)",
      "VALUES (?, ?, ?, ?, ?, ?, 'N')"
    ));

    statement.setInt(1, flightInstance.flight_plan.id);
    statement.setDate(2, flightInstance.date);
    statement.setInt(3, customer.id);
    statement.setInt(4, tix_business);
    statement.setInt(5, tix_economy);
    
    // price
    statement.setInt(
      6,
      flightInstance.price_business * tix_business +
      flightInstance.price_economy * tix_economy
    );

    statement.executeUpdate();
    
  }
  public ArrayList<FlightInstance> findFlightInstanceByAirportsAndDate(Airport origin, Airport destination, Date date) throws SQLException {
    var statement = this.connection.prepareStatement(String.join(" ", 
      "SELECT",
      FlightInstance.selects(),
      "FROM",
      FlightInstance.table(),
      FlightInstance.joins(),
      "WHERE flight_schedule.origin_airport_code = ?",
      "AND flight_schedule.destination_airport_code = ?",
      "AND flight.flight_date = ?",
      "AND flight.can_book = 'Y'"
    ));

    statement.setString(1, origin.code);
    statement.setString(2, destination.code);

    statement.setDate(3, date);

    var rows = statement.executeQuery();

    var results = new ArrayList<FlightInstance>();
    while (rows.next()) {
      results.add(new FlightInstance(rows));
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
      + "values(?, ?, ?, ?, ?, ?, ?)"
    );

    statement.setString(1, origin.code);
    statement.setString(2, destination.code);

    // Intervals have stupid syntax
    statement.setString(
      3,
      "0 " + String.join(":",
        String.format("%02d", depart_time.toHours()),
        String.format("%02d", depart_time.toMinutesPart()),
        String.format("%02d", depart_time.toSecondsPart())
      )
    );
    statement.setString(
      4,
      "0 " + String.join(":",
        String.format("%02d", arrival_time.toHours()),
        String.format("%02d", arrival_time.toMinutesPart()),
        String.format("%02d", arrival_time.toSecondsPart())
      )
    );

    statement.setDouble(5, price_economy);
    statement.setDouble(6, price_business);

    statement.setString(7, airline.code);
    
    statement.executeUpdate();
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

      if (rows.next()) {
          return new Airline(rows);
      } else {
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


  public ArrayList<AnalyticResult<Airline>> getAirlineAnalytics() throws SQLException {
    var statement = this.connection.prepareStatement(String.join(" ",
      "With Big_table as (",
      "  select al.AIRLINE_CODE, al.NAME, f.FLIGHT_ID, f.FLIGHT_DATE, r.RESERVATION_ID, r.CUSTOMER_ID, r.TICKETS_BUSINESS, r.TICKETS_ECONOMY, r.PRICE, r.HAS_PAID",
      "      from airline al",
      "      inner join FLETCHERT1.flight_schedule fs",
      "        on al.airline_code = fs.airline_code",
      "      inner join fletchert1.flight f",
      "        on fs.flight_id = f.flight_id",
      "      inner join fletchert1.reservation r", "            on f.flight_id = r.flight_id",
      "          and f.flight_date = r.flight_date),", "Airline_Price as (",
      "  select airline_code, sum(price) as Airline_Total_Price", "   from big_table",
      "  where has_paid = 'Y'", "   group by airline_code),", "Eco_Class_Price as (",
      "  select airline_code, sum(tickets_economy) as Economy_Total_Tickets",
      "  from big_table", "    where has_paid = 'Y'", "    group by airline_code),",
      "Bus_Class_Price as (",
      "    select airline_code, sum(tickets_business) as Business_Total_Tickets",
      "    from big_table", "    where has_paid = 'Y'", "    group by airline_code),",
      "Price_2019 as (", "    select airline_code, sum(price) as Year_2019", "    from big_table",
      "    where has_paid = 'Y' and extract(year from flight_date) = 2019",
      "    group by airline_code),", "Price_2018 as (",
      "    select airline_code, sum(price) as Year_2018", "    from big_table",
      "    where has_paid = 'Y' and extract(year from flight_date) = 2018",
      "    group by airline_code),", "Price_2017 as (",
      "    select airline_code, sum(price) as Year_2017", "    from big_table",
      "    where has_paid = 'Y' and extract(year from flight_date) = 2017",
      "    group by airline_code)",
      "SELECT",
      Airline.selects(),",",
      "  airline_Total_price, business_Total_tickets,",
      "  economy_Total_tickets, nvl(Year_2019, 0) as Price_2019,",
      "  nvl(Year_2018, 0) as Price_2018,",
      "  nvl(Year_2017, 0) as Price_2017",
      "from airline_price ",
      "inner join airline",
      "  ON airline.airline_code = airline_price.airline_code",
      Airline.joins(),
      "full outer join eco_class_price ",
      "  on airline_price.airline_code = eco_class_price.airline_code",
      "full outer join bus_class_price",
      "  on bus_class_price.airline_code = eco_class_price.airline_code",
      "full outer join price_2019 ",
      "  on bus_class_price.airline_code = price_2019.airline_code",
      "full outer join price_2018",
      "  on bus_class_price.airline_code = Price_2018.airline_code",
      "full outer join price_2017",
      "  on bus_class_price.airline_code = Price_2017.airline_code"
    ));

      var rows = statement.executeQuery();

      var results = new ArrayList<AnalyticResult<Airline>>();
      while (rows.next()) {
          var airline = new Airline(rows);

          var result = new HashMap<String,String>();
          result.put("airline_total_price",    Integer.toString(rows.getInt("airline_Total_price")));
          result.put("business_total_tickets", Integer.toString(rows.getInt("business_Total_tickets")));
          result.put("economy_total_tickets",  Integer.toString(rows.getInt("economy_Total_tickets")));
          result.put("price_2019",             Integer.toString(rows.getInt("Price_2019")));
          result.put("price_2018",             Integer.toString(rows.getInt("Price_2018")));
          result.put("price_2017",             Integer.toString(rows.getInt("Price_2017")));

          results.add(new AnalyticResult<Airline>(airline, result));
      }
      statement.close();

      return results;
  }

  //3
  public ArrayList<Airline> getTopAirlineBySales()throws SQLException {
      var statement = this.connection.prepareStatement(String.join(" ",
          "with flight_total_price as ( ",
              "    select flight_id,  sum(price) as price ",
              "    from reservation ",
              "    where flight_date > add_months(SYSDATE, - 12) ",
              "    group by flight_id), ",
              " airline_total_price as ( ",
               "    select airline_code, sum(price) as price ",
              "    from flight_total_price join flight_schedule ",
              "    on flight_total_price.flight_id = flight_schedule.flight_id ",
              "    group by airline_code",")",
               "SELECT ",
          Airline.selects(), "FROM", Airline.table(), Airline.joins(),
          "INNER JOIN airline_total_price",
          "ON airline_total_price.airline_code = airline.airline_code",
          "where price = ( ",
          "    select max(price) ",
          "    from airline_total_price)"));

      var rows = statement.executeQuery();

      var results = new ArrayList<Airline>();
      while (rows.next()) {
          results.add(new Airline(rows));

      }
      statement.close();

      return results;
  }

  //4
  public ArrayList<Airline> getTopAirlineCompany()throws SQLException {
      var statement = this.connection.prepareStatement(String.join(" ",
          "WITH occupancy_by_airline AS (" ,
          "  SELECT ",
          "    fs.airline_code,",
          "   (ROUND(1 - SUM(seats_free_economy + seats_free_business) / SUM(seats_available_economy + seats_available_business), 4)) * 100 AS occupancy ",
          "  FROM flight_view fv ",
          "  INNER JOIN flight f ",
          "    ON f.flight_id = fv.flight_id ",
          "  AND f.flight_date = fv.flight_date ",
          "  INNER JOIN aircraft ac",
          "    ON f.aircraft_id = ac.aircraft_id ",
          "  INNER JOIN flight_schedule fs ",
          "    ON fs.flight_id = fv.flight_id ",
          "  WHERE f.flight_date > add_months(SYSDATE, -12)",
          "  GROUP BY fs.airline_code",
          ")",
          "SELECT ",
          Airline.selects(), "FROM", Airline.table(), Airline.joins(),
          "INNER JOIN occupancy_by_airline",
          "  ON occupancy_by_airline.airline_code = airline.airline_code",
          "WHERE occupancy = (",
          "  SELECT MAX(occupancy)",
          "FROM occupancy_by_airline)"));
      var rows = statement.executeQuery();

      var results = new ArrayList<Airline>();
      while (rows.next()) {
          results.add(new Airline(rows));

      }
      statement.close();

      return results;
  }


  //5
  public ArrayList<Customer> getFrequentFlyer()throws SQLException {
      var statement = this.connection.prepareStatement(String.join(" ",
          "WITH past_year_customer_view as ( ",
          "  SELECT ",
          "    c.customer_id, ",
          "    c.first_name, ",
          "    c.last_name, ",
          "    SUM(f.distance) as distance ",
          "  from customer c ",
          "  inner join reservation r ",
          "    on r.customer_id = c.customer_id",
          "  INNER JOIN flight_view f ",
          "    on f.flight_id = r.flight_id ",
          "  WHERE r.flight_date > add_months(SYSDATE, -12) ",
          "  GROUP BY c.customer_id, c.first_name, c.last_name ",
          ")",
          "SELECT ",
          Customer.selects(), "FROM", Customer.table(), Customer.joins(),
          "INNER JOIN past_year_customer_view",
          "on past_year_customer_view.customer_id = customer.customer_id ",
          "WHERE distance = ( ",
              "  SELECT MAX(distance)",
              "  FROM past_year_customer_view )"));
      var rows = statement.executeQuery();

      var results = new ArrayList<Customer>();
      while (rows.next()) {
          results.add(new Customer(rows));

      }
      statement.close();

      return results;
  }

  //6
  public Month getFrequentFlyerMonth()throws SQLException {
      var statement = this.connection.prepareStatement(String.join(" ",
        "With tix_by_month as (",
        "  SELECT",
        "    to_char(flight_date, 'Month') as Month,",
        "    sum(r.tickets_business + r.tickets_economy) as Tickets ",
        "  from customer_view c ",
        "  inner join fletchert1.reservation r ",
        "  on r.customer_id = c.customer_id ",
        "  where c.mileage_club = 'Platinum' ",
        "  group by to_char(flight_date, 'Month') ",
        ")",
        "SELECT ",
        "month",
        "from tix_by_month",
        "where tickets = ( ",
        "select max(tickets) ",
         "from tix_by_month) "
        ));
      var rows = statement.executeQuery();
      rows.next();

      var month_str = rows.getString("Month").toUpperCase().trim();
      
      return Month.valueOf(month_str);
  }

  //7
  public ArrayList<Customer> getCustomerNotPaid()throws SQLException {
      var statement = this.connection.prepareStatement(String.join(" ",
        "With customer_total_not_paid as ( ",
        "  select customer_id, count(customer_id) as count_tickets ",
        "  from reservation ",
        "  where has_paid = 'N' ",
        "  group by customer_id",
        "  ),",
        "  customer_max as ( ",
        "    select customer_id",
        "    from customer_total_not_paid ",
        "    where count_tickets = ( ",
        "      select max(count_tickets) ",
        "      from customer_total_not_paid",
        "    )",
        "    group by customer_id",
        ")",
        "SELECT ",
        Customer.selects(), "FROM", Customer.table(), Customer.joins(),
        "INNER JOIN customer_total_not_paid",
        "on customer_total_not_paid.customer_id = customer.customer_id ",
        "where customer.customer_id = (",
        "  select customer_id ",
        " from customer_max",
        ")"
      ));
      var rows = statement.executeQuery();

      var results = new ArrayList<Customer>();
      while (rows.next()) {
          results.add(new Customer(rows));

      }
      statement.close();
      return results;
  }
}