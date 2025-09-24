# Glossary

This glossary defines the key terms and concepts used throughout the SASS (Spring Boot Application with Security System) project.

## A

### API (Application Programming Interface)
A set of rules and protocols that allows different software applications to communicate with each other. In SASS, we use RESTful APIs and GraphQL endpoints.

### Architecture Decision Record (ADR)
A document that captures an important architectural decision made along with its context and consequences.

### Authentication (AuthN)
The process of verifying the identity of a user or system. In SASS, this is handled through OAuth2 and session-based authentication.

### Authorization (AuthZ)
The process of determining what level of access an authenticated user has to specific resources or functions.

## B

### Backend
The server-side of the application, implemented using Java Spring Boot. Handles business logic, data access, and API endpoints.

### Bounded Context
A term from Domain-Driven Design (DDD) that describes a specific responsibility or area of the application. In SASS, we have bounded contexts for authentication, payment, subscription, and user management.

## C

### CLI (Command Line Interface)
A text-based interface for interacting with the system through commands. SASS includes various CLI tools for development and operations.

### Constitutional Development
A development approach that emphasizes following a project constitution with non-negotiable rules and practices.

### Continuous Integration (CI)
The practice of automatically building and testing code changes whenever they are committed to the repository.

### Continuous Deployment (CD)
The practice of automatically deploying code changes to production after they pass all tests and quality checks.

## D

### Dual-Stack Architecture
The architectural approach used in SASS that combines an Application Stack (Java/TypeScript) with a Constitutional Tools Stack (Python).

### Domain-Driven Design (DDD)
An approach to software development that focuses on the core domain and domain logic, with complex designs based on models of that domain.

## E

### Event-Driven Architecture
An architectural style promoting the production, detection, consumption of, and reaction to events. Used in SASS for communication between modules and agents.

### E2E Testing (End-to-End Testing)
Testing technique that evaluates the complete flow of an application from start to finish, simulating real user scenarios.

## F

### Frontend
The client-side of the application, implemented using React and TypeScript. Provides the user interface and user experience.

## I

### Integration Testing
A level of software testing where individual software modules are combined and tested as a group.

### Inversion of Control (IoC)
A design principle in which the control of objects or portions of a program is transferred to a framework or runtime.

### Issue
A task, enhancement, or bug that needs to be addressed in the software. Managed through GitHub Issues in the SASS project.

## L

### License
The legal terms under which the SASS software is distributed. See the LICENSE file for details.

## M

### Microservice
A software architecture pattern in which a single application is composed of small, independent services that communicate over well-defined APIs.

### Modular Architecture
An architectural approach that structures the application as a set of cohesive modules with well-defined interfaces and dependencies.

### Module
A self-contained unit of functionality within the application. In SASS, we use Spring Modulith for modular architecture.

## O

### OAuth2
An authorization framework that enables applications to obtain limited access to user accounts on an HTTP service. Used in SASS for authentication.

### OpenAPI
A specification for machine-readable interface files for describing, producing, consuming, and visualizing RESTful web services.

### OWASP
The Open Web Application Security Project, an online community that produces freely available articles, methodologies, documentation, tools, and technologies in the field of web application security.

## P

### Payment Platform
The core business functionality of SASS that handles payment processing, subscription management, and financial transactions.

### PCI DSS
The Payment Card Industry Data Security Standard, a set of security standards designed to ensure that all companies that accept, process, store or transmit credit card information maintain a secure environment.

### Pre-commit Hook
A script that runs automatically before a commit is made to the repository, used for validation and code quality checks.

### Pull Request (PR)
A GitHub feature that allows contributors to submit their changes for review and integration into the main codebase.

## Q

### Quality Gates
Automated checks that ensure code quality and compliance before it can be merged into the main branch.

## R

### React
A JavaScript library for building user interfaces. Used in the SASS frontend.

### Redux
A predictable state container for JavaScript applications. Used in SASS for state management.

### Repository
A storage location for software packages. In SASS, we use Git repositories hosted on GitHub.

### REST (Representational State Transfer)
An architectural style for designing networked applications using HTTP requests to access and manipulate data.

## S

### SASS
Spring Boot Application with Security System - the name of this project that provides a comprehensive payment platform.

### Security First
An approach to software development that prioritizes security considerations at every stage of the development lifecycle.

### SonarQube
A platform for continuous inspection of code quality to perform automatic reviews with static analysis of code to detect bugs, vulnerabilities and code smells.

### Spring Boot
An open-source Java-based framework used to create stand-alone, production-grade Spring-based applications that you can "just run".

### Spring Modulith
A Spring framework extension that provides first-class support for building modular monoliths.

### SQL
Structured Query Language, used for managing and manipulating relational databases.

### Static Analysis
The analysis of code without actually executing it, often used for detecting bugs, security vulnerabilities, and code quality issues.

### Swagger
A set of open-source tools built around the OpenAPI Specification that helps design, build, document and consume REST APIs.

## T

### TDD (Test-Driven Development)
A software development process that relies on the repetition of a very short development cycle: requirements are turned into very specific test cases, then the software is improved to pass the new tests, only.

### Tenant
In the context of SASS, a tenant represents an organization or user group with isolated data and configuration in the multi-tenant system.

### Test-First
A development approach where tests are written before the implementation code, ensuring requirements are clearly defined.

### TypeScript
A strongly typed programming language that builds on JavaScript, giving you better tooling at any scale. Used in the SASS frontend.

## U

### Unit Test
A level of software testing where individual units/components of a software are tested in isolation.

### User Story
A description of a feature from the perspective of the end user, used in agile development to capture requirements.

## V

### Vulnerability
A weakness in the computational logic found in software and hardware components that, when exploited, results in a negative impact to the confidentiality, integrity, or availability of an application.

## W

### Webhook
A method for augmenting or altering the behavior of a web page, or web application, with custom callbacks.

### Workflow
A sequence of connected steps that define a process, often automated in the context of CI/CD or business processes.

## Y

### YAML
YAML Ain't Markup Language, a human-readable data serialization standard that can be used in conjunction with all programming languages and is commonly used for configuration files.

## Z

### Zero Trust
A security concept centered on the belief that organizations should not automatically trust anything inside or outside its perimeters and instead must verify anything and everything trying to connect to its systems before granting access.