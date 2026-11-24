package taskweave.storage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import taskweave.model.FixedTask;
import taskweave.model.PerUnitTask;
import taskweave.model.Task;
import taskweave.model.Workflow;

// file format, one task per line in input order:
//   FIXED|Chop|10|Wash
//   PER_UNIT|Dumplings|4,5|
// fields: type | name | minutes (or units,minutesPerUnit) | prerequisites separated by commas
// lines starting with # and empty lines are skipped
public class WorkflowFileIO {

    public static void save(Workflow w, File f) throws IOException {
        try (PrintWriter out = new PrintWriter(f, StandardCharsets.UTF_8)) {
            out.println("# TaskWeave workflow");
            for (Task t : w.getTasks()) {
                out.println(t.getType() + "|" + t.getName() + "|" + durationText(t) + "|"
                        + String.join(",", t.getPrerequisites()));
            }
        }
    }

    public static Workflow load(File f) throws IOException {
        Workflow w = new Workflow();
        try (Scanner in = new Scanner(f, StandardCharsets.UTF_8)) {
            int lineNo = 0;
            while (in.hasNextLine()) {
                String line = in.nextLine();
                lineNo++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                try {
                    w.addTask(parseLine(line));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Line " + lineNo + ": " + e.getMessage());
                }
            }
        }
        return w;
    }

    private static String durationText(Task t) {
        if (t instanceof PerUnitTask) {
            PerUnitTask p = (PerUnitTask) t;
            return p.getUnits() + "," + p.getMinutesPerUnit();
        }
        return String.valueOf(t.getDuration());
    }

    private static Task parseLine(String line) {
        // -1 keeps the last field even when it is empty
        String[] parts = line.split("\\|", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("expected 4 fields separated by |, found " + parts.length);
        }
        String type = parts[0].trim();
        String name = parts[1];
        String duration = parts[2].trim();

        Task task;
        try {
            if (type.equals("FIXED")) {
                task = new FixedTask(name, Integer.parseInt(duration));
            } else if (type.equals("PER_UNIT")) {
                String[] nums = duration.split(",");
                if (nums.length != 2) {
                    throw new IllegalArgumentException("PER_UNIT needs units,minutesPerUnit");
                }
                task = new PerUnitTask(name, Integer.parseInt(nums[0].trim()), Integer.parseInt(nums[1].trim()));
            } else {
                throw new IllegalArgumentException("unknown task type " + type);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("duration is not a whole number: " + duration);
        }

        if (!parts[3].isBlank()) {
            for (String p : parts[3].split(",")) {
                task.addPrerequisite(p);
            }
        }
        return task;
    }
}
