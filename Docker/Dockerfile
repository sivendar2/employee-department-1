FROM eclipse-temurin:21-jdk

COPY target/employee-department.jar employee-department.jar

USER non-root
ENTRYPOINT ["java", "-jar", "/employee-department.jar"]
