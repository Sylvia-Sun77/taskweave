package taskweave.graph;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private final boolean valid;
    private final List<String> order;
    private final List<String> cycle;
    private final String message;

    // use the three static methods below instead of new
    private ValidationResult(boolean valid, List<String> order, List<String> cycle, String message) {
        this.valid = valid;
        this.order = List.copyOf(order);
        this.cycle = List.copyOf(cycle);
        this.message = message;
    }

    public static ValidationResult valid(List<String> order) {
        String msg;
        if (order.isEmpty()) {
            msg = "No tasks yet";
        } else {
            msg = "No cycles. Valid order: " + String.join(" -> ", order);
        }
        return new ValidationResult(true, order, new ArrayList<>(), msg);
    }

    public static ValidationResult cycle(List<String> cycle) {
        String msg = "Cycle found: " + String.join(" -> ", cycle) + " -> " + cycle.get(0);
        return new ValidationResult(false, new ArrayList<>(), cycle, msg);
    }

    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, new ArrayList<>(), new ArrayList<>(), message);
    }

    public boolean isValid() {
        return valid;
    }

    public boolean hasCycle() {
        return !cycle.isEmpty();
    }

    // execution order, empty when the workflow is not valid
    public List<String> getOrder() {
        return order;
    }

    // tasks on the cycle in execution direction, empty when there is no cycle
    public List<String> getCycle() {
        return cycle;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
