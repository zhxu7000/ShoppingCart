package au.edu.sydney.soft3202.task3;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class ShoppingController {
    private final SecureRandom randomNumberGenerator = new SecureRandom();
    private final HexFormat hexFormatter = HexFormat.of();
    private final AtomicLong counter = new AtomicLong();
    Map<String, ShoppingBasket> userBaskets;
    List<String> users = null;
    DatabaseHelper dbHelper ;

    private static final Logger logger = LoggerFactory.getLogger(ShoppingController.class);

    @Autowired
    public ShoppingController() throws SQLException {
        dbHelper = new DatabaseHelper();
        userBaskets = new HashMap<>();
        List<String> users = dbHelper.getUsers();
        for (String user : users) {
            userBaskets.put(user, loadShoppingBasket(user));
        }
    }
    public ShoppingBasket loadShoppingBasket(String user) throws SQLException {
        ShoppingBasket basket = new ShoppingBasket(user);
        if (dbHelper.userExists(user)) {
            Map<String, Integer> items = dbHelper.getItems(user);
            basket.items.putAll(items);
        }
        return basket;
    }

    @PostMapping("/login")
    public RedirectView login(@RequestParam(value = "user", defaultValue = "") String user,
                              HttpSession session, HttpServletRequest request, HttpServletResponse response) throws SQLException {
        try {
            System.out.println("Starting login for user: " + user);
            if (dbHelper.getUser("Admin") != null && user.equals("Admin")) {
                System.out.println("User is an admin");
                session.setAttribute("user","Admin");
                return new RedirectView("/users");
            } else {
                String dbUser = dbHelper.getUser(user);
                System.out.println("dbUser: " + dbUser);
                if (dbUser == null) {
                    System.out.println("User is unauthorized");
                    return new RedirectView("/unauthorized");
                }
                byte[] sessionTokenBytes = new byte[16];
                randomNumberGenerator.nextBytes(sessionTokenBytes);
                String sessionToken = hexFormatter.formatHex(sessionTokenBytes);
                session.setAttribute("user", dbUser);
                String setCookieHeaderValue = String.format("session=%s; Path=/; HttpOnly; SameSite=Strict;", sessionToken);
                response.setHeader("Set-Cookie", setCookieHeaderValue);
                return new RedirectView("/cart");
            }
        } catch (SQLException sqle) {
            return new RedirectView("/error");
        }
    }

    @GetMapping("/cart")
    public String cart(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                       Model model, HttpSession session) throws SQLException {
        String user = (String) session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }
        ShoppingBasket basket = loadShoppingBasket(user); // 调用该方法以获取购物篮
        model.addAttribute("user", user);
        model.addAttribute("shoppingBasket", basket);
        model.addAttribute("currentItems", basket.getCurrentItems());
        return "cart";
    }

    @PostMapping("/cart")
    public String updateCart(@RequestParam("counts") List<String> counts, @NotNull HttpSession session) throws SQLException  {
        String user = (String) session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }
        ShoppingBasket basket = loadShoppingBasket(user);
//        Map<String, Integer> itemsMap = new HashMap<>((Map) basket.getItems());

        List<Map.Entry<String, Integer>> items = new ArrayList<>(basket.getItems());

        // Update the shopping cart in the database
        try {
            for (int i = 0; i < items.size(); i++) {
                String item = items.get(i).getKey();
                int count = Integer.parseInt(counts.get(i));
                dbHelper.updateShoppingCart(user, item, count);
            }
        } catch (SQLException e) {
            logger.error("Error updating cart", e);
            return "error";
        }finally {
            dbHelper.close();
        }
        // Update the shopping basket in memory
        for (int i = 0; i < items.size(); i++) {
            basket.setItemCount(items.get(i).getKey(), Integer.parseInt(counts.get(i)));
        }
        return "redirect:/cart";
    }



    @GetMapping("/delname")
    public String delname(Model model, HttpSession session) throws SQLException {
        String user = (String) session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }
        ShoppingBasket basket = loadShoppingBasket(user); // 调用该方法以获取购物篮
        model.addAttribute("user", user);
        model.addAttribute("items", dbHelper.getShoppingCartItems(user));
        model.addAttribute("shoppingBasket", basket);


        return "delname";
    }

    @PostMapping("/delname")
    public String removeItem(@RequestParam(value = "itemNames", required = false) List<String> itemNames,
                             @RequestParam(value = "itemCounts", required = false) List<Integer> itemCounts,
                             RedirectAttributes redirectAttributes, HttpSession session) throws SQLException {
        String user = (String) session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }


        ShoppingBasket basket = loadShoppingBasket(user);
