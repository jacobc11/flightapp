
  package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;

  private static final String CLEAR_USERS = "DELETE FROM Users_jacobc35";
  PreparedStatement clearUsers;

  private static final String CLEAR_RES = "DELETE FROM Reservations_jacobc35";
  PreparedStatement clearRes;

  private static final String CHECK_USER = "SELECT * FROM Users_jacobc35 WHERE username = ?";
  PreparedStatement checkUser;

  private static final String CREATE_USER = "INSERT INTO Users_jacobc35(username, hashedPassword, balance) VALUES (?, ?, ?)";
  PreparedStatement createUser;

  private static final String CHECK_PASS = "SELECT hashedPassword as pw FROM Users_jacobc35 WHERE username = ?";
  PreparedStatement checkPassword;

  private static final String DIRECT_SEARCH = 
  "SELECT TOP (?) "
  + "fid, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, day_of_month, price " 
  + "FROM FLIGHTS " 
  + "WHERE canceled = 0 AND origin_city = ? AND dest_city = ? AND day_of_month = ? "
  + "ORDER BY actual_time";
  PreparedStatement oneHop;

  private static final String IS1 = 
  "SELECT "
  + "1 AS numFlights, f1.actual_time as total," 
  + "f1.fid AS fid1, f1.day_of_month AS day_of_month1, f1.carrier_id AS carrier_id1, f1.flight_num AS flight_num1, " 
  + "f1.origin_city AS origin_city1, f1.dest_city AS dest_city1, f1.actual_time AS time1, "
  + "f1.capacity AS capacity1, f1.price AS price1, " 
  + "-1 AS fid2, -1 AS day_of_month2, NULL AS carrier_id2, -1 AS flight_num2, "
  + "NULL AS origin_city2, NULL AS dest_city2, -1 AS time2, "
  + "-1 AS capacity2, -1 AS price2 " 
  + "FROM flights AS f1 " 
  + "WHERE f1.canceled = 0 AND f1.origin_city = ? AND f1.dest_city = ? AND f1.day_of_month = ? ";

  private static final String IS2 = 
  "SELECT "
  + "2 AS numFlights, (f1.actual_time + f2.actual_time) as total, " 
  + "f1.fid AS fid1, f1.day_of_month AS day_of_month1, f1.carrier_id AS carrier_id1, f1.flight_num AS flight_num1, " 
  + "f1.origin_city AS origin_city1, f1.dest_city AS dest_city1, f1.actual_time AS time1, "
  + "f1.capacity AS capacity1, f1.price AS price1, " 
  + "f2.fid AS fid2, f2.day_of_month AS day_of_month2, f2.carrier_id AS carrier_id2, f2.flight_num AS flight_num2, " 
  + "f2.origin_city AS origin_city2, f2.dest_city AS dest_city2, f2.actual_time AS time2, "
  + "f2.capacity AS capacity2, f2.price AS price2 " 
  + "FROM Flights AS f1, Flights AS f2 " 
  + "WHERE f1.canceled = 0 AND f2.canceled = f1.canceled AND f1.dest_city = f2.origin_city "  
  + "AND f1.origin_city = ? AND f2.dest_city = ? AND f1.day_of_month = ? AND f2.day_of_month = f1.day_of_month";

  private static final String COMBO = 
  "SELECT * "
  + "FROM (SELECT TOP (?) * "
  + "FROM (" 
  + IS1 + " UNION " + IS2 + 
  ") AS allFlights ORDER BY numFlights, total) AS cutoff ORDER BY total";
  private PreparedStatement getFlights;
  
  private static final String CHECK_CAP = 
  "SELECT " 
  + "(SELECT f1.capacity FROM FLIGHTS AS f1 where f1.fid = ?) AS f1cap, "
  + "(SELECT f2.capacity FROM FLIGHTS AS f2 where f2.fid = ?) AS f2cap, "
  + "(SELECT COUNT(*) FROM Reservations_jacobc35 AS R1 where R1.fid1 = ? or R1.fid2 = ?) AS f1count, " 
  + "(SELECT COUNT(*) FROM Reservations_jacobc35 AS R2 where R2.fid1 = ? or R2.fid2 = ?) AS f2count, "
  + "(SELECT COUNT(*) FROM Reservations_jacobc35) AS currID";
  private PreparedStatement checkValid;

  private static final String ADD_RES = 
  "INSERT "
  + "INTO Reservations_jacobc35 "
  + "VALUES(?,?,?,?,?,?,?,?)";
  private PreparedStatement addRes;

  private static final String PAID = 
  "UPDATE Reservations_jacobc35 "
  + "SET paid = 1 "
  + "WHERE res_id = ?";
  private PreparedStatement setPaid;

  private static final String UPDATE_BALANCE = 
  "UPDATE Users_jacobc35 "
  + "SET balance = ? "
  + "WHERE username = ?";
  private PreparedStatement updateBalance;

  private static final String GET_RESERVATION = 
  "SELECT "
  + "R.price as price, R.day as day, R.paid as paid, U.balance as balance "
  + "FROM Reservations_jacobc35 as R, Users_jacobc35 as U "
  + "WHERE R.res_id = ? AND R.username = ? and U.username = R.username";
  private PreparedStatement getRes;

  private static final String GET_RESERVATIONS = 
  "SELECT * "
  + "FROM Reservations_jacobc35 "
  + "WHERE username = ?";
  private PreparedStatement getRess;

  private static final String GET_FLIGHT_1 = 
  "SELECT "
  + "f1.fid AS fid1, f1.day_of_month AS day_of_month1, f1.carrier_id AS carrier_id1, f1.flight_num AS flight_num1, " 
  + "f1.origin_city AS origin_city1, f1.dest_city AS dest_city1, f1.actual_time AS time1, "
  + "f1.capacity AS capacity1, f1.price AS price1 "
  + "FROM flights as f1 "
  + "WHERE f1.fid = ?"; 
  private PreparedStatement getFlight1;

  private static final String GET_FLIGHT_2 = 
  "SELECT "
  + "f1.fid AS fid1, f1.day_of_month AS day_of_month1, f1.carrier_id AS carrier_id1, f1.flight_num AS flight_num1, " 
  + "f1.origin_city AS origin_city1, f1.dest_city AS dest_city1, f1.actual_time AS time1, "
  + "f1.capacity AS capacity1, f1.price AS price1, "
  + "f2.fid AS fid2, f2.day_of_month AS day_of_month2, f2.carrier_id AS carrier_id2, f2.flight_num AS flight_num2, " 
  + "f2.origin_city AS origin_city2, f2.dest_city AS dest_city2, f2.actual_time AS time2, "
  + "f2.capacity AS capacity2, f2.price AS price2 "
  + "FROM flights as f1, flights as f2 "
  + "WHERE f1.fid = ? AND f2.fid = ?"; 
  private PreparedStatement getFlight2;

  //
  // Instance variables
  String login;
  ArrayList<Itinerary> itineraries;

  protected Query() throws SQLException, IOException {
    prepareStatements();
    itineraries = new ArrayList<>();
    login = null;
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      clearRes.clearParameters();
      clearRes.executeUpdate();
      clearUsers.clearParameters();
      clearUsers.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);
    clearUsers = conn.prepareStatement(CLEAR_USERS);
    clearRes = conn.prepareStatement(CLEAR_RES);
    checkUser = conn.prepareStatement(CHECK_USER);
    createUser = conn.prepareStatement(CREATE_USER);
    checkPassword = conn.prepareStatement(CHECK_PASS);
    oneHop = conn.prepareStatement(DIRECT_SEARCH);
    getFlights = conn.prepareStatement(COMBO);
    checkValid = conn.prepareStatement(CHECK_CAP);
    addRes = conn.prepareStatement(ADD_RES);
    setPaid = conn.prepareStatement(PAID);
    updateBalance = conn.prepareStatement(UPDATE_BALANCE);
    getRes = conn.prepareStatement(GET_RESERVATION);
    getRess = conn.prepareStatement(GET_RESERVATIONS);
    getFlight1 = conn.prepareStatement(GET_FLIGHT_1);
    getFlight2 = conn.prepareStatement(GET_FLIGHT_2);
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    if(login != null) {
      return "User already logged in\n";
    }
    try{
      checkPassword.clearParameters();
      checkPassword.setString(1, username);
      ResultSet result = checkPassword.executeQuery();

      byte[] hashWord = null;
      if(!result.next()) {
        return "Login failed\n";
      }
      hashWord = result.getBytes("pw");
      
      result.close();

      boolean matches = PasswordUtils.plaintextMatchesSaltedHash(password, hashWord);
      if(hashWord == null || !matches) {
        return "Login failed\n";
      }

      this.login = username;
      itineraries = new ArrayList<>();
      return "Logged in as " + username + "\n";
    } catch (SQLException e) {
      return "Login failed\n";
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if(initAmount < 0) {
        return "Failed to create user\n";
    }
    try {
      conn.setAutoCommit(false);
      checkUser.clearParameters();
      checkUser.setString(1, username);
      ResultSet result = checkUser.executeQuery();
      if(result.next()) {
        result.close();
        conn.rollback();
        conn.setAutoCommit(true);
        return "Failed to create user\n";
      }
      result.close();
      byte[] saltPassword = PasswordUtils.saltAndHashPassword(password);
      createUser.clearParameters();
      createUser.setString(1, username);
      createUser.setBytes(2, saltPassword);
      createUser.setInt(3, initAmount);
      createUser.executeUpdate();
      conn.commit();
      conn.setAutoCommit(true);
      return "Created user " + username + '\n';
    } catch (SQLException e) {
      try {
        conn.rollback();
        conn.setAutoCommit(true);
        if(isDeadlock(e)){
          return transaction_createCustomer(username, password, initAmount);
        }
        return "Failed to create user\n";
      } catch (SQLException x) {
        return "Failed to create user\n";
      }
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection attacks) AND only
    // handles searches for direct flights.  We are providing it *only* as an example of how
    // to use JDBC; you are required to replace it with your own secure implementation.
    //
    // TODO: YOUR CODE HERE
    StringBuffer sb = new StringBuffer();
    if(directFlight) {
      try {
        oneHop.clearParameters();
        oneHop.setInt(1, numberOfItineraries);
        oneHop.setString(2, originCity);
        oneHop.setString(3, destinationCity);
        oneHop.setInt(4, dayOfMonth);
        ResultSet oneHopResults = oneHop.executeQuery();
        itineraries = new ArrayList<>();

        int counter = 0;
        while(oneHopResults.next()) {
          int result_fid = oneHopResults.getInt("fid");
          int result_dayOfMonth = oneHopResults.getInt("day_of_month");
          String result_carrierId = oneHopResults.getString("carrier_id");
          String result_flightNum = oneHopResults.getString("flight_num");
          String result_originCity = oneHopResults.getString("origin_city");
          String result_destCity = oneHopResults.getString("dest_city");
          int result_time = oneHopResults.getInt("actual_time");
          int result_capacity = oneHopResults.getInt("capacity");
          int result_price = oneHopResults.getInt("price");

          sb.append("Itinerary " + counter + ": 1 flight(s), " + result_time + " minutes\n" 
          + "ID: " + result_fid + " Day: " + result_dayOfMonth + " Carrier: " + result_carrierId
          + " Number: " + result_flightNum + " Origin: " + result_originCity + " Dest: " + result_destCity
          + " Duration: " + result_time + " Capacity: " + result_capacity + " Price: " + result_price + "\n");

          Flight flight1  = new Flight(result_fid, result_dayOfMonth, result_carrierId, result_flightNum,
          result_originCity, result_destCity, result_time, result_capacity, result_price);
          itineraries.add(new Itinerary(counter, 1, result_time, flight1, null));
          counter+=1;
        }
        oneHopResults.close();
        if (sb.length() == 0) {
          return "No flights match your selection\n";
        }
      } catch (SQLException e) {
        return "Failed to search\n";
      }
    } else {
      try {
        getFlights.clearParameters();
        getFlights.setInt(1, numberOfItineraries);
        getFlights.setString(2, originCity);
        getFlights.setString(3, destinationCity);
        getFlights.setInt(4, dayOfMonth);
        getFlights.setString(5, originCity);
        getFlights.setString(6, destinationCity);
        getFlights.setInt(7, dayOfMonth);
        ResultSet getFlightsResults = getFlights.executeQuery();
        itineraries = new ArrayList<>();

        int counter = 0;
        while(getFlightsResults.next()) {
          int numFlights = getFlightsResults.getInt("numFlights");
          int fid1 = getFlightsResults.getInt("fid1");
          int dayOfMonth1 = getFlightsResults.getInt("day_of_month1");
          String carrier1 = getFlightsResults.getString("carrier_id1");
          String flightNum1 = getFlightsResults.getString("flight_num1");
          String originCity1 = getFlightsResults.getString("origin_city1");
          String destCity1 = getFlightsResults.getString("dest_city1");
          int time1 = getFlightsResults.getInt("time1");
          int capacity1 = getFlightsResults.getInt("capacity1");
          int price1 = getFlightsResults.getInt("price1");
          int total = getFlightsResults.getInt("total");

          sb.append("Itinerary " + counter + ": " + numFlights +" flight(s), " + total + " minutes\n" 
              + "ID: " + fid1 + " Day: " + dayOfMonth + " Carrier: " + carrier1
              + " Number: " + flightNum1 + " Origin: " + originCity1 + " Dest: " + destCity1
              + " Duration: " + time1 + " Capacity: " + capacity1 + " Price: " + price1 + "\n");

          Flight flight1 = new Flight(fid1,  dayOfMonth1,  carrier1,  flightNum1, 
            originCity1,  destCity1,  time1,  capacity1,  price1);
          Flight flight2 = null;
          
          if(numFlights == 2) {
            int fid2 = getFlightsResults.getInt("fid2");
            int dayOfMonth2 = getFlightsResults.getInt("day_of_month2");
            String carrier2 = getFlightsResults.getString("carrier_id2");
            String flightNum2 = getFlightsResults.getString("flight_num2");
            String originCity2 = getFlightsResults.getString("origin_city2");
            String destCity2 = getFlightsResults.getString("dest_city2");
            int time2 = getFlightsResults.getInt("time2");
            int capacity2 = getFlightsResults.getInt("capacity2");
            int price2 = getFlightsResults.getInt("price2");

            sb.append("ID: " + fid2 + " Day: " + dayOfMonth + " Carrier: " + carrier2
                + " Number: " + flightNum2 + " Origin: " + originCity2 + " Dest: " + destCity2
                + " Duration: " + time2 + " Capacity: " + capacity2 + " Price: " + price2 + "\n");
            flight2 = new Flight(fid2, dayOfMonth2, carrier2, flightNum2, 
            originCity2, destCity2, time2, capacity2, price2);                             
          }
          itineraries.add(new Itinerary(counter, numFlights, total, flight1, flight2));
          counter += 1;
        }
        getFlightsResults.close();
        if (sb.length() == 0) {
          return "No flights match your selection\n";
        }
      } catch (SQLException e) {
        return "Failed to search\n";
      }
    }
    return sb.toString();
  }
                                   
  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    if(login == null) {
      return "Cannot book reservations, not logged in\n";
    } else if(itineraries.size() == 0 || itineraryId >= itineraries.size()) {
      return "No such itinerary " + itineraryId +"\n";
    }
    Itinerary itin = itineraries.get(itineraryId);
    if(itin.numFlights == 1) {
      int fid1 = itin.flight1.fid;
      int day = itin.flight1.dayOfMonth;
      try {
        conn.setAutoCommit(false);
        getRess.clearParameters();
        getRess.setString(1, login);
        ResultSet getResults = getRess.executeQuery();
        ArrayList days = new ArrayList<>();
        while(getResults.next()) {
          int resDay = getResults.getInt("day");
          days.add(resDay);
        }
        getResults.close();
        if(days.contains(day)) {
          conn.rollback();
          conn.setAutoCommit(true);
          return "You cannot book two flights in the same day\n";
        }
        //check capacity
        checkValid.clearParameters();
        checkValid.setInt(1, fid1);
        checkValid.setInt(2, -1);
        checkValid.setInt(3, fid1);
        checkValid.setInt(4, fid1);
        checkValid.setInt(5, -1);
        checkValid.setInt(6, -1);

        ResultSet results = checkValid.executeQuery();
        results.next();
        int f1cap = results.getInt("f1cap");
        int f1count = results.getInt("f1count");
        int currID = results.getInt("currID") + 1;
        results.close(); 

        if(f1count + 1 > f1cap) {
          conn.rollback();
          conn.setAutoCommit(true);
          return "Booking failed\n";
        }

        //update reservations
        addRes.clearParameters();
        addRes.setInt(1, currID);
        addRes.setInt(2, 0);
        addRes.setString(3, login);
        addRes.setInt(4, fid1);
        addRes.setNull(5, java.sql.Types.INTEGER);
        addRes.setInt(6, itin.flight1.price);
        addRes.setInt(7, itin.flight1.dayOfMonth);
        addRes.setInt(8, 1);
        addRes.executeUpdate();
        conn.commit();
        conn.setAutoCommit(true);
        return "Booked flight(s), reservation ID: " + currID + "\n";
      } catch (SQLException e) {
        try {
          conn.rollback();
          conn.setAutoCommit(true);
          if(isDeadlock(e)){
            return transaction_book(itineraryId);
          }
          return "Booking failed\n";
        } catch (SQLException x) {
          return "Booking failed\n";
        }
      }
    } else {
      int fid1 = itin.flight1.fid;
      int fid2 = itin.flight2.fid;
      try {
        conn.setAutoCommit(false);
        //check capacity
        checkValid.clearParameters();
        checkValid.setInt(1, fid1);
        checkValid.setInt(2, fid2);
        checkValid.setInt(3, fid1);
        checkValid.setInt(4, fid1);
        checkValid.setInt(5, fid2);
        checkValid.setInt(6, fid2);

        ResultSet results = checkValid.executeQuery();
        results.next();
        int f1cap = results.getInt("f1cap");
        int f2cap = results.getInt("f2cap");
        int f1count = results.getInt("f1count");
        int f2count = results.getInt("f2count");
        int currID = results.getInt("currID") + 1;
        results.close(); 

        if(f1count + 1 > f1cap || f2count + 1 > f2cap) {
          conn.rollback();
          conn.setAutoCommit(true);
          return "Booking failed\n";
        }

        //update reservations
        addRes.clearParameters();
        addRes.setInt(1, currID);
        addRes.setInt(2, 0);
        addRes.setString(3, login);
        addRes.setInt(4, fid1);
        addRes.setInt(5, fid2);
        addRes.setInt(6, itin.flight1.price + itin.flight2.price);
        addRes.setInt(7, itin.flight1.dayOfMonth);
        addRes.setInt(8, 2);
        addRes.executeUpdate();
        conn.commit();
        conn.setAutoCommit(true);
        return "Booked flight(s), reservation ID: " + currID + "\n";
      } catch (SQLException e) {
        try {
          conn.rollback();
          conn.setAutoCommit(true);
          if(isDeadlock(e)){
            return transaction_book(itineraryId);
          }
          return "Booking failed\n";
        } catch (SQLException x) {
          return "Booking failed\n";
        }
      }
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    if (login == null) {
      return "Cannot pay, not logged in\n";
    }
    try {
      conn.setAutoCommit(false);
      getRes.clearParameters();
      getRes.setInt(1, reservationId);
      getRes.setString(2, login);
      ResultSet results = getRes.executeQuery();
      if(!results.next()) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Cannot find unpaid reservation " + reservationId + " under user: " + login + "\n";
      }
      int price = results.getInt("price");
      int day = results.getInt("day");
      int paid = results.getInt("paid");
      int balance = results.getInt("balance");

      if(paid == 1) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Cannot find unpaid reservation " + reservationId + " under user: " + login + "\n";
      }

      results.close();
      if(price > balance) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "User has only " + balance + " in account but itinerary costs " + price + "\n";
      }
      setPaid.clearParameters();
      setPaid.setInt(1, reservationId);
      setPaid.executeUpdate();
      updateBalance.clearParameters();
      int newBalance = balance - price;
      updateBalance.setInt(1, newBalance);
      updateBalance.setString(2, login);
      updateBalance.executeUpdate();
      conn.commit();
      conn.setAutoCommit(true);
      return "Paid reservation: " + reservationId + " remaining balance: " + newBalance + "\n";
    } catch (SQLException e) {
      try {
          conn.rollback();
          conn.setAutoCommit(true);
          if(isDeadlock(e)){
            return transaction_pay(reservationId);
          }
          return "Cannot find unpaid reservation " + reservationId + " under user: " + login + "\n";
        } catch (SQLException x) {
          return "Cannot find unpaid reservation " + reservationId + " under user: " + login + "\n";
        }
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    if(login == null) {
      return "Cannot view reservations, not logged in\n";
    }
    StringBuffer sb = new StringBuffer();
    try {
      getRess.clearParameters();
      getRess.setString(1, login);
      ResultSet results = getRess.executeQuery();
      while(results.next()) {
        int resId = results.getInt("res_id");
        int paidInt = results.getInt("paid");
        int numFlights = results.getInt("numFlights");
        int f1 = results.getInt("fid1");
        int f2 = results.getInt("fid2");
        String paid = "false";
        if(paidInt == 1) {
          paid = "true";
        }
        if(numFlights == 1) {
          getFlight1.clearParameters();
          getFlight1.setInt(1, f1);
          ResultSet f1Results = getFlight1.executeQuery();
          f1Results.next();
          int fid1 = f1Results.getInt("fid1");
          int dayOfMonth1 = f1Results.getInt("day_of_month1");
          String carrier1 = f1Results.getString("carrier_id1");
          String flightNum1 = f1Results.getString("flight_num1");
          String originCity1 = f1Results.getString("origin_city1");
          String destCity1 = f1Results.getString("dest_city1");
          int time1 = f1Results.getInt("time1");
          int capacity1 = f1Results.getInt("capacity1");
          int price1 = f1Results.getInt("price1");

          sb.append("Reservation " + resId + " paid: " + paid + ":\n" 
            + "ID: " + fid1 + " Day: " + dayOfMonth1 + " Carrier: " + carrier1
            + " Number: " + flightNum1 + " Origin: " + originCity1 + " Dest: " + destCity1
            + " Duration: " + time1 + " Capacity: " + capacity1 + " Price: " + price1 + "\n");
          f1Results.close();
        } else {
          getFlight2.clearParameters();
          getFlight2.setInt(1, f1);
          getFlight2.setInt(2, f2);
          ResultSet f2Results = getFlight2.executeQuery();
          f2Results.next();
          int fid1 = f2Results.getInt("fid1");
          int dayOfMonth1 = f2Results.getInt("day_of_month1");
          String carrier1 = f2Results.getString("carrier_id1");
          String flightNum1 = f2Results.getString("flight_num1");
          String originCity1 = f2Results.getString("origin_city1");
          String destCity1 = f2Results.getString("dest_city1");
          int time1 = f2Results.getInt("time1");
          int capacity1 = f2Results.getInt("capacity1");
          int price1 = f2Results.getInt("price1");
          int fid2 = f2Results.getInt("fid2");
          int dayOfMonth2 = f2Results.getInt("day_of_month2");
          String carrier2 = f2Results.getString("carrier_id2");
          String flightNum2 = f2Results.getString("flight_num2");
          String originCity2 = f2Results.getString("origin_city2");
          String destCity2 = f2Results.getString("dest_city2");
          int time2 = f2Results.getInt("time2");
          int capacity2 = f2Results.getInt("capacity2");
          int price2 = f2Results.getInt("price2");

          sb.append("Reservation " + resId + " paid: " + paid + ":\n" 
            + "ID: " + fid1 + " Day: " + dayOfMonth1 + " Carrier: " + carrier1
            + " Number: " + flightNum1 + " Origin: " + originCity1 + " Dest: " + destCity1
            + " Duration: " + time1 + " Capacity: " + capacity1 + " Price: " + price1 + "\n");
          sb.append("ID: " + fid2 + " Day: " + dayOfMonth1 + " Carrier: " + carrier2
            + " Number: " + flightNum2 + " Origin: " + originCity2 + " Dest: " + destCity2
            + " Duration: " + time2 + " Capacity: " + capacity2 + " Price: " + price2 + "\n");
          f2Results.close();
        }
      }
      results.close();
      if(sb.length() == 0) {
        return "No reservations found\n";
      }
      return sb.toString();
    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to view reservations\n";
    }
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   *
   * TODO(hctang): move this into QueryAbstract
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }

  class Itinerary {
    public int id;
    public int numFlights;
    public int duration;
    public Flight flight1;
    public Flight flight2;

    public Itinerary(int id, int numFlights, int duration, Flight flight1, Flight flight2) {
      this.id = id;
      this.numFlights = numFlights;
      this.duration = duration;
      this.flight1 = flight1;
      this.flight2 = flight2;
    }
  }
}