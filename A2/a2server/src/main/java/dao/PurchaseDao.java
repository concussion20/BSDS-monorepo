package dao;

import com.mysql.cj.MysqlType;
import dbcp.DBCPDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import model.Purchase;
import org.apache.commons.dbcp2.BasicDataSource;

public class PurchaseDao {
  private static BasicDataSource dataSource;

  public PurchaseDao() {
    dataSource = DBCPDataSource.getDataSource();
  }

  public boolean createPurchase(Purchase purchase, String storeID, String custID, String date) {
    boolean flag = true;

    Connection conn = null;
    PreparedStatement preparedStatement = null;
    String insertQueryStatement = "INSERT INTO `purchase` (`items`, `storeid`, `custid`, `date`) " +
        "VALUES (?,?,?,?)";
    try {
      conn = dataSource.getConnection();
      preparedStatement = conn.prepareStatement(insertQueryStatement);
      preparedStatement.setObject(1, purchase.toString(), MysqlType.JSON);
      preparedStatement.setString(2, storeID);
      preparedStatement.setString(3, custID);
      preparedStatement.setString(4, date);

      // execute insert SQL statement
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
      flag = false;
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
    return flag;
  }
}
