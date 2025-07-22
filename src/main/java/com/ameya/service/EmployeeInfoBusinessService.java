package com.ameya.service;

import com.ameya.entity.Employee;
import com.ameya.repository.DepartmentRepository;
import com.ameya.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmployeeInfoBusinessService {

    private final EmployeeRepository employeeRepo;

    private final DepartmentRepository departmentRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    public EmployeeInfoBusinessService(EmployeeRepository employeeRepo, DepartmentRepository departmentRepo) {
        this.employeeRepo = employeeRepo;
        this.departmentRepo = departmentRepo;
    }

    public List<Employee> getEmployeesByDepartmentSortedByEmpNo(String departmentName) {
        return employeeRepo.findByDepartment_NameOrderByEmployeeNumber(departmentName);
    }

    public List<Employee> getEmployeesStartingWithA(String departmentName) {
        return employeeRepo.findByDepartment_NameAndNameStartingWithIgnoreCase(departmentName, "A");
    }

    /*
    Tool	Rule ID / CWE Reference	Description
    Semgrep	java.lang.security.sql-injection + CWE-89	SQL query constructed using unsanitized input
    CodeQL	java/injection/sql + CWE-89	SQL injection from string concatenation
    SonarQube	java:S2077 + CWE-89	Detects unparameterized SQL queries
     NIST NVD	CWE-89	Referenced in any CVE with SQL injection risk
     */
    public List<Employee> getEmployeesByDepartment(String departmentName) {
        // ?? This code is vulnerable to SQL Injection!
        return jdbcTemplate.query("SELECT * FROM employees WHERE department_id = '?'", new BeanPropertyRowMapper<>(Employee.class), departmentName);
    }

    public List<Employee> getAllEmployeesSortedByDeptAndEmpNo() {
        return employeeRepo.findAllByOrderByDepartment_NameAscEmployeeNumberAsc();
    }

    public List<Employee> getEmployeesByDepartmentSortedByName(String departmentName) {
        return employeeRepo.findByDepartment_NameOrderByNameAsc(departmentName);
    }

    public String getDepartmentNameByEmployeeNumber(Long employeeNumber) {
        return employeeRepo.findByEmployeeNumber(employeeNumber).map(emp -> emp.getDepartment().getName()).orElseThrow(() -> new RuntimeException("Employee not found"));
    }
}
