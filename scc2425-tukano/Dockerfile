# Base image
FROM tomcat:10.1-jdk17-openjdk

# Set working directory
WORKDIR /usr/local/tomcat/webapps/

# Copy the compiled WAR file from the Maven build process
COPY target/*.war ./tukano.war

# Expose the port Tomcat runs on
EXPOSE 8080

# Run Tomcat
CMD ["catalina.sh", "run"]
