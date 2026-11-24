package taskweave.ui;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import taskweave.graph.DependencyChecker;
import taskweave.graph.ValidationResult;
import taskweave.model.SampleWorkflows;
import taskweave.model.Workflow;
import taskweave.scheduler.InputOrderStrategy;
import taskweave.scheduler.LongestFirstStrategy;
import taskweave.scheduler.ScheduleResult;
import taskweave.scheduler.SchedulingStrategy;
import taskweave.scheduler.Simulator;
import taskweave.storage.UndoManager;
import taskweave.storage.WorkflowFileIO;

public class MainApp extends Application {

    private Workflow workflow = SampleWorkflows.cooking();
    private final UndoManager history = new UndoManager();
    private final List<SchedulingStrategy> strategies = List.of(new InputOrderStrategy(), new LongestFirstStrategy());

    private Stage stage;
    private TaskEditorPane editor;
    private GraphView graphView;
    private ScheduleView scheduleView;
    private ComboBox<Integer> workerBox;
    private ComboBox<String> ruleBox;
    private Label status;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        editor = new TaskEditorPane(this);
        graphView = new GraphView();
        scheduleView = new ScheduleView();
        status = new Label();

        Button newBtn = new Button("New");
        newBtn.setOnAction(e -> replaceWorkflow(new Workflow(), "Started an empty workflow"));
        Button sampleBtn = new Button("Sample");
        sampleBtn.setOnAction(e -> replaceWorkflow(SampleWorkflows.cooking(), "Loaded the cooking sample"));
        Button openBtn = new Button("Open");
        openBtn.setOnAction(e -> open());
        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> save());
        Button undoBtn = new Button("Undo");
        undoBtn.setOnAction(e -> undo());
        Button redoBtn = new Button("Redo");
        redoBtn.setOnAction(e -> redo());

        workerBox = new ComboBox<>();
        workerBox.getItems().addAll(1, 2, 3, 4, 5);
        workerBox.setValue(2);
        workerBox.setOnAction(e -> refreshAll());
        ruleBox = new ComboBox<>();
        for (SchedulingStrategy s : strategies) {
            ruleBox.getItems().add(s.getName());
        }
        ruleBox.setValue(strategies.get(0).getName());
        ruleBox.setOnAction(e -> refreshAll());

        Button checkBtn = new Button("Check");
        checkBtn.setOnAction(e -> check());
        Button runBtn = new Button("Run");
        runBtn.setOnAction(e -> run());

        ToolBar toolbar = new ToolBar(
                newBtn, sampleBtn, openBtn, saveBtn,
                new Separator(), undoBtn, redoBtn,
                new Separator(), new Label("Workers"), workerBox, new Label("Rule"), ruleBox,
                new Separator(), checkBtn, runBtn);

        Label graphTitle = new Label("Dependency graph");
        graphTitle.setStyle("-fx-font-weight: bold;");
        ScrollPane graphScroll = new ScrollPane(graphView);
        VBox graphBox = new VBox(8, graphTitle, graphScroll);
        graphBox.setPadding(new Insets(10));
        VBox.setVgrow(graphScroll, Priority.ALWAYS);

        VBox center = new VBox(graphBox, scheduleView);
        VBox.setVgrow(graphBox, Priority.ALWAYS);

        ScrollPane editorScroll = new ScrollPane(editor);
        editorScroll.setFitToWidth(true);

        status.setPadding(new Insets(6, 10, 6, 10));

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setLeft(editorScroll);
        root.setCenter(center);
        root.setBottom(status);

        stage.setTitle("TaskWeave");
        stage.setScene(new Scene(root, 1200, 800));
        stage.show();

        refreshAll();
        showStatus("Loaded the cooking sample", false);
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    // every edit goes through here: run it, keep a snapshot for undo, redraw everything.
    // returns false if the edit was rejected
    public boolean applyChange(String doneMessage, Consumer<Workflow> change) {
        Workflow before = workflow.copy();
        try {
            change.accept(workflow);
        } catch (IllegalArgumentException ex) {
            workflow = before;
            refreshAll();
            showStatus(ex.getMessage(), true);
            return false;
        }
        history.beforeChange(before);
        ValidationResult r = refreshAll();
        if (r.isValid()) {
            showStatus(doneMessage, false);
        } else {
            showStatus(r.getMessage(), true);
        }
        return true;
    }

    private void replaceWorkflow(Workflow next, String message) {
        history.beforeChange(workflow);
        workflow = next;
        refreshAll();
        showStatus(message, false);
    }

    // redraws the table, the graph and the schedule from the current workflow
    private ValidationResult refreshAll() {
        ValidationResult r = DependencyChecker.check(workflow);
        editor.refresh(workflow);
        graphView.show(workflow, r);
        if (workflow.isEmpty()) {
            scheduleView.showMessage("Add tasks to see a schedule");
        } else if (!r.isValid()) {
            scheduleView.showMessage("Fix the dependencies before running");
        } else {
            List<ScheduleResult> results = Simulator.runAll(workflow, strategies, workerBox.getValue());
            scheduleView.show(results.get(selectedRuleIndex()), results);
        }
        return r;
    }

    private int selectedRuleIndex() {
        int i = ruleBox.getItems().indexOf(ruleBox.getValue());
        return Math.max(i, 0);
    }

    private void check() {
        ValidationResult r = refreshAll();
        showStatus(r.getMessage(), !r.isValid());
    }

    private void run() {
        ValidationResult r = refreshAll();
        if (workflow.isEmpty()) {
            showStatus("Add tasks first", true);
        } else if (!r.isValid()) {
            showStatus("Fix the dependencies before running: " + r.getMessage(), true);
        } else {
            ScheduleResult result = Simulator.run(workflow, strategies.get(selectedRuleIndex()), workerBox.getValue());
            showStatus("Ran " + result, false);
        }
    }

    private void undo() {
        if (!history.canUndo()) {
            showStatus("Nothing to undo", false);
            return;
        }
        workflow = history.undo(workflow);
        refreshAll();
        showStatus("Undone", false);
    }

    private void redo() {
        if (!history.canRedo()) {
            showStatus("Nothing to redo", false);
            return;
        }
        workflow = history.redo(workflow);
        refreshAll();
        showStatus("Redone", false);
    }

    private void open() {
        FileChooser chooser = makeChooser("Open workflow");
        File f = chooser.showOpenDialog(stage);
        if (f == null) {
            return;
        }
        try {
            replaceWorkflow(WorkflowFileIO.load(f), "Opened " + f.getName());
        } catch (IOException | IllegalArgumentException ex) {
            showStatus("Could not open " + f.getName() + ": " + ex.getMessage(), true);
        }
    }

    private void save() {
        FileChooser chooser = makeChooser("Save workflow");
        chooser.setInitialFileName("workflow.txt");
        File f = chooser.showSaveDialog(stage);
        if (f == null) {
            return;
        }
        try {
            WorkflowFileIO.save(workflow, f);
            showStatus("Saved to " + f.getName(), false);
        } catch (IOException ex) {
            showStatus("Could not save: " + ex.getMessage(), true);
        }
    }

    private FileChooser makeChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TaskWeave workflow (*.txt)", "*.txt"));
        File samples = new File("samples");
        if (samples.isDirectory()) {
            chooser.setInitialDirectory(samples);
        }
        return chooser;
    }

    private void showStatus(String message, boolean isError) {
        status.setText(message);
        status.setTextFill(isError ? Color.web("#c62828") : Color.web("#444444"));
    }
}
