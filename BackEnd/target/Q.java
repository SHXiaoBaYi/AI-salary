import java.sql.*;
public class Q {
  public static void main(String[] a) throws Exception {
    String url = "jdbc:mysql://127.0.0.1:13306/base_admin?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    try (Connection c = DriverManager.getConnection(url, a[0], a[1]);
         Statement s = c.createStatement()) {
      ResultSet r = s.executeQuery("SELECT COUNT(*) c, SUM(attrs IS NOT NULL AND attrs<>'') attrs_n, SUM(attrs LIKE '%seriesCode%') series_n, SUM(attrs LIKE '%imageUrl%') img_n FROM geo_review_kb WHERE category='product' AND IFNULL(is_active,1)=1");
      r.next();
      System.out.println("product_active="+r.getInt(1)+" with_attrs="+r.getObject(2)+" with_series="+r.getObject(3)+" with_image="+r.getObject(4));
      ResultSet r2 = s.executeQuery("SELECT code, title, LEFT(IFNULL(attrs,''),140) a FROM geo_review_kb WHERE category='product' AND IFNULL(is_active,1)=1 ORDER BY sort_order,id LIMIT 6");
      while (r2.next()) System.out.println(r2.getString(1)+" | "+r2.getString(2)+" | "+r2.getString(3));
      ResultSet r3 = s.executeQuery("SELECT COUNT(*) FROM geo_review_kb WHERE category='product' AND code LIKE 'product-sku-%' AND IFNULL(is_active,1)=1");
      r3.next(); System.out.println("sku_count="+r3.getInt(1));
    }
  }
}
