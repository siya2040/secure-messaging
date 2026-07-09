# Secure Messaging Web Application

A full-stack, secure, real-time messaging web application developed as a Bachelor of Technology project in Computer Science & Engineering (specialization in Cyber Security and Forensics). 

This platform supports direct messages, group chats, image sharing, and a custom UDP-based LAN Discovery Service allowing users on the same Wi-Fi network to connect without manual IP configuration.

---

## Key Features

*   **User Registration & Authentication**:
    *   Stateless authentication using **JSON Web Tokens (JWT)**.
    *   Secure password storage using **BCrypt** hashing.
    *   Mandatory **OTP-based email verification** via Gmail SMTP.
*   **Messaging System**:
    *   Supports one-on-one direct messages and group conversations.
    *   Secure image sharing (supports JPG, PNG, WEBP).
    *   Automated message delivery tracking.
*   **LAN Mode Connectivity**:
    *   **UDP Broadcast Beaconing**: Broadcasts server presence every 5 seconds on UDP port `47777`.
    *   **Automatic Server Discovery**: Clients can automatically resolve and connect to the local host interface.
    *   **Peer Trust Management**: Active LAN discovery with admin-approved trusted status to prevent Man-in-the-Middle (MITM) attacks.
*   **Performance & Reliability**:
    *   Dynamic cache-busting of CSS and JS assets using startup timestamps.
    *   Global error handling preventing internal info leakages.
    *   Strict HTTPS redirect enforcing transport layer security (TLS 1.2+).

---

## Technology Stack

*   **Backend**: Spring Boot 3.2.5, Spring Security, Spring Data JPA, Java JCE (AES, HMAC-SHA256, DH)
*   **Database**: MySQL 8.x
*   **Frontend**: HTML5, CSS3, Vanilla JavaScript (Single-Page Application)
*   **Build Tool**: Apache Maven 3.x
*   **Mail Protocol**: SMTP (Spring Mail)

---

## Project Structure

```
├── src/main/java/com/securechat/securemessaging
│   ├── config/          # Security, Web and HTTPS routing configs
│   ├── controller/      # Auth, Message, Group, Image, and LAN REST endpoints
│   ├── dto/             # Request & Response Data Transfer Objects
│   ├── exception/       # Centralized exception handlers
│   ├── lan/             # LAN broadcast & presence services
│   ├── model/           # Entity schemas (User, Message, Group, etc.)
│   ├── repository/      # Spring Data JPA repositories
│   └── security/        # Cryptography (AES, HMAC, DH) & JWT filters
└── src/main/resources
    ├── static/          # HTML, CSS, and JS frontend files
    └── templates/       # HTML page templates
```

---

## Getting Started (Local Setup)

### Prerequisites
*   Java Development Kit (JDK) 17+
*   MySQL Server 8.x
*   Maven 3.x (or use the included wrapper `mvnw`)

### 1. Database Setup
Create a MySQL database named `secure_chat`:
```sql
CREATE DATABASE secure_chat;
```

### 2. Environment Configuration
The application uses environment variables for sensitive credentials to prevent leaks. Copy the template `.env.example` to a new file named `.env` and fill in your local credentials:
```properties
DB_PASSWORD=your_mysql_password
MAIL_USERNAME=your_gmail_address
MAIL_PASSWORD=your_gmail_app_password
JWT_SECRET=your_custom_jwt_secret
```

### 3. Run the Application

#### Windows (PowerShell)
Set the environment variables and run using the Maven wrapper:
```powershell
$env:DB_PASSWORD="your_mysql_password"
$env:MAIL_USERNAME="your_gmail_address"
$env:MAIL_PASSWORD="your_gmail_app_password"
$env:JWT_SECRET="your_custom_jwt_secret"
./mvnw spring-boot:run
```

#### macOS / Linux
```bash
export DB_PASSWORD="your_mysql_password"
export MAIL_USERNAME="your_gmail_address"
export MAIL_PASSWORD="your_gmail_app_password"
export JWT_SECRET="your_custom_jwt_secret"
./mvnw spring-boot:run
```

The application will start on port `8080` (HTTP) or `8443` (HTTPS in production). Access it locally at `http://localhost:8080`.

---

## Project Contributors

*   **Vishesh Duggal** (Roll No. R2142230889)
*   **Siya Chauhan** (Roll No. R2142230927)
*   **Mudit Singh Bora** (Roll No. R2142231814)
*   **Ishu Thakur** (Roll No. R2142230108)

**Under the Guidance of**:
*   **Prof. N Prasanthi Kumari**
*   *School of Computer Science, University of Petroleum & Energy Studies (UPES), Dehradun*
