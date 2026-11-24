package taskweave.ui;

import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import taskweave.scheduler.ScheduleResult;
import taskweave.scheduler.ScheduledSegment;

public class ScheduleView extends VBox {

    private static final double LABEL_W = 80;
    private static final double CHART_W = 640;
    private static final double ROW_H = 32;
    private static final double BAR_H = 24;
    private static final double TOP = 6;

    private static final Color BAR_FILL = Color.web("#dbe8f7");
    private static final Color BAR_LINE = Color.web("#3d6fb5");
    private static final Color BAR_TEXT = Color.web("#1f3f6b");
    private static final Color QUIET = Color.web("#666666");

    private final Pane chart = new Pane();
    private final Label totalLabel = new Label();
    private final Label idleLabel = new Label();
    private final Label waitLabel = new Label();
    private final Label compareLabel = new Label();

    public ScheduleView() {
        setSpacing(8);
        setPadding(new Insets(10));
        Label title = new Label("Schedule");
        title.setStyle("-fx-font-weight: bold;");
        HBox metrics = new HBox(24, totalLabel, idleLabel, waitLabel);
        compareLabel.setTextFill(QUIET);
        getChildren().addAll(title, chart, metrics, compareLabel);
    }

    // current = the rule picked in the toolbar, all = every rule for the comparison line
    public void show(ScheduleResult current, List<ScheduleResult> all) {
        chart.getChildren().clear();

        // same scale for every rule, so switching rules shows bars getting shorter or longer
        int max = 1;
        for (ScheduleResult r : all) {
            max = Math.max(max, r.getTotalTime());
        }
        double scale = CHART_W / max;
        int workers = current.getWorkerCount();

        for (int i = 0; i < workers; i++) {
            Text name = new Text(0, TOP + i * ROW_H + BAR_H / 2 + 4, "Worker " + (i + 1));
            name.setFill(QUIET);
            chart.getChildren().add(name);
        }

        for (ScheduledSegment s : current.getSegments()) {
            double x = LABEL_W + s.getStart() * scale;
            double w = (s.getEnd() - s.getStart()) * scale;
            double y = TOP + s.getWorkerIndex() * ROW_H;
            Rectangle bar = new Rectangle(x + 1, y, Math.max(1, w - 2), BAR_H);
            bar.setArcWidth(6);
            bar.setArcHeight(6);
            bar.setFill(BAR_FILL);
            bar.setStroke(BAR_LINE);
            chart.getChildren().add(bar);

            Text label = new Text(s.getTaskName());
            label.setFill(BAR_TEXT);
            double textW = label.getLayoutBounds().getWidth();
            // only write the name when it fits inside the bar
            if (textW < w - 6) {
                label.setX(x + (w - textW) / 2);
                label.setY(y + BAR_H / 2 + 4);
                chart.getChildren().add(label);
            }
        }

        double axisY = TOP + workers * ROW_H + 2;
        Line axis = new Line(LABEL_W, axisY, LABEL_W + CHART_W, axisY);
        axis.setStroke(QUIET);
        chart.getChildren().add(axis);

        int step = max > 300 ? 50 : (max > 100 ? 20 : 10);
        for (int v = 0; v <= max; v += step) {
            Text tick = new Text(String.valueOf(v));
            tick.setFill(QUIET);
            tick.setX(LABEL_W + v * scale - tick.getLayoutBounds().getWidth() / 2);
            tick.setY(axisY + 16);
            chart.getChildren().add(tick);
        }

        // dashed line where the current rule finishes
        double endX = LABEL_W + current.getTotalTime() * scale;
        Line end = new Line(endX, TOP - 4, endX, axisY);
        end.setStroke(BAR_LINE);
        end.getStrokeDashArray().addAll(4.0, 4.0);
        chart.getChildren().add(end);

        double height = axisY + 24;
        chart.setPrefSize(LABEL_W + CHART_W + 30, height);
        chart.setMinHeight(height);

        totalLabel.setText("Total time: " + current.getTotalTime() + " min");
        idleLabel.setText("Worker idle time: " + current.getIdleTime() + " min");
        waitLabel.setText("Average wait: " + String.format("%.1f", current.getAverageWait()) + " min");
        compareLabel.setText(compareText(all));
    }

    public void showMessage(String message) {
        Text t = new Text(0, 20, message);
        t.setFill(QUIET);
        chart.getChildren().setAll(t);
        chart.setPrefSize(LABEL_W + CHART_W + 30, 40);
        chart.setMinHeight(40);
        totalLabel.setText("Total time: -");
        idleLabel.setText("Worker idle time: -");
        waitLabel.setText("Average wait: -");
        compareLabel.setText("");
    }

    private String compareText(List<ScheduleResult> all) {
        StringBuilder sb = new StringBuilder();
        ScheduleResult best = all.get(0);
        for (ScheduleResult r : all) {
            if (sb.length() > 0) {
                sb.append("   |   ");
            }
            sb.append(r.getStrategyName()).append(": ").append(r.getTotalTime()).append(" min");
            if (r.getTotalTime() < best.getTotalTime()) {
                best = r;
            }
        }
        int saved = 0;
        for (ScheduleResult r : all) {
            saved = Math.max(saved, r.getTotalTime() - best.getTotalTime());
        }
        if (saved > 0) {
            sb.append("   |   ").append(best.getStrategyName()).append(" saves ").append(saved).append(" min");
        } else {
            sb.append("   |   same result");
        }
        return sb.toString();
    }
}
