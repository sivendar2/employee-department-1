Certainly! SQL injection is a common security vulnerability that occurs when untrusted input is concatenated directly into an SQL query. To mitigate this risk, it's important to use prepared statements or parameterized queries, which safely handle user input.

Below is a Java code example that demonstrates how to fix SQL injection vulnerabilities by using `PreparedStatement`:

### Original Java Code (Vulnerable to SQL Injection)

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class UserLogin {
    public boolean login(String username, String password) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        boolean isAuthenticated = false;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydatabase", "user", "password");
            stmt = conn.createStatement();
            String query = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";
            rs = stmt.executeQuery(query);

            if (rs.next()) {
                isAuthenticated = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return isAuthenticated;
    }
}
```

### Secure Java Code (Using PreparedStatement)

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserLogin {
    public boolean login(String username, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean isAuthenticated = false;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydatabase", "user", "password");
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                isAuthenticated = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return isAuthenticated;
    }
}
```

### Key Changes and Security Improvements:

1. **Use of `PreparedStatement`:** The code now uses `PreparedStatement` instead of `Statement`. This prevents SQL injection by safely handling user inputs.

2. **Parameterized Queries:** The SQL query uses placeholders (`?`) for parameters, and the actual values are set using `pstmt.setString()`. This ensures that user inputs are treated as data, not executable code.

3. **Error Handling:** While the example still prints stack traces, in a production environment, you should log errors appropriately without exposing sensitive information.

4. **Resource Management:** The `finally` block ensures that all resources (ResultSet, PreparedStatement, Connection) are closed properly to prevent resource leaks.

By following these practices, you can significantly reduce the risk of SQL injection and improve the security of your Java applications.