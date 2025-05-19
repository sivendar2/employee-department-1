
Detail Explanantion of the project -

The EmployeeController is a Spring Boot REST controller that exposes multiple GET endpoints to retrieve employee information based on department and sorting criteria. It delegates the core business logic to the EmployeeInfoBusinessService.

1. Get employees in a department, sorted by name
URL: /by-department/{departmentName}

Description: Returns a list of employees in the specified department, sorted alphabetically by employee name.

Delegates to: getEmployeesByDepartmentSortedByName(departmentName)

2. Get department name by employee number
URL: /{employeeNumber}/department

Description: Returns the department name for the given employee number.

Delegates to: getDepartmentNameByEmployeeNumber(employeeNumber)

3. Get employees by department, sorted by employee number
URL: /department/{departmentName}/sorted-by-id

Description: Returns employees in the specified department, sorted by employee number.

Delegates to: getEmployeesByDepartmentSortedByEmpNo(departmentName)

4. Get employees in a department whose names start with 'A'
URL: /department/{departmentName}/starting-with-a

Description: Returns employees in the specified department whose names start with the letter 'A'.

Delegates to: getEmployeesStartingWithA(departmentName)

5. Get all employees sorted by department and employee number
URL: /sorted-by-department-and-id

Description: Returns all employees sorted first by department name and then by employee number.

Delegates to: getAllEmployeesSortedByDeptAndEmpNo()

✅ Use Case Summary
Provides flexible query options to retrieve employee data based on:

Department name

Sorting by employee name or number

Filtering names starting with a specific character (A)



Deployment steps 
================
Package your Spring Boot app into a .jar.

Push it to ECR or upload it manually.

Use the AWS Console to:

Create a Fargate task definition

Setup an ECS service

Connect to ALB, VPC, subnets, etc.