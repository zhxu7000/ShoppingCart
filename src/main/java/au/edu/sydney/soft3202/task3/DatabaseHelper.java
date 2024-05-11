package au.edu.sydney.soft3202.task3;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
@Component
public class DatabaseHelper {

  private static final String DB_NAME = "fruitbasket.db";
  private Connection connection;
  private void connect() throws SQLException {
    connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
  }

  public boolean isUserExist(String user) throws SQLException {
    String query = "SELECT COUNT(*) AS count FROM user WHERE name = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, user);
      ResultSet rs = stmt.executeQuery();
      rs.next();
      return rs.getInt("count") > 0;
    } catch (SQLException e) {
      throw new SQLException("Unable to check if user exists: " + user, e);
    }
  }
  public void ensureShoppingCartTable() throws SQLException {
    String sql = "CREATE TABLE IF NOT EXISTS shoppingcart (user TEXT NOT NULL, item TEXT NOT NULL, count INTEGER NOT NULL, cost REAL NOT NULL, PRIMARY KEY (user, item), FOREIGN KEY (user) REFERENCES users(user) ON DELETE CASCADE)";
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
  private void ensureUsersTable() throws SQLException {
    String sql =
      "CREATE TABLE IF NOT EXISTS users (user TEXT PRIMARY KEY NOT NULL)";
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
  private void deleteUsersTable() throws SQLException {
    String sql = "DROP TABLE IF EXISTS users";
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
  public boolean addUser(String name) throws SQLException {
    String sql = "INSERT INTO users (user) VALUES (?)";
    String insertSql = "INSERT INTO shoppingcart (user, item, count, cost) VALUES (?, ?, 0, ?)";
    try (
            PreparedStatement pstmt1 = connection.prepareStatement(sql);
         PreparedStatement pstmt2 = connection.prepareStatement(insertSql)) {
      pstmt1.setString(1, name);
      pstmt1.executeUpdate();
      // Initialize shopping cart items with their corresponding costs
      Map<String, Double> itemCosts = new HashMap<>();
      itemCosts.put("apple", 2.5);
      itemCosts.put("orange", 1.25);
      itemCosts.put("pear", 3.0);
      itemCosts.put("banana", 4.95);

      for (Map.Entry<String, Double> entry : itemCosts.entrySet()) {
        pstmt2.setString(1, name);
        pstmt2.setString(2, entry.getKey());
        pstmt2.setDouble(3, entry.getValue());
        pstmt2.executeUpdate();
      }
      return true;
    }
  }
  public List<String> getUsers() throws SQLException {
    String sql = "SELECT user FROM users";

    PreparedStatement preparedStatement = connection.prepareStatement(sql);
    ResultSet resultSet = preparedStatement.executeQuery();
    List<String> users = new ArrayList<String>();

    while (resultSet.next()) {
      String user = resultSet.getString("user");
      users.add(user);
    }
    return users;
  }
  public String getUser(String name) throws SQLException {

      if (name.equals("Admin")) {
        return "Admin";
      }
      String sql = "SELECT user FROM users WHERE user = ?";
    try(Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

      preparedStatement.setString(1, name);
      ResultSet resultSet = preparedStatement.executeQuery();

      while (resultSet.next()) {
        String user = resultSet.getString("user");
        return user;
      }
    }
    return null;

  }
  public void deleteUser(String user) throws SQLException{
    String sql =" DELETE FROM users WHERE user = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)){
      pstmt.setString(1,user);
      pstmt.executeUpdate();
    }
  }
  public ShoppingBasket loadShoppingBasket(String user) throws SQLException {
    ShoppingBasket basket = new ShoppingBasket(user);
    String query = "SELECT item, count FROM shoppingcart WHERE user = ?";
    try (
            PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, user);
      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        String item = rs.getString("item");
        int count = rs.getInt("count");
        if (basket.items == null) {
          basket.items = new HashMap<>();
        }
        basket.items.put(item, count);
      }
    } catch (SQLException e) {
      throw new SQLException("Unable to load shopping basket for user: " + user, e);
    }
    return basket;
  }
  public List<Map.Entry<String, Integer>> getShoppingCartItems(String user) throws SQLException {
    String sql = "SELECT item, count FROM shoppingcart WHERE user = ?";

    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, user);
      ResultSet resultSet = preparedStatement.executeQuery();

      List<Map.Entry<String, Integer>> items = new ArrayList<>();

      while (resultSet.next()) {
        String item = resultSet.getString("item");
        int count = resultSet.getInt("count");
        items.add(Map.entry(item, count));
      }
      return items;
    }
  }
  public void updateShoppingCart(String user, String item, int count) throws SQLException {
    String sql = "UPDATE shoppingcart SET count = ? WHERE user = ? AND item = ?";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
         PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, user);
      pstmt.setString(2, item);
      pstmt.setInt(3, count);
      pstmt.executeUpdate();
    }catch (SQLException e){
      System.out.println(e.getMessage());
      throw e;
    }
  }
  public void addShoppingCartItem(String user, String item, int count, double cost) throws SQLException {
    String sql = "INSERT INTO shoppingcart (user, item, count, cost) VALUES (?, ?, ?, ?)";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, user);
      pstmt.setString(2, item);
      pstmt.setInt(3, count);
      pstmt.setDouble(4, cost);

      pstmt.executeUpdate();
    }
  }



  public void deleteShoppingCartItem(String user, String item) throws SQLException {
    String sql = "DELETE FROM shoppingcart WHERE user = ? AND item = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, user);
      pstmt.setString(2, item);
      pstmt.executeUpdate();
    }
  }
  public void updateItemCost(String item, double cost) throws SQLException {
    String sql = "UPDATE shoppingcart SET cost = ? WHERE item = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setDouble(1, cost);
      pstmt.setString(2, item);
      pstmt.executeUpdate();
    }
  }
  public void updateShoppingCartItem(String user, String item, int count) throws SQLException {
    String sql = "UPDATE shoppingcart SET count = ? WHERE user = ? AND item = ?";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setInt(1, count);
      pstmt.setString(2, user);
      pstmt.setString(3, item);

      pstmt.executeUpdate();
    }
  }
  public void commitChanges() throws SQLException {
    if (connection != null && !connection.isClosed()) {
      connection.commit();
    }
  }
  public Double getPrice(String user,String item) throws SQLException {
    String sql = "SELECT cost FROM shoppingcart WHERE user =? AND item = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setString(1, user);
      pstmt.setString(2, item);

      ResultSet rs = pstmt.executeQuery();
      if (rs.next()) {
        return rs.getDouble("cost");
      } else {
        return null;
      }
    }
  }
  public List<String> getNames() throws SQLException {
    String sql = "SELECT DISTINCT item FROM shoppingcart";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      ResultSet rs = pstmt.executeQuery();
      List<String> names = new ArrayList<>();
      while (rs.next()) {
        String name = rs.getString("item");
        names.add(name);
      }
      return names;
    }
  }

  public Map<String, Double> getValues() throws SQLException {
    String sql = "SELECT DISTINCT item, cost FROM shoppingcart";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      ResultSet rs = pstmt.executeQuery();
      Map<String, Double> values = new HashMap<>();
      while (rs.next()) {
        String name = rs.getString("item");
        Double value = rs.getDouble("cost");
        values.put(name, value);
      }
      return values;
    }
  }
  public boolean userExists(String user) throws SQLException {
    String query = "SELECT COUNT(*) AS count FROM users WHERE user = ?";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, user);
      ResultSet rs = stmt.executeQuery();
      return rs.getInt("count") > 0;
    }
  }
  public Map<String, Integer> getItems(String user) throws SQLException {
    String query = "SELECT item, count FROM shoppingcart WHERE user = ?";
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, user);
      ResultSet rs = stmt.executeQuery();
      Map<String, Integer> items = new HashMap<>();
      while (rs.next()) {
        String item = rs.getString("item");
        int count = rs.getInt("count");
        items.put(item, count);
      }
      return items;
    }
  }
  public void close() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }
  public void deleteSessionToken(String user,String sessionToken) throws SQLException {
    String sql = "DELETE FROM session_tokens WHERE token = ?";

    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, user);
      preparedStatement.setString(2, sessionToken);
      preparedStatement.executeUpdate();
    }
  }



  public DatabaseHelper() throws SQLException {
    connect();
    ensureShoppingCartTable();
    ensureUsersTable();


  }
}

