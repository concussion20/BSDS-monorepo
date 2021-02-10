import java.io.File;
import java.io.IOException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.HistogramDataset;

public class Tutorial {
  public static void main(String[] args) throws IOException {

    double[] values = { 95, 49, 14, 59, 50, 66, 47, 40, 1, 67,
        12, 58, 28, 63, 14, 9, 31, 17, 94, 71,
        49, 64, 73, 97, 15, 63, 10, 12, 31, 62,
        93, 49, 74, 90, 59, 14, 15, 88, 26, 57,
        77, 44, 58, 91, 10, 67, 57, 19, 88, 84
    };


    HistogramDataset dataset = new HistogramDataset();
    dataset.addSeries("key", values, 20);

    JFreeChart histogram = ChartFactory.createHistogram("JFreeChart Histogram",
        "Data", "Frequency", dataset);

    ChartUtils
        .saveChartAsPNG(new File("histogram.png"), histogram, 600, 400);
  }
}
