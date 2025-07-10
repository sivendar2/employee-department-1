package com.ameya.service;

import java.sql.*;

public class VulnerableApp {
    public void authenticateUser(String username) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "user", "pass");
        // Vulnerable: SQL Injection here
        String sql = "SELECT * FROM users WHERE username = '" + username + "'";
        PreparedStatement stmt = conn.createPreparedStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            System.out.println("User authenticated");
        }
    }
}