//        Map<String, Integer> itemsMap = new HashMap<>((Map) basket.getItems());


        List<Map.Entry<String, Integer>> allItems = new ArrayList<>(dbHelper.getShoppingCartItems(user));

        if (itemNames != null) {
            for (int i = 0; i < allItems.size(); i++) {
                String itemName = allItems.get(i).getKey();
                if (!itemNames.contains(itemName)) { // Check if the item is unchecked
                    int itemCount = itemCounts.get(i);
                    if (itemCount > 0) {
                        basket.removeItem(itemName, itemCount);
                        dbHelper.updateShoppingCartItem(user,itemName,itemCount);
                    }
                }
            }
        }
        redirectAttributes.addFlashAttribute("successMessage", "Item removed successfully.");
        return "redirect:/cart";
    }


    @GetMapping("/newname")
    public String showPage(@NotNull HttpSession session) {
        String user = (String) session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }

        return "newname";
    }

    @PostMapping("/newname")
    public String addNewName(@RequestParam("name") String name, @RequestParam("cost") double cost,
                             @NotNull HttpSession session) throws SQLException {
        String user = (String) session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }
        String lowercaseName = name.toLowerCase();
        ShoppingBasket basket = userBaskets.get(user);
//        basket.addNewItem(name, cost);
        dbHelper.addShoppingCartItem(user, lowercaseName, 0, cost);

        return "redirect:/cart";
    }

    @GetMapping("/updatename")
    public String showUPage(HttpSession session) {
        String user = (String) session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }

        return "updatename";
    }

    @PostMapping("/updatename")
    public String updatename(@RequestParam("name") String name, @RequestParam("cost") Double cost,
                             RedirectAttributes redirectAttributes, HttpSession session) throws SQLException {
        String user = (String) session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }

        ShoppingBasket basket = loadShoppingBasket(user);
        basket.updateItem(name, cost);
        redirectAttributes.addFlashAttribute("successMessage", "Item updated successfully.");

        return "redirect:/cart";
    }






    @GetMapping("/logout")
    public String logout(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                         HttpServletResponse response, HttpSession session) throws SQLException {


        String user = (String) session.getAttribute("user");
        if (user == null) {
            logger.warn("User is null during logout.");
            return "unauthorized";
        }
        // Remove user basket from memory
        userBaskets.remove(user);

        // Invalidate session
        session.invalidate();

        // Remove session cookie
        Cookie cookie = new Cookie("session", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        // If user is an admin, return logout view
        if (user.equals("Admin")) {
            logger.info("Admin user is logging out.");
            return "logout";
        }

        // If user is a regular user, return logout view
        if (dbHelper.getUser(user) != null) {
            return "logout";
        }

        // If user is not found in any table, return unauthorized
        return "unauthorized";
    }



    @GetMapping("/users")
    public String getUsers(HttpSession session,Model model) {
        try {
            dbHelper = new DatabaseHelper();
            users = dbHelper.getUsers();
            session.setAttribute("users", users);
            model.addAttribute("users", users);
            return "users";
        } catch (SQLException se) {
            model.addAttribute("errorMessage", "Unable to connect to database: " + se.getMessage());
            return "error";
        }
    }

    @PostMapping("/updateusers")
    public String updateUsers(@RequestParam(value = "userToDelete", required = false) List<String> usersToDelete,
                              @RequestParam(value = "newUser", defaultValue = "") String newUser,
                              HttpSession session) {
        try {
            dbHelper = new DatabaseHelper();
            List<String> users = dbHelper.getUsers();

            if (usersToDelete != null && !usersToDelete.isEmpty()) {
                for (String user : usersToDelete) {
                    if (!user.equals("Admin")) {
                        dbHelper.deleteUser(user);
                        users.remove(user);
                    }
                }
            }

            if (!newUser.isEmpty() && !users.contains(newUser)) {
                dbHelper.addUser(newUser);
                users.add(newUser);
            }

            session.setAttribute("users", users);
            dbHelper.commitChanges();

            return "redirect:/users";
        } catch (SQLException e) {
            return "error";
        }
    }

    @GetMapping("/newuser.html")
    public String newuser(Model model, HttpSession session) {
        // Add any necessary model attributes and logic here
        return "newuser";
    }

    @PostMapping("/deleteuser")
    public String deleteUser(@RequestParam(value = "userToDelete", required = false) List<String> userToDelete, HttpSession session) throws SQLException {

        if (userToDelete == null || userToDelete.isEmpty()) {
            session.setAttribute("message", "Please select at least one user to delete.");
            return "redirect:/users";
        }

        try {
            dbHelper = new DatabaseHelper();
            for (String user : userToDelete) {
                dbHelper.deleteUser(user);
            }
            dbHelper.commitChanges();
            session.setAttribute("message", "Selected users deleted successfully.");
        } catch (SQLException e) {
            session.setAttribute("message", "Unable to connect to database: " + e.getMessage() + ".");
        } catch (Exception e) {
            session.setAttribute("message", "Error deleting users: " + e.getMessage() + ".");
        }
        finally {
            dbHelper.close();
        }

        return "redirect:/users";
    }


    @PostMapping("/adduser")
    public String addNewUser(@RequestParam("username") String username, HttpSession session) {

        try {
            dbHelper = new DatabaseHelper();
            if (dbHelper.addUser(username)) {
                session.setAttribute("message", "User " + username + " added successfully.");
            } else {
                session.setAttribute("message", "Error adding user " + username + ". User already exists.");
            }
            dbHelper.close();
        } catch (SQLException e) {
            session.setAttribute("message", "Unable to connect to database: " + e.getMessage() + ".");
        }
        return "redirect:/users";
    }
}
