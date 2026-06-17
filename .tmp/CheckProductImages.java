import java.sql.*;

public class CheckProductImages {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/supply_hub?serverTimezone=Asia/Seoul&characterEncoding=UTF-8",
                "root",
                "1234");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("select sku, image_url from products order by product_id limit 10")) {
            while (rs.next()) {
                System.out.println(rs.getString(1) + " | " + rs.getString(2));
            }
        }
    }
}
