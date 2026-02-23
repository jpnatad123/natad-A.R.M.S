/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.proteanit.sql.DbUtils; // Add this import for DbUtils


/**
 *
 * @author USER35
 */
public class configclass {
    
    //Connection Method to SQLITE
    public static Connection connectDB() {
        Connection con = null;
        try {
            Class.forName("org.sqlite.JDBC"); // Load the SQLite JDBC driver
            con = DriverManager.getConnection("jdbc:sqlite:paulDB.db"); // Establish connection
            // Make the connection more resilient to concurrent access:
            // - set a busy timeout so the driver will wait for locks instead of failing immediately
            // - enable WAL journal mode to reduce write-lock contention
            // - ensure auto-commit is enabled for short transactions
            try (java.sql.Statement pragma = con.createStatement()) {
                pragma.execute("PRAGMA busy_timeout = 5000;");
                pragma.execute("PRAGMA journal_mode = WAL;");
            } catch (Exception e) {
                System.out.println("Warning: couldn't set PRAGMA: " + e.getMessage());
            }
            con.setAutoCommit(true);
            System.out.println("Connection Successful");
        } catch (Exception e) {
            System.out.println("Connection Failed: " + e);
        }
        return con;
    }
    
    public void addRecord(String sql, Object... values) {
        // Use retry logic to reduce failures due to SQLITE_BUSY / "database is locked"
        try (Connection conn = this.connectDB()) {
            int attempts = 0;
            int maxAttempts = 5;
            while (attempts < maxAttempts) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    // set parameters
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] instanceof Integer) {
                            pstmt.setInt(i + 1, (Integer) values[i]);
                        } else if (values[i] instanceof Double) {
                            pstmt.setDouble(i + 1, (Double) values[i]);
                        } else if (values[i] instanceof Float) {
                            pstmt.setFloat(i + 1, (Float) values[i]);
                        } else if (values[i] instanceof Long) {
                            pstmt.setLong(i + 1, (Long) values[i]);
                        } else if (values[i] instanceof Boolean) {
                            pstmt.setBoolean(i + 1, (Boolean) values[i]);
                        } else if (values[i] instanceof java.util.Date) {
                            pstmt.setDate(i + 1, new java.sql.Date(((java.util.Date) values[i]).getTime()));
                        } else if (values[i] instanceof java.sql.Date) {
                            pstmt.setDate(i + 1, (java.sql.Date) values[i]);
                        } else if (values[i] instanceof java.sql.Timestamp) {
                            pstmt.setTimestamp(i + 1, (java.sql.Timestamp) values[i]);
                        } else {
                            pstmt.setString(i + 1, values[i].toString());
                        }
                    }

                    pstmt.executeUpdate();
                    System.out.println("Record added successfully!");
                    break; // success
                } catch (SQLException e) {
                    String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                    if (msg.contains("database is locked") || e.getErrorCode() == 5) {
                        attempts++;
                        System.out.println("Database locked, retrying (" + attempts + ")...");
                        try { Thread.sleep(200 * attempts); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        if (attempts >= maxAttempts) {
                            System.out.println("Error adding record after retries: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Error adding record: " + e.getMessage());
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error opening connection for addRecord: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Variant of addRecord that returns success status
    public boolean addRecordBool(String sql, Object... values) {
        try (Connection conn = this.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) values[i]);
                } else if (values[i] instanceof Double) {
                    pstmt.setDouble(i + 1, (Double) values[i]);
                } else if (values[i] instanceof Float) {
                    pstmt.setFloat(i + 1, (Float) values[i]);
                } else if (values[i] instanceof Long) {
                    pstmt.setLong(i + 1, (Long) values[i]);
                } else if (values[i] instanceof Boolean) {
                    pstmt.setBoolean(i + 1, (Boolean) values[i]);
                } else if (values[i] instanceof java.util.Date) {
                    pstmt.setDate(i + 1, new java.sql.Date(((java.util.Date) values[i]).getTime()));
                } else if (values[i] instanceof java.sql.Date) {
                    pstmt.setDate(i + 1, (java.sql.Date) values[i]);
                } else if (values[i] instanceof java.sql.Timestamp) {
                    pstmt.setTimestamp(i + 1, (java.sql.Timestamp) values[i]);
                } else {
                    pstmt.setString(i + 1, values[i].toString());
                }
            }

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error adding record: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Helper to fetch a single string value (first column of first row)
    public String getSingleString(String sql, Object... values) {
        try (Connection conn = this.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching single string: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    // Authentication method to check user credentials and return user type
    public String authenticate(String sql, Object... values) {
        try (Connection conn = connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Set parameters using setObject for simplicity
            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("type"); // Return user type (e.g., "Admin", "Teacher")
                }
            }
        } catch (SQLException e) {
            System.out.println("Login Error: " + e.getMessage());
        }
        return null; // Return null if authentication fails
    }
    
    // Overloaded method with parameters for queries with WHERE clauses
    public void displayData(String sql, javax.swing.JTable table, Object... values) {
        try (Connection conn = connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set parameters if provided
            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                table.setModel(DbUtils.resultSetToTableModel(rs));
            }
            
        } catch (SQLException e) {
            System.out.println("Error displaying data: " + e.getMessage());
        }
    }
    
    // UPDATE method for modifying existing records
    public boolean updateRecord(String sql, Object... values) {
        try (Connection conn = this.connectDB()) {
            int attempts = 0;
            int maxAttempts = 5;
            while (attempts < maxAttempts) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] instanceof Integer) {
                            pstmt.setInt(i + 1, (Integer) values[i]);
                        } else if (values[i] instanceof Double) {
                            pstmt.setDouble(i + 1, (Double) values[i]);
                        } else if (values[i] instanceof Float) {
                            pstmt.setFloat(i + 1, (Float) values[i]);
                        } else if (values[i] instanceof Long) {
                            pstmt.setLong(i + 1, (Long) values[i]);
                        } else if (values[i] instanceof Boolean) {
                            pstmt.setBoolean(i + 1, (Boolean) values[i]);
                        } else if (values[i] instanceof java.util.Date) {
                            pstmt.setDate(i + 1, new java.sql.Date(((java.util.Date) values[i]).getTime()));
                        } else if (values[i] instanceof java.sql.Date) {
                            pstmt.setDate(i + 1, (java.sql.Date) values[i]);
                        } else if (values[i] instanceof java.sql.Timestamp) {
                            pstmt.setTimestamp(i + 1, (java.sql.Timestamp) values[i]);
                        } else {
                            pstmt.setString(i + 1, values[i].toString());
                        }
                    }

                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("Record updated successfully!");
                        return true;
                    }
                } catch (SQLException e) {
                    String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                    if (msg.contains("database is locked") || e.getErrorCode() == 5) {
                        attempts++;
                        System.out.println("Database locked on update, retrying (" + attempts + ")...");
                        try { Thread.sleep(200 * attempts); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        if (attempts >= maxAttempts) {
                            System.out.println("Error updating record after retries: " + e.getMessage());
                            e.printStackTrace();
                            return false;
                        }
                    } else {
                        System.out.println("Error updating record: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error opening connection for updateRecord: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // DELETE method for removing records
    public boolean deleteRecord(String sql, Object... values) {
        try (Connection conn = this.connectDB()) {
            int attempts = 0;
            int maxAttempts = 5;
            while (attempts < maxAttempts) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] instanceof Integer) {
                            pstmt.setInt(i + 1, (Integer) values[i]);
                        } else if (values[i] instanceof Double) {
                            pstmt.setDouble(i + 1, (Double) values[i]);
                        } else {
                            pstmt.setString(i + 1, values[i].toString());
                        }
                    }
                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("Record deleted successfully!");
                        return true;
                    }
                } catch (SQLException e) {
                    String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                    if (msg.contains("database is locked") || e.getErrorCode() == 5) {
                        attempts++;
                        System.out.println("Database locked on delete, retrying (" + attempts + ")...");
                        try { Thread.sleep(200 * attempts); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        if (attempts >= maxAttempts) {
                            System.out.println("Error deleting record after retries: " + e.getMessage());
                            e.printStackTrace();
                            return false;
                        }
                    } else {
                        System.out.println("Error deleting record: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error opening connection for deleteRecord: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}