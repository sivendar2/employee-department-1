To fix the SQL injection vulnerability in the `getEmployeesByDepartment` method, you should use parameterized queries. This can be achieved using the `JdbcTemplate`'s `query` method with a parameterized SQL statement. Here's how you can modify the method:

```java
public List<Employee> getEmployeesByDepartment(String departmentName) {
    // Use a parameterized query to prevent SQL Injection
    String sql = "SELECT * FROM employees WHERE department_id = ?";
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Employee.class), departmentName);
}
```

### Explanation:
- **Parameterized Query**: The `?` in the SQL string is a placeholder for a parameter. The actual value of `departmentName` is passed as an argument to the `query` method, which safely handles the input and prevents SQL injection.
- **BeanPropertyRowMapper**: This remains unchanged, as it is used to map the result set to the `Employee` class.

By using parameterized queries, you ensure that user input is properly escaped and handled, thus mitigating the risk of SQL injection.