package com.project;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class ComputerRepairShop{
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/ComputerRepairShop";  // Ваша база даних
    private static final String DB_USER = "postgres";  // Ваш користувач PostgreSQL
    private static final String DB_PASSWORD = "1234";  // Ваш пароль PostgreSQL

    private static int loginAttempts = 0;
    private static int currentUserId = -1; // Змінна для зберігання ID користувача


    public static void main(String[] args) {
        try {
            ensureDatabaseExists();
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
        showWelcomeScreen();
    }

    private static void ensureDatabaseExists() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            if (conn != null) {
                System.out.println("Database connection established. Path: " + DB_URL);
                createDatabase();
            }
        }
    }

    private static void createDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Database connection established. Path: " + DB_URL);

            // Create tables if they don't exist (Customers, Technicians, Orders, Transport, Devices)

            // Customers Table
            String createCustomersTable = """
        CREATE TABLE IF NOT EXISTS customers (
            id SERIAL PRIMARY KEY,
            name TEXT NOT NULL,
            contact_number TEXT NOT NULL,
            email TEXT NOT NULL UNIQUE,
            password TEXT NOT NULL,
            role TEXT NOT NULL
        );
        """;
            conn.createStatement().execute(createCustomersTable);

            // Technicians Table
            String createTechniciansTable = """
        CREATE TABLE IF NOT EXISTS technicians (
            id SERIAL PRIMARY KEY,
            name TEXT NOT NULL,
            specialty TEXT NOT NULL,
            is_available BOOLEAN NOT NULL DEFAULT TRUE
        );
        """;
            conn.createStatement().execute(createTechniciansTable);

            // Orders Table
            String createOrdersTable = """
        CREATE TABLE IF NOT EXISTS orders (
            id SERIAL PRIMARY KEY,
            customer_id INTEGER NOT NULL,
            device_details TEXT NOT NULL,
            order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            status TEXT NOT NULL DEFAULT 'pending',
            repair_location TEXT NOT NULL,  -- can be 'stationary' or 'on-site'
            technician_id INTEGER,
            FOREIGN KEY (customer_id) REFERENCES customers(id),
            FOREIGN KEY (technician_id) REFERENCES technicians(id)
        );
        """;
            conn.createStatement().execute(createOrdersTable);

            // Transport Table
            String createTransportTable = """
        CREATE TABLE IF NOT EXISTS transport (
            id SERIAL PRIMARY KEY,
            order_id INTEGER NOT NULL,
            transport_type TEXT NOT NULL,  -- can be 'pickup' or 'delivery'
            status TEXT NOT NULL DEFAULT 'pending',
            FOREIGN KEY (order_id) REFERENCES orders(id)
        );
        """;
            conn.createStatement().execute(createTransportTable);

            // Devices Table
            String createDevicesTable = """
        CREATE TABLE IF NOT EXISTS devices (
            id SERIAL PRIMARY KEY,
            order_id INTEGER NOT NULL,
            device_type TEXT NOT NULL,
            brand TEXT NOT NULL,
            model TEXT NOT NULL,
            issue_description TEXT NOT NULL,
            FOREIGN KEY (order_id) REFERENCES orders(id)
        );
        """;
            conn.createStatement().execute(createDevicesTable);

            // Додаємо початкових користувачів (включаючи адміністратора)
            String passwordHashAdmin = Encryption.hashPassword("admin123"); // Хешуємо пароль для адміністратора
            String passwordHashUser = Encryption.hashPassword("user123"); // Хешуємо пароль для користувача
            String insertCustomers = """
        
                    INSERT INTO customers (name, contact_number, email, password, role)
            VALUES\s
            ('Admin', '111-222-3333', 'admin@repairshop.com', ?, 'admin'),
            ('John Doe', '123-456-7890', 'john.doe@example.com', ?, 'user'),
            ('Jane Smith', '987-654-3210', 'jane.smith@example.com', ?, 'user')
            ON CONFLICT (email) DO NOTHING;
        """;

            PreparedStatement stmt = conn.prepareStatement(insertCustomers);
            stmt.setString(1, passwordHashAdmin);
            stmt.setString(2, passwordHashUser);
            stmt.setString(3, passwordHashUser);
            stmt.executeUpdate();
            // Insert initial technicians
            String insertTechnicians = """
        INSERT INTO technicians (name, specialty)
        VALUES 
        ('Alice Johnson', 'Laptop Repair'),
        ('Bob Brown', 'Desktop Repair'),
        ('Charlie Davis', 'On-Site Service')
        ON CONFLICT (name) DO NOTHING;
        """;
            conn.createStatement().execute(insertTechnicians);

            // Insert initial orders (sample orders)
            String insertOrders = """
        INSERT INTO orders (customer_id, device_details, repair_location, technician_id)
        VALUES 
        (1, 'Laptop: Dell XPS 15 - Broken Screen', 'stationary', 1),
        (2, 'Desktop: HP Pavilion - Overheating', 'on-site', 2)
        ON CONFLICT (customer_id) DO NOTHING;
        """;
            conn.createStatement().execute(insertOrders);

            // Insert initial transport (example pickup/delivery)
            String insertTransport = """
        INSERT INTO transport (order_id, transport_type)
        VALUES 
        (1, 'pickup'),
        (2, 'delivery')
        ON CONFLICT (order_id) DO NOTHING;
        """;
            conn.createStatement().execute(insertTransport);

            // Insert initial devices
            String insertDevices = """
        INSERT INTO devices (order_id, device_type, brand, model, issue_description)
        VALUES 
        (1, 'Laptop', 'Dell', 'XPS 15', 'Broken Screen'),
        (2, 'Desktop', 'HP', 'Pavilion', 'Overheating Issue')
        ON CONFLICT (order_id) DO NOTHING;
        """;
            conn.createStatement().execute(insertDevices);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void showWelcomeScreen() {
        JFrame frame = new JFrame("Computer Repair Shop");
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel welcomeLabel = new JLabel("COMPUTER REPAIR SHOP");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        frame.add(welcomeLabel, gbc);

        // Кнопка Login
        JButton loginButton = new JButton("Login");
        loginButton.setToolTipText("Click to login to your account");
        gbc.gridx = 0;
        gbc.gridy = 1;  // Встановлюємо правильну позицію для кнопки login
        frame.add(loginButton, gbc);

        // Кнопка Create Repair Order
        JButton createOrderButton = new JButton("Create Repair Order");
        createOrderButton.setToolTipText("Click to create a new repair order");
        gbc.gridx = 0;
        gbc.gridy = 2;  // Встановлюємо нову позицію для кнопки Create Repair Order
        frame.add(createOrderButton, gbc);

        // Кнопка Urgent Repair Service
        JButton urgentRepairButton = new JButton("Urgent Repair Service");
        urgentRepairButton.setToolTipText("Request urgent repair services at your location");
        gbc.gridx = 0;
        gbc.gridy = 3;  // Встановлюємо нову позицію для кнопки Urgent Repair Service
        frame.add(urgentRepairButton, gbc);

        // Кнопка Manage Transport
        JButton transportButton = new JButton("Manage Transport");
        transportButton.setToolTipText("Manage transport for equipment delivery or pickup");
        gbc.gridx = 0;
        gbc.gridy = 4;  // Встановлюємо нову позицію для кнопки Manage Transport
        frame.add(transportButton, gbc);

        // Кнопка Contact Us
        JButton contactButton = new JButton("Contact Us");
        contactButton.setToolTipText("Contact us for further information or assistance");
        gbc.gridx = 0;
        gbc.gridy = 5;  // Встановлюємо нову позицію для кнопки Contact Us
        frame.add(contactButton, gbc);

        // Кнопка Exit
        JButton exitButton = new JButton("Exit");
        exitButton.setToolTipText("Exit the application");
        gbc.gridx = 0;
        gbc.gridy = 6;  // Встановлюємо нову позицію для кнопки Exit
        frame.add(exitButton, gbc);

        frame.setSize(400, 400);  // Встановлюємо більший розмір для вікна
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Action Listeners
        loginButton.addActionListener(e -> {
            frame.dispose();
            loginForm();
        });

        createOrderButton.addActionListener(e -> {
            frame.dispose();
            // Перевірка чи користувач залогінений
            if (currentUserId == -1) {
                JOptionPane.showMessageDialog(frame, "You need to log in first.");
                loginForm();
            } else {
                createRepairOrder();  // Створення замовлення
            }
        });

        urgentRepairButton.addActionListener(e -> {
            frame.dispose();
            requestUrgentRepair();
        });

        transportButton.addActionListener(e -> {
            frame.dispose();
            manageTransport();
        });

        contactButton.addActionListener(e -> {
            frame.dispose();
            showContactPage();
        });

        exitButton.addActionListener(e -> System.exit(0));
    }

    private static void createRepairOrder() {
        JFrame orderFrame = new JFrame("Create Repair Order");
        orderFrame.setSize(300, 200);
        orderFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        orderFrame.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Client Name input field with validation
        JTextField clientNameField = new JTextField();
        clientNameField.setText("Enter Client Name");
        clientNameField.setForeground(Color.GRAY);
        clientNameField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (clientNameField.getText().equals("Enter Client Name")) {
                    clientNameField.setText("");
                    clientNameField.setForeground(Color.BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (clientNameField.getText().isEmpty()) {
                    clientNameField.setText("Enter Client Name");
                    clientNameField.setForeground(Color.GRAY);
                }
            }
        });
        panel.add(clientNameField);

        // Device details input field with validation
        JTextField deviceDetailsField = new JTextField();
        deviceDetailsField.setText("Enter Device Details");
        deviceDetailsField.setForeground(Color.GRAY);
        deviceDetailsField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (deviceDetailsField.getText().equals("Enter Device Details")) {
                    deviceDetailsField.setText("");
                    deviceDetailsField.setForeground(Color.BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (deviceDetailsField.getText().isEmpty()) {
                    deviceDetailsField.setText("Enter Device Details");
                    deviceDetailsField.setForeground(Color.GRAY);
                }
            }
        });
        panel.add(deviceDetailsField);

        // Submit button with input validation
        JButton submitButton = new JButton("Submit Order");
        submitButton.addActionListener(e -> {
            String clientName = clientNameField.getText().trim();
            String deviceDetails = deviceDetailsField.getText().trim();

            if (clientName.isEmpty() || clientName.equals("Enter Client Name")) {
                JOptionPane.showMessageDialog(orderFrame, "Client name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (deviceDetails.isEmpty() || deviceDetails.equals("Enter Device Details")) {
                JOptionPane.showMessageDialog(orderFrame, "Device details cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Submit the order logic here
            JOptionPane.showMessageDialog(orderFrame, "Order submitted for " + clientName + " with device " + deviceDetails);
            orderFrame.dispose();
            showWelcomeScreen();  // Return to main screen
        });
        panel.add(submitButton);

        orderFrame.setVisible(true);
    }
    // Method to request an urgent repair
    private static void requestUrgentRepair() {
        JFrame urgentFrame = new JFrame("Urgent Repair Request");
        urgentFrame.setSize(300, 150);
        urgentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        urgentFrame.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField clientNameField = new JTextField("Enter Client Name");
        panel.add(clientNameField);
        JTextField issueDescriptionField = new JTextField("Describe the Issue");
        panel.add(issueDescriptionField);

        JButton submitButton = new JButton("Submit Urgent Repair");
        submitButton.addActionListener(e -> {
            String clientName = clientNameField.getText();
            String issueDescription = issueDescriptionField.getText();
            // Logic to handle urgent repair request
            JOptionPane.showMessageDialog(urgentFrame, "Urgent repair request submitted for " + clientName + " with issue: " + issueDescription);
            urgentFrame.dispose();
            showWelcomeScreen();  // Return to main screen
        });
        panel.add(submitButton);

        urgentFrame.setVisible(true);
    }

    // Method to manage transport
    private static void manageTransport() {
        JFrame transportFrame = new JFrame("Manage Transport");
        transportFrame.setSize(300, 150);
        transportFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        transportFrame.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField orderIdField = new JTextField("Enter Order ID for Transport");
        panel.add(orderIdField);
        JButton transportButton = new JButton("Send for Transport");
        transportButton.addActionListener(e -> {
            String orderId = orderIdField.getText();
            // Logic to manage transport for the order
            JOptionPane.showMessageDialog(transportFrame, "Order " + orderId + " is ready for transport.");
            transportFrame.dispose();
            showWelcomeScreen();  // Return to main screen
        });
        panel.add(transportButton);

        transportFrame.setVisible(true);
    }

    // Method to show contact page
    private static void showContactPage() {
        JFrame contactFrame = new JFrame("Contact Us");
        contactFrame.setSize(300, 200);
        contactFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        contactFrame.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel contactLabel = new JLabel("Contact Information:");
        panel.add(contactLabel);
        JLabel emailLabel = new JLabel("Email: support@repairshop.com");
        panel.add(emailLabel);
        JLabel phoneLabel = new JLabel("Phone: +1 800 123 4567");
        panel.add(phoneLabel);

        contactFrame.setVisible(true);
    }
    private static void loginForm() {
        JFrame frame = new JFrame("Login");
        JLabel userLabel = new JLabel("Email:");
        JTextField userField = new JTextField();
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton backButton = new JButton("Back");

        frame.setLayout(null);
        userLabel.setBounds(50, 50, 80, 25);
        userField.setBounds(150, 50, 150, 25);
        passLabel.setBounds(50, 100, 80, 25);
        passField.setBounds(150, 100, 150, 25);
        loginButton.setBounds(50, 150, 100, 25);
        backButton.setBounds(200, 150, 100, 25);

        frame.add(userLabel);
        frame.add(userField);
        frame.add(passLabel);
        frame.add(passField);
        frame.add(loginButton);
        frame.add(backButton);
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Логіка аутентифікації користувача
        loginButton.addActionListener(e -> {
            if (loginAttempts >= 3) {
                JOptionPane.showMessageDialog(frame, "Too many failed attempts. Try again later.");
            } else {
                String username = userField.getText();
                String password = new String(passField.getPassword());
                if (authenticateUser(username, password)) {
                    frame.dispose();
                    showMainApp(username, getUserRole(username)); // Показуємо головне вікно після успішного входу
                } else {
                    loginAttempts++;
                    JOptionPane.showMessageDialog(frame, "Invalid credentials. Attempts left: " + (3 - loginAttempts));
                }
            }
        });

        // Повернення на головний екран
        backButton.addActionListener(e -> {
            frame.dispose();
            showWelcomeScreen();
        });
    }

    // Аутентифікація користувача
    private static boolean authenticateUser(String email, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT id, password, role FROM customers WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email); // Використовуємо email замість username
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPasswordHash = rs.getString("password");
                String role = rs.getString("role");

                // Перевірка пароля за допомогою хешу
                if (Encryption.checkPassword(password, storedPasswordHash)) {
                    currentUserId = rs.getInt("id"); // Отримуємо ID користувача
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return false;
    }


    // Отримання ролі користувача для відображення відповідного інтерфейсу

    private static String getUserRole(String email) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT role FROM customers WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email); // Використовуємо email замість username
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user role: " + e.getMessage());
        }
        return "user"; // За замовчуванням роль user
    }

    private static void showMainApp(String username, String role) {
        JFrame frame = new JFrame("Main App");
        JLabel label = new JLabel("Welcome, " + username + " (" + role + ")");
        label.setBounds(50, 20, 300, 25);
        frame.setLayout(null); // використовуємо абсолютне позиціонування
        frame.add(label);

        if ("admin".equals(role)) {
            JButton manageOrdersButton = new JButton("Manage Orders");
            manageOrdersButton.setBounds(50, 170, 150, 30);
            frame.add(manageOrdersButton);

            JButton manageTechniciansButton = new JButton("Manage Technicians");
            manageTechniciansButton.setBounds(50, 220, 150, 30);
            frame.add(manageTechniciansButton);

            // Кнопка для повернення на головний екран
            JButton backButton = new JButton("Back");
            backButton.setBounds(50, 270, 150, 30);
            frame.add(backButton);

            manageOrdersButton.addActionListener(e -> {
                manageOrders(); // управління замовленнями
            });

            manageTechniciansButton.addActionListener(e -> {
                manageTechnicians(); // управління майстрами
            });

            backButton.addActionListener(e -> {
                frame.dispose();
                showWelcomeScreen(); // повернення на головний екран
            });

        } else if ("user".equals(role)) {

            JButton createOrderButton = new JButton("Create Repair Order");
            createOrderButton.setBounds(50, 120, 150, 30);
            frame.add(createOrderButton);

            JButton backButton = new JButton("Back");
            backButton.setBounds(50, 170, 150, 30);
            frame.add(backButton);


            createOrderButton.addActionListener(e -> {
                frame.dispose();
                createRepairOrderForm(); // форма для створення замовлення на ремонт
            });

            backButton.addActionListener(e -> {
                frame.dispose();
                showWelcomeScreen(); // повернення на головний екран
            });
        }

        frame.setSize(400, 350); // Змінили розмір вікна
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    private static void createRepairOrderForm() {
        JFrame orderFrame = new JFrame("Create Repair Order");
        orderFrame.setSize(400, 300);
        orderFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        orderFrame.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Поля для введення даних
        JTextField customerNameField = new JTextField("Enter Customer Name");
        panel.add(customerNameField);
        JTextField deviceDetailsField = new JTextField("Enter Device Details (e.g., model, issue)");
        panel.add(deviceDetailsField);
        JComboBox<String> repairLocationCombo = new JComboBox<>(new String[]{"Stationary", "On-site"});
        panel.add(repairLocationCombo);

        JButton submitButton = new JButton("Submit Order");
        submitButton.addActionListener(e -> {
            String customerName = customerNameField.getText();
            String deviceDetails = deviceDetailsField.getText();
            String repairLocation = (String) repairLocationCombo.getSelectedItem();

            // Збереження даних у базі (потрібно додати відповідну логіку для збереження)
            JOptionPane.showMessageDialog(orderFrame, "Repair order created for " + customerName + " with device: " + deviceDetails + " for " + repairLocation + " repair.");
            orderFrame.dispose();
            showMainApp(customerName, "user"); // Повернення до основного вікна після створення замовлення
        });
        panel.add(submitButton);

        orderFrame.setVisible(true);
    }
    private static void manageTechnicians() {
        JFrame techniciansFrame = new JFrame("Manage Technicians");
        techniciansFrame.setSize(400, 300);
        techniciansFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        techniciansFrame.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Приклад списку майстрів
        String[] technicianList = {"Alice Johnson - Available", "Bob Brown - Busy", "Charlie Davis - Available"};
        JList<String> technicianListView = new JList<>(technicianList);
        JScrollPane scrollPane = new JScrollPane(technicianListView);
        panel.add(scrollPane);

        JButton assignTechnicianButton = new JButton("Assign Technician");
        assignTechnicianButton.addActionListener(e -> {
            // Логіка для призначення майстра до замовлення
            String selectedTechnician = technicianListView.getSelectedValue();
            if (selectedTechnician != null) {
                JOptionPane.showMessageDialog(techniciansFrame, "Assigned " + selectedTechnician + " to the job.");
            } else {
                JOptionPane.showMessageDialog(techniciansFrame, "No technician selected.");
            }
        });
        panel.add(assignTechnicianButton);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            techniciansFrame.dispose();
            showMainApp("admin", "admin"); // Повернення до головного екрану адміністратора
        });
        panel.add(backButton);

        techniciansFrame.setVisible(true);
    }

    private static void manageOrders() {
        JFrame ordersFrame = new JFrame("Manage Orders");
        ordersFrame.setSize(400, 300);
        ordersFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        ordersFrame.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Приклад списку замовлень
        String[] orderList = {
                "Order #1 - Pending - Laptop Repair",
                "Order #2 - Completed - Desktop Repair",
                "Order #3 - In Progress - Mobile Phone Repair"
        };
        JList<String> orderListView = new JList<>(orderList);
        JScrollPane scrollPane = new JScrollPane(orderListView);
        panel.add(scrollPane);

        JButton updateOrderButton = new JButton("Update Order Status");
        updateOrderButton.addActionListener(e -> {
            // Логіка для оновлення статусу замовлення
            String selectedOrder = orderListView.getSelectedValue();
            if (selectedOrder != null) {
                JOptionPane.showMessageDialog(ordersFrame, "Updated " + selectedOrder);
            } else {
                JOptionPane.showMessageDialog(ordersFrame, "No order selected.");
            }
        });
        panel.add(updateOrderButton);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            ordersFrame.dispose();
            showMainApp("admin", "admin"); // Повернення до головного екрану адміністратора
        });
        panel.add(backButton);

        ordersFrame.setVisible(true);
    }
}
