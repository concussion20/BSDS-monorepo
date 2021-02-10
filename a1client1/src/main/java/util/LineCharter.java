package util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class LineCharter extends JFrame {
  public LineCharter(int[] xVals, double[] yVals, String title, String xLabel, String yLabel
      , String savedFileName) throws IOException {
    initUI(xVals, yVals, title, xLabel, yLabel, savedFileName);
  }

  private void initUI(int[] xVals, double[] yVals, String title, String xLabel, String yLabel
      , String savedFileName) throws IOException {
    XYDataset dataset = createDataset(xVals, yVals);
    JFreeChart chart = createChart(dataset, title, xLabel, yLabel);

    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    chartPanel.setBackground(Color.white);
    add(chartPanel);

    pack();
    setTitle("Line chart");
    setLocationRelativeTo(null);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    ChartUtils
        .saveChartAsPNG(new File(savedFileName), chart, 600, 400);
  }

  private XYDataset createDataset(int[] xVals, double[] yVals) {
    XYSeries series = new XYSeries("Simple Client");
    for (int i = 0; i < xVals.length; i++) {
      series.add(xVals[i], yVals[i]);
    }

    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries(series);

    return dataset;
  }

  private JFreeChart createChart(XYDataset dataset, String title, String xLabel, String yLabel) {
    JFreeChart chart = ChartFactory.createXYLineChart(
        title,
        xLabel,
        yLabel,
        dataset,
        PlotOrientation.VERTICAL,
        true,
        true,
        false
    );

    XYPlot plot = chart.getXYPlot();

    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
    renderer.setSeriesPaint(0, Color.RED);
    renderer.setSeriesStroke(0, new BasicStroke(2.0f));

    plot.setRenderer(renderer);
    plot.setBackgroundPaint(Color.white);

    plot.setRangeGridlinesVisible(true);
    plot.setRangeGridlinePaint(Color.BLACK);

    plot.setDomainGridlinesVisible(true);
    plot.setDomainGridlinePaint(Color.BLACK);

    chart.getLegend().setFrame(BlockBorder.NONE);

    chart.setTitle(new TextTitle(title,
            new Font("Serif", Font.BOLD, 18)
        )
    );

    return chart;
  }

}
