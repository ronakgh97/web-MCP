.PHONY: help clean compile test run package install build dev debug format check deps tree update

# Default target
help:
	@echo "Available targets:"
	@echo "  make build      - Clean and package the application"
	@echo "  make run        - Run the Spring Boot application"
	@echo "  make dev        - Run with dev profile"
	@echo "  make debug      - Run with debug enabled (port 5005)"
	@echo "  make test       - Run all tests"
	@echo "  make clean      - Clean build artifacts"
	@echo "  make compile    - Compile the project"
	@echo "  make package    - Package the application"
	@echo "  make install    - Install to local Maven repo"
	@echo "  make format     - Format code (if formatter configured)"
	@echo "  make check      - Run checkstyle/validation"
	@echo "  make deps       - Download dependencies"
	@echo "  make tree       - Show dependency tree"
	@echo "  make update     - Update dependencies"

# Clean build artifacts
clean:
	mvn clean

# Compile the project
compile:
	mvn compile

# Run tests
test:
	mvn test

# Run the application
run:
	mvn spring-boot:run

# Run with dev profile
dev:
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with debug enabled
debug:
	mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Package the application
package:
	mvn package -DskipTests

# Full build
build: clean
	mvn package

# Install to local repo
install:
	mvn install

# Format code
format:
	mvn fmt:format

# Run validation/checkstyle
check:
	mvn validate

# Download dependencies
deps:
	mvn dependency:resolve

# Show dependency tree
tree:
	mvn dependency:tree

# Update dependencies
update:
	mvn versions:display-dependency-updates
