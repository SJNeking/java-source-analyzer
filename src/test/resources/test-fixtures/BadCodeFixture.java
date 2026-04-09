package com.test.fixtures;

import java.util.*;
import java.io.*;

/**
 * Test fixture class containing various code smells and bugs for testing rules.
 */
public class BadCodeFixture {

    private String name;
    private int value;
    private List<String> items;

    // RSPEC-108: Empty catch block
    public void readFile() {
        try {
            FileInputStream fis = new FileInputStream("file.txt");
            fis.read();
        } catch (Exception e) {
        }
    }

    // RSPEC-4973: String literal equality
    public boolean isNameValid(String input) {
        if (input == "valid") {
            return true;
        }
        return false;
    }

    // RSPEC-107: Too many parameters
    public void process(String a, String b, String c, String d, String e, String f, String g) {
        System.out.println(a + b + c + d + e + f + g);
    }

    // RSPEC-1142: Too many return paths
    public int getStatus(int code) {
        if (code == 1) return 100;
        if (code == 2) return 200;
        if (code == 3) return 300;
        if (code == 4) return 400;
        if (code == 5) return 500;
        return 0;
    }

    // RSPEC-2675: Boolean method naming
    public boolean getStatus() {
        return true;
    }

    // RSPEC-1444: Public static mutable field
    public static List<String> publicList = new ArrayList<>();

    // RSPEC-2208: Wildcard import
    // (This would be at file level, not method level)

    // RSPEC-108: Another empty catch
    public void loadData() {
        try {
            doSomething();
        } catch (RuntimeException e) {
            // ignore
        }
    }

    private void doSomething() {
        // placeholder
    }

    // RSPEC-2259: Null dereference
    public void processString(String input) {
        String s = null;
        s.length();
        if (input != null) {
            input.trim();
        }
    }

    // RSPEC-1643: String concatenation in loop
    public String buildMessage(List<String> messages) {
        String result = "";
        for (String msg : messages) {
            result += msg + " ";
        }
        return result;
    }

    // RSPEC-1764: Identical operand
    public boolean checkValue(int x) {
        if (x == x) {
            return true;
        }
        return false;
    }

    // RSPEC-1166: Exception ignored
    public void parseData(String data) {
        try {
            Integer.parseInt(data);
        } catch (NumberFormatException e) {
        }
    }

    // RSPEC-4434: Access control not implemented
    public void adminAction(String userInput) {
        // performs admin action without auth check
        processInput(userInput);
    }

    private void processInput(String input) {
        System.out.println("Processing: " + input);
    }

    // RSPEC-5659: Insecure JWT validation
    public String validateToken(String token) {
        return token.split("\\.")[1];
    }

    // RSPEC-1205: Feature envy
    public int calculateTotal(Order order) {
        return order.getPrice() * order.getQuantity() + order.getTax() + order.getShipping();
    }
}

class Order {
    private int price;
    private int quantity;
    private double tax;
    private double shipping;

    public int getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public double getTax() { return tax; }
    public double getShipping() { return shipping; }
}
