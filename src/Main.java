import java.sql.Connection;


public class Main {
    public static void main(String[] args) throws Exception {

        Connection connection = ConnectionManager.getConnection("lab.sqlite");

        if (connection != null)
        {
            SQLMethods.showItems(connection);
            SQLMethods.showOrders(connection);
            SQLMethods.showRangeOrders(connection, 100, 3);
            SQLMethods.orderToday(connection);
            SQLMethods.showOrders(connection);
            SQLMethods.deleteOrder(connection, 1, 7);
            SQLMethods.showOrders(connection);
        }
    }
}
