package taskweave.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import taskweave.graph.LayoutCalculator;
import taskweave.graph.ValidationResult;
import taskweave.model.Task;
import taskweave.model.Workflow;

public class GraphView extends Pane {

    private static final double BOX_W = 130;
    private static final double BOX_H = 36;
    private static final double COL_GAP = 60;
    private static final double ROW_GAP = 20;
    private static final double MARGIN = 20;
    private static final double CURVE_DROP = 50;

    private static final Color EDGE = Color.web("#666666");
    private static final Color BOX_FILL = Color.web("#f4f4f4");
    private static final Color BOX_LINE = Color.web("#999999");
    private static final Color RED = Color.web("#c62828");
    private static final Color RED_FILL = Color.web("#fdecea");

    // columns from the last valid check, reused when there is a cycle
    private Map<String, Integer> lastLevels = new HashMap<>();

    public GraphView() {
        setStyle("-fx-background-color: white;");
    }

    public void show(Workflow w, ValidationResult r) {
        getChildren().clear();
        if (w.isEmpty()) {
            getChildren().add(new Text(MARGIN, MARGIN + 16, "No tasks yet"));
            setPrefSize(300, 80);
            return;
        }

        Map<String, Integer> levels;
        if (r.isValid()) {
            levels = LayoutCalculator.computeLevels(w, r.getOrder());
            lastLevels = levels;
        } else {
            levels = new HashMap<>();
            for (Task t : w.getTasks()) {
                levels.put(t.getName(), lastLevels.getOrDefault(t.getName(), 0));
            }
        }

        // top-left corner of each box: column from levels, row = how many are already in that column
        Map<Integer, Integer> rowsUsed = new HashMap<>();
        Map<String, double[]> pos = new HashMap<>();
        int maxCol = 0;
        int maxRows = 0;
        for (Task t : w.getTasks()) {
            int col = levels.get(t.getName());
            int row = rowsUsed.getOrDefault(col, 0);
            rowsUsed.put(col, row + 1);
            double x = MARGIN + col * (BOX_W + COL_GAP);
            double y = MARGIN + row * (BOX_H + ROW_GAP);
            pos.put(t.getName(), new double[] { x, y });
            maxCol = Math.max(maxCol, col);
            maxRows = Math.max(maxRows, row + 1);
        }

        // edges first so the boxes are drawn on top of them
        List<String> cycle = r.getCycle();
        boolean hasCurve = false;
        for (Task t : w.getTasks()) {
            for (String p : t.getPrerequisites()) {
                double[] from = pos.get(p);
                double[] to = pos.get(t.getName());
                if (from == null) {
                    continue;
                }
                Color c = isCycleEdge(cycle, p, t.getName()) ? RED : EDGE;
                if (levels.get(t.getName()) > levels.get(p)) {
                    drawArrow(from[0] + BOX_W, from[1] + BOX_H / 2, to[0], to[1] + BOX_H / 2, c);
                } else {
                    drawCurve(from, to, c);
                    hasCurve = true;
                }
            }
        }

        for (Task t : w.getTasks()) {
            double[] p = pos.get(t.getName());
            boolean red = cycle.contains(t.getName());
            Rectangle box = new Rectangle(p[0], p[1], BOX_W, BOX_H);
            box.setArcWidth(10);
            box.setArcHeight(10);
            box.setFill(red ? RED_FILL : BOX_FILL);
            box.setStroke(red ? RED : BOX_LINE);

            Text label = new Text(shorten(t.getName()) + "  " + t.getDuration());
            label.setFill(red ? RED : Color.BLACK);
            double textW = label.getLayoutBounds().getWidth();
            label.setX(p[0] + (BOX_W - textW) / 2);
            label.setY(p[1] + BOX_H / 2 + 4);
            getChildren().addAll(box, label);
        }

        double width = MARGIN * 2 + (maxCol + 1) * (BOX_W + COL_GAP) - COL_GAP;
        double height = MARGIN * 2 + maxRows * (BOX_H + ROW_GAP) + (hasCurve ? CURVE_DROP : 0);
        setPrefSize(width, height);
        setMinSize(width, height);
    }

    // true when p -> t is one of the steps around the cycle
    private boolean isCycleEdge(List<String> cycle, String p, String t) {
        int i = cycle.indexOf(p);
        return i >= 0 && cycle.get((i + 1) % cycle.size()).equals(t);
    }

    private void drawArrow(double x1, double y1, double x2, double y2, Color c) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(c);
        getChildren().addAll(line, arrowHead(x2, y2, Math.atan2(y2 - y1, x2 - x1), c));
    }

    // an edge that points back to an earlier column, drawn as a dashed curve under the boxes
    private void drawCurve(double[] from, double[] to, Color c) {
        double sx = from[0] + BOX_W / 2;
        double sy = from[1] + BOX_H;
        double ex = to[0] + BOX_W / 2;
        double ey = to[1] + BOX_H;
        double low = Math.max(sy, ey) + CURVE_DROP;
        CubicCurve curve = new CubicCurve(sx, sy, sx, low, ex, low, ex, ey);
        curve.setFill(Color.TRANSPARENT);
        curve.setStroke(c);
        curve.getStrokeDashArray().addAll(6.0, 4.0);
        getChildren().addAll(curve, arrowHead(ex, ey, -Math.PI / 2, c));
    }

    private Polygon arrowHead(double x, double y, double angle, Color c) {
        double len = 10;
        double half = 5;
        double bx = x - len * Math.cos(angle);
        double by = y - len * Math.sin(angle);
        Polygon head = new Polygon(
                x, y,
                bx + half * Math.sin(angle), by - half * Math.cos(angle),
                bx - half * Math.sin(angle), by + half * Math.cos(angle));
        head.setFill(c);
        return head;
    }

    private String shorten(String name) {
        return name.length() > 12 ? name.substring(0, 10) + "..." : name;
    }
}
