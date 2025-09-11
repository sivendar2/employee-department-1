package com.ameya.service;

import java.sql.*;

public class VulnerableApp {
    public void authenticateUser(String username) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "user", "pass");
        // Vulnerable: SQL Injection here
        String query = "SELECT * FROM users WHERE username = ?";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setString(1, username);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            System.out.println("User authenticated");
        }
    }
}
