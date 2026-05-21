package DataBase_Sever;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBconnection {
    public static Connection getConnection() {
        try {
            String url = "jdbc:mysql://localhost:3306/UserManagement";
            String user = "root";
            String password = "Liemdz@12345";
            return DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}