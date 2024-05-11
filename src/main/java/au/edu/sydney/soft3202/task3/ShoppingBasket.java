package au.edu.sydney.soft3202.task3;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ShoppingBasket {
    String user;
    HashMap<String, Integer> items;
    HashMap<String, Double> values ;
    DatabaseHelper dbHelper;
    Map<String, Double> newItem = new HashMap<>();
    public ShoppingBasket(String user) throws SQLException {
        this.user = user;
        this.items = new HashMap<>();
        this.values= new HashMap<>();
        this.items.put("apple", 0);
        this.items.put("orange", 0);
        this.items.put("pear", 0);
        this.items.put("banana", 0);
        dbHelper =new DatabaseHelper();
    }

    public void addItem(String item, int count) throws IllegalArgumentException, SQLException {
        if (item == null)
            throw new IllegalArgumentException("Item is invalid");
        String stringItem = item.toLowerCase();
        if (!this.items.containsKey(stringItem))
            throw new IllegalArgumentException("Item " + stringItem + " is not present.");
        if (count < 1)
            throw new IllegalArgumentException("Item " + item + " has invalid count.");
        Integer itemVal = this.items.get(stringItem);
        if (itemVal == Integer.MAX_VALUE)
            throw new IllegalArgumentException("Item " + item + " has reached maximum count.");

        this.items.put(stringItem, itemVal + count);
        dbHelper.updateShoppingCartItem(user, item, itemVal + count);
    }


    public void updateItemCount(String name, int count) throws SQLException {
        String lowercaseName = name.toLowerCase();

        if (items.containsKey(lowercaseName)) {
            items.put(lowercaseName, count);

            // Update the item count in the shopping cart table in the dbHelper
            try {
                dbHelper.updateShoppingCart(user, lowercaseName, count);
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public boolean removeItem(String item, int count) throws IllegalArgumentException, SQLException {
        if (item == null) throw new IllegalArgumentException("Item is invalid");
        String sitem = item.toLowerCase();
        if (!this.items.containsKey(sitem)) return false;
        if (count < 1) throw new IllegalArgumentException(count + " is invalid count.");

        Integer itemVal = this.items.get(sitem);

        Integer newVal = itemVal - count;
        if (newVal < 0) return false;
        this.items.put(sitem, newVal);
        dbHelper.updateShoppingCart(user, sitem, newVal);
        return true;
    }

    public List<Entry<String, Integer>> getItems() throws SQLException {
        return dbHelper.getShoppingCartItems(user);
    }

    public void addNewItem(String name, double cost) throws SQLException {
        String lowercaseName = name.toLowerCase();

        if (!items.containsKey(lowercaseName)) {
            items.put(lowercaseName, 0);
            values.put(lowercaseName, cost);// add the new item's cost
            newItem.put(lowercaseName,cost);
            // Add the new item to the shopping cart table in the dbHelper
            if (dbHelper == null) {
                System.out.println("dbHelper is null");
            }
            try {
                dbHelper.addShoppingCartItem(user, lowercaseName, 0, cost);
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public void updateItem(String name, double cost) throws IllegalArgumentException, SQLException {
        String lowercaseName = name.toLowerCase();
        if (items.containsKey(lowercaseName)) {
            values.put(lowercaseName, cost);
            // Update the cost of the item in the shopping cart table in the dbHelper
            try {
                dbHelper.updateItemCost(lowercaseName, cost);
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        } else if (newItem.containsKey(lowercaseName)) {
            newItem.put(lowercaseName, cost);
            // Update the cost of the item in the shopping cart table in the dbHelper
            try {
                dbHelper.updateItemCost(lowercaseName, cost);
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public Map<String, Integer> getCurrentItems() throws IllegalArgumentException,SQLException {
        Map<String, Integer> currentItems = new HashMap<>();
        for (String name: dbHelper.getNames()) {
            currentItems.put(name, this.items.get(name));
        }
        for (Entry<String, Double> entry : newItem.entrySet()) {
            currentItems.put(entry.getKey(), 0);
        }
        // Get the items in the shopping cart table from the dbHelper and update the counts
        try {
            ShoppingBasket dbBasket = dbHelper.loadShoppingBasket(user);
            for (Entry<String, Integer> entry : dbBasket.getItems()) {
                String item = entry.getKey();
                int count = entry.getValue();
                if (currentItems.containsKey(item)) {
                    currentItems.put(item, currentItems.get(item) + count);
                } else {
                    currentItems.put(item, count);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return currentItems;
    }

    public Double getValue(String user) throws SQLException {
        Double val = 0.0;

        for (String name : items.keySet()) {
            Double price = dbHelper.getPrice(user, name);
            if (price != null) {
                val += price * items.get(name);
            }
        }
        if (val == 0.0) return null;
        return val;
    }

    public void clear () throws SQLException {
        for (String name : dbHelper.getNames()) {
            this.items.put(name, 0);
            dbHelper.updateShoppingCartItem(user, name, 0);
        }
    }

    public void setItemCount (String item,int count) throws IllegalArgumentException, SQLException {
        if (item == null) throw new IllegalArgumentException("Item is invalid");
        String stringItem = item.toLowerCase();

        if (!items.containsKey(stringItem))
            throw new IllegalArgumentException("Item " + stringItem + " is not present.");
        if (count < 0) throw new IllegalArgumentException("Item " + item + " has invalid count.");

        items.put(stringItem, count);
        // Update the count of the item in the shopping cart table in the dbHelper
        try {
            dbHelper.updateShoppingCartItem(user, stringItem, count);
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void setInitialItems(Map<String, Integer> initialItems) {
        this.items.putAll(initialItems);
    }

}