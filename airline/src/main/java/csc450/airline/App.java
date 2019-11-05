package csc450.airline;

import csc450.airline.controllers.Repository;
import csc450.airline.models.*;

import java.sql.SQLException;

/**
 * Hello world!
 */
public final class App {
    private App() {
    }

    /**
     * Says hello to the world.
     * @param args The arguments of the program.
     */
    public static void main(String[] args) throws SQLException{
        var repo = new Repository();
        var customers = repo.listCustomers();

        System.out.println(customers.get(0).name());
    }
}
