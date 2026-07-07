# MedSched Local Setup Guide

This document provides the necessary steps to configure and run the **MedSched** application locally.

---

## Prerequisites

Before running the application, make sure you have the following installed:

- **Java**  
  Required to compile and run the Spring Boot backend.

- **Node.js and npm**  
  Required to install dependencies and run the React frontend.

- **PostgreSQL**  
  Required for the backend database.

---

# Backend Setup

## 1. Navigate to the backend directory

Open a terminal and run:

```bash
cd MedSched/backend
```

---

## 2. Configure database settings

Review the following file:

```
src/main/resources/application-dev.yml
```

Make sure your local database credentials and environment configuration match your PostgreSQL setup.

Example:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/medsched
    username: your_username
    password: your_password
```

---

## 3. Start the database

Ensure that your local PostgreSQL database is running.

The application uses **Flyway** for database migrations. When the backend starts, Flyway will automatically apply the migrations defined in:

```
flyway_migrations.txt
```

---

## 4. Run the backend application

Using Maven Wrapper:

### Mac/Linux

```bash
./mvnw spring-boot:run
```

### Windows

```cmd
mvnw.cmd spring-boot:run
```

The backend should start successfully on the configured local port.

---

# Frontend Setup

## 1. Navigate to the frontend directory

Open another terminal window:

```bash
cd MedSched/frontend
```

---

## 2. Install dependencies

Install the required React and Vite dependencies:

```bash
npm install
```

---

## 3. Start the development server

Run:

```bash
npm run dev
```

The frontend development server will start and provide a local URL where the application can be accessed.

---

# Additional Documentation

## Project Documentation

Additional technical details, specifications, and system architecture diagrams can be found in:

```
MedSched/documents/
```

Available documents:

- `Caiet de Sarcini 2.pdf` - Project specifications
- `MedSched.drawio.png` - System architecture diagram

---

## Issue Tracking

Current development tasks, features, and bugs can be found in:

```
github_issues.csv
```

This file contains the imported GitHub issue list used for project tracking.

---

# Project Structure

```
MedSched/
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── mvnw
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
│
├── documents/
│   ├── Caiet de Sarcini 2.pdf
│   └── MedSched.drawio.png
│
└── github_issues.csv
```