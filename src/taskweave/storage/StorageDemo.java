package taskweave.storage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import taskweave.model.FixedTask;
import taskweave.model.PerUnitTask;
import taskweave.model.SampleWorkflows;
import taskweave.model.Task;
import taskweave.model.Workflow;

public class StorageDemo {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        // save and load back
        Workflow w = SampleWorkflows.cooking();
        w.addTask(new PerUnitTask("Dumplings", 4, 5));
        w.addDependency("Dumplings", "Wash");

        File f = File.createTempFile("taskweave", ".txt");
        f.deleteOnExit();
        WorkflowFileIO.save(w, f);
        Workflow loaded = WorkflowFileIO.load(f);

        check("loaded workflow has 6 tasks", loaded.size() == 6);
        check("input order is kept", loaded.getTasks().get(5).getName().equals("Dumplings"));
        check("dependencies are kept", loaded.getTask("Stir-fry").getPrerequisites().contains("Chop"));
        Task d = loaded.getTask("Dumplings");
        check("per-unit task comes back as per-unit", d instanceof PerUnitTask && d.getDuration() == 20);
        check("fixed task keeps its minutes", loaded.getTask("Soup").getDuration() == 40);

        // a broken file
        File bad = File.createTempFile("taskweave-bad", ".txt");
        bad.deleteOnExit();
        try (PrintWriter out = new PrintWriter(bad, StandardCharsets.UTF_8)) {
            out.println("FIXED|Wash|5|");
            out.println("FIXED|Chop|ten|Wash");
        }
        try {
            WorkflowFileIO.load(bad);
            check("bad line is reported", false);
        } catch (IllegalArgumentException e) {
            System.out.println("error message: " + e.getMessage());
            check("bad line is reported with its line number", e.getMessage().startsWith("Line 2"));
        }

        // a file edited by hand: spaces around names and an extra comma at the end
        File spaced = File.createTempFile("taskweave-spaced", ".txt");
        spaced.deleteOnExit();
        try (PrintWriter out = new PrintWriter(spaced, StandardCharsets.UTF_8)) {
            out.println("FIXED| Wash |5|");
            out.println("FIXED|Chop|10| Wash ,");
        }
        Workflow trimmed = WorkflowFileIO.load(spaced);
        check("spaces around a name are trimmed", trimmed.contains("Wash"));
        check("prerequisite with spaces and extra comma still found",
                trimmed.getTask("Chop").getPrerequisites().equals(java.util.List.of("Wash")));

        // undo and redo
        UndoManager history = new UndoManager();
        Workflow current = SampleWorkflows.cooking();

        history.beforeChange(current);
        current.addTask(new FixedTask("Tea", 3));
        history.beforeChange(current);
        current.removeTask("Soup");

        current = history.undo(current);
        check("undo brings Soup back", current.contains("Soup") && current.contains("Tea"));
        current = history.undo(current);
        check("second undo removes Tea", !current.contains("Tea") && current.size() == 5);
        check("nothing left to undo", !history.canUndo());
        current = history.redo(current);
        check("redo adds Tea again", current.contains("Tea"));

        history.beforeChange(current);
        current.addTask(new FixedTask("Rest", 5));
        check("a new change clears redo", !history.canRedo());

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("PASS  " + label);
        } else {
            failed++;
            System.out.println("FAIL  " + label);
        }
    }
}
