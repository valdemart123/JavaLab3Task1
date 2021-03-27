import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class ConnectionManager {
    public static Connection connection;

    public static Connection getConnection(String url)
    {
        File tmpFile = new File(url);
        boolean exists = tmpFile.exists();
        try
        {
            connection = DriverManager.getConnection("jdbc:sqlite:"+url);
            if (!exists)
            {
                createTable();
                fillData();
            }
            return connection;
        }
        catch (SQLException e)
        {
            System.err.println(e.getMessage());
            return null;
        }
    }

    private static void createTable() throws SQLException
    {
        String sql_shop = "CREATE TABLE ShopOrders (\n"
                +   "OrderN SERIAL PRIMARY KEY, \n"
                +   "OrderDate DATE NOT NULL DEFAULT CURRENT_DATE);";

        String sql_goods =  "CREATE TABLE GOODS (\n"
                +   "GoodsN SERIAL PRIMARY KEY, \n"
                +   "Title VARCHAR NOT NULL, \n"
                +   "Description VARCHAR NOT NULL, \n"
                +   "Price INTEGER NOT NULL);";
                
        String sql_gi =  "CREATE TABLE GoodsInOrder (\n"
                +   "OrderN INTEGER NOT NULL, \n"
                +   "GoodsN INTEGER NOT NULL, \n"
                +   "Quantity INTEGER NOT NULL, \n"
                +   "FOREIGN KEY (OrderN) REFERENCES ShopOrders(OrderN) ON DELETE CASCADE, \n"
                +   "FOREIGN KEY (GoodsN) REFERENCES Goods(GoodsN) ON DELETE CASCADE);";
        
        Statement stmt = connection.createStatement();
        stmt.execute(sql_shop);
        stmt.execute(sql_goods);
        stmt.execute(sql_gi);

        System.out.println("Data tables created");
    }

    private static void fillData() throws SQLException
    {
        try {
            File myObj = new File("/fillData.txt");
            Scanner scanner = new Scanner(myObj);

            Statement stmt = connection.createStatement();

            while(scanner.hasNextLine())
            {
                String sql = scanner.nextLine();
                System.out.println(sql);
                stmt.execute(sql);
            }

            System.out.println("Data tables filled");
            scanner.close();

        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }  
    }
}
