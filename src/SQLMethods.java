import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class SQLMethods {
    public static void showItems(Connection connection) //Метод аби показати які є товари в магазині
    {
        String sql = "SELECT * FROM GOODS";

        try {
            Statement stmt = connection.createStatement();

            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("Goods:");

            ResultSetMetaData rsmd = rs.getMetaData(); // Звідси будемо брати назви стовпців таблиці

            for (int i = 1; i <= rsmd.getColumnCount(); i++)
            {
                System.out.print(rsmd.getColumnName(i) + "\t");
            }
            System.out.println();

            while (rs.next())
            {
                System.out.println(rs.getInt("GoodsN") + "\t" +
                                   rs.getString("Title") + "\t" +
                                   rs.getString("Description") + "\t" +
                                   rs.getInt("Price"));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void showOrders(Connection connection)
    {
        String sql_orders = "SELECT * FROM ShopOrders";
        String sql_goods = "SELECT Tab.GoodsN, Goods.Title, Tab.Quantity, Goods.Price*Tab.Quantity AS TotalPrice FROM GoodsInOrder AS Tab INNER JOIN Goods ON Tab.GoodsN=Goods.GoodsN WHERE Tab.OrderN=?";

        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql_orders);

            System.out.println("Orders:");

            while(rs.next())
            {
                System.out.println(rs.getInt("OrderN") + "\t" +
                                   rs.getString("OrderDate") + " : "                   
                );
                PreparedStatement pstmt = connection.prepareStatement(sql_goods);
                pstmt.setInt(1, rs.getInt("OrderN"));

                ResultSet rs_inner = pstmt.executeQuery();
                ResultSetMetaData rsmd = rs_inner.getMetaData();

                for (int i = 1; i <= rsmd.getColumnCount(); i++)
                {
                    System.out.print(rsmd.getColumnName(i) + "\t");
                }

                System.out.println();


                int OrderTotalPrice = 0;
                while(rs_inner.next())
                {
                    
                    System.out.println(rs_inner.getInt("GoodsN") + "\t" +
                                       rs_inner.getString("Title") + "\t" +
                                       rs_inner.getInt("Quantity") + "\t" +
                                       rs_inner.getInt("TotalPrice")
                    );
                    OrderTotalPrice += rs_inner.getInt("TotalPrice");
                }
                System.out.print("Order total price: " + OrderTotalPrice);
                System.out.println();
            }
            
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void showRangeOrders(Connection connection, int MaxSum, int GoodsNumber)
    {
        String sql_orders = "SELECT COUNT(Tab.OrderN) AS NumberOfGoods, Tab.OrderN, SUM(Goods.Price*Tab.Quantity) as OrderPrice FROM GoodsInOrder AS Tab INNER JOIN Goods ON Tab.GoodsN=Goods.GoodsN GROUP BY Tab.OrderN HAVING NumberOfGoods = ? AND OrderPrice < ?";

        try {
            PreparedStatement stmt = connection.prepareStatement(sql_orders);
            stmt.setInt(1, GoodsNumber);
            stmt.setInt(2, MaxSum);

            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();

            System.out.println(String.format("Show orders where MaxSum is %d and Goods Number is %d", MaxSum, GoodsNumber));

            
            for (int i = 1; i <= rsmd.getColumnCount(); i++)
            {
                System.out.print(rsmd.getColumnName(i) + "\t");
            }

            System.out.println();

            while(rs.next())
            {
                System.out.println(rs.getInt("NumberOfGoods") + "\t" +
                                   rs.getString("OrderN") + "\t" +
                                   rs.getInt("OrderPrice")                   
                );

            }
            
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void orderToday(Connection connection) //Створення замовлення на основі замовлень за поточний день
    {
        String today = "2021-03-02"; // Так як під час запуску програми можливо не бути поточних замовлень тому для тесту поставимо дату яка точно є в бд

        System.out.println("Forming new order for today");

        String sql_lastId = "SELECT OrderN FROM ShopOrders WHERE ROWID IN ( SELECT max( ROWID ) FROM ShopOrders );";
        String sql_orderN = "SELECT OrderN FROM ShopOrders WHERE OrderDate=?";
        String sql_goods = "SELECT * FROM GoodsInOrder WHERE OrderN=?";
        String sql_insert = "INSERT INTO GoodsInOrder (OrderN, GoodsN, Quantity) VALUES (?,?,?)";
        String sql_addOrder = "INSERT INTO ShopOrders (OrderN, OrderDate) VALUES (?, ?)";

        Map<Integer, Integer> goods = new HashMap<Integer, Integer>();//Словник де будуть накоплювати замовлення для наступного їх напавленян в нове замовлення

        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql_lastId);//Шукаємо останній номер замовлення аби присвоїти для нового +1

            int newID = rs.getInt("OrderN") + 1;

            PreparedStatement pstmt = connection.prepareStatement(sql_orderN);//Шукаємо номери замовлень за поточний день
            pstmt.setString(1, today);
            ResultSet rs_orderN = pstmt.executeQuery();

            int size = 0;
            while(rs_orderN.next())
            {
                ++size;

                PreparedStatement pStatement = connection.prepareStatement(sql_goods);//Шукаємо товари по номеру замовлення
                pStatement.setInt(1, rs_orderN.getInt("OrderN"));
                ResultSet rSet = pStatement.executeQuery();

                while(rSet.next()) // Додаємо товар в словник
                {
                    int key = rSet.getInt("GoodsN");
                    if(goods.containsKey(key))
                    {
                        int newValue = goods.get(key) + rSet.getInt("Quantity");
                        goods.replace(key, newValue);
                    }
                    else
                    {
                        goods.put(key, rSet.getInt("Quantity"));
                    }
                }
            }

            if(size == 0 )
            {
                System.err.println("No orders today.");
                return;
            }


            PreparedStatement iStatement = connection.prepareStatement(sql_addOrder);//Додаємо нове замовлення
            iStatement.setInt(1, newID);
            iStatement.setString(2, today);

            iStatement.execute();

            for (int key : goods.keySet()) {
                iStatement = connection.prepareStatement(sql_insert);//Додаємо товари до замовлення
                iStatement.setInt(1, newID);
                iStatement.setInt(2, key);
                iStatement.setInt(3, goods.get(key));

                iStatement.execute();
            }

            System.out.println("New Order Added");

        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }


    }

    public static void deleteOrder(Connection connection, int GoodsN, int Quantity) //Метод для видалення вибраних замовлень
    {
        System.out.println("Searching orders to delete");

        String sql_findOrderN = "SELECT OrderN FROM GoodsInOrder WHERE GoodsN=? AND Quantity=?";
        String sql_deleteGoods = "DELETE FROM GoodsInOrder WHERE OrderN=?";
        String sql_deleteOrder = "DELETE FROM ShopOrders WHERE OrderN=?";
        try {
            PreparedStatement pStatement = connection.prepareStatement(sql_findOrderN); //Шукаємо замовлення за параметрами
            pStatement.setInt(1, GoodsN);
            pStatement.setInt(2, Quantity);

            ResultSet rSet = pStatement.executeQuery();

            int size = 0;
            while(rSet.next())
            {
                ++size;

                PreparedStatement pStatement2 = connection.prepareStatement(sql_deleteOrder);//Видаляємо замовлення
                pStatement2.setInt(1, rSet.getInt("OrderN"));

                pStatement2.execute();

                pStatement2 = connection.prepareStatement(sql_deleteGoods);//Видаляємо товари з замовлення
                pStatement2.setInt(1, rSet.getInt("OrderN"));

                pStatement2.execute();
            }

            if(size == 0 )
            {
                System.err.println("No orders found.");
                return;
            }

            System.out.println("Deleted "+ size + " orders");

        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

    }
}
