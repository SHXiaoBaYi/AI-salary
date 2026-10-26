import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.nio.file.*;
public class PageCount {
  public static void main(String[] a) throws Exception {
    Path dir = Path.of(a[0]);
    try (var s = Files.list(dir)) {
      s.filter(p -> p.toString().toLowerCase().endsWith(".pdf")).forEach(p -> {
        try (PDDocument d = Loader.loadPDF(p.toFile())) {
          System.out.println(d.getNumberOfPages() + "\t" + p.getFileName() + "\t" + Files.size(p));
        } catch (Exception e) { System.out.println("ERR\t" + p.getFileName() + "\t" + e.getMessage()); }
      });
    }
  }
}