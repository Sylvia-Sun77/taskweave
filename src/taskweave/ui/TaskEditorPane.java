package taskweave.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import taskweave.model.FixedTask;
import taskweave.model.PerUnitTask;
import taskweave.model.Task;
import taskweave.model.Workflow;

public class TaskEditorPane extends VBox {

    private static final String NONE = "(no dependency)";
    private static final String FIXED = "Fixed minutes";
    private static final String PER_UNIT = "Per unit";

    private final MainApp app;

    private final TableView<Task> table = new TableView<>();
    private final TextField nameField = new TextField();
    private final ComboBox<String> typeBox = new ComboBox<>();
    private final TextField minutesField = new TextField();
    private final TextField unitsField = new TextField();
    private final TextField perUnitField = new TextField();
    private final HBox durationRow = new HBox(6);
    private final ComboBox<String> prereqBox = new ComboBox<>();
    private final ComboBox<String> linkTaskBox = new ComboBox<>();
    private final ComboBox<String> linkPrereqBox = new ComboBox<>();
    private final Label error = new Label();

    public TaskEditorPane(MainApp app) {
        this.app = app;
        setSpacing(8);
        setPadding(new Insets(10));
        setPrefWidth(380);

        Label title = new Label("Tasks");
        title.setStyle("-fx-font-weight: bold;");

        TableColumn<Task, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        nameCol.setPrefWidth(110);
        TableColumn<Task, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        typeCol.setPrefWidth(75);
        TableColumn<Task, String> minCol = new TableColumn<>("Min");
        minCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDuration())));
        minCol.setPrefWidth(45);
        TableColumn<Task, String> preCol = new TableColumn<>("Depends on");
        preCol.setCellValueFactory(c -> new SimpleStringProperty(String.join(", ", c.getValue().getPrerequisites())));
        preCol.setPrefWidth(120);
        table.getColumns().add(nameCol);
        table.getColumns().add(typeCol);
        table.getColumns().add(minCol);
        table.getColumns().add(preCol);
        table.setPrefHeight(220);
        table.setPlaceholder(new Label("No tasks yet"));

        Button deleteBtn = new Button("Delete selected task");
        deleteBtn.setOnAction(e -> deleteSelected());

        nameField.setPromptText("Task name, e.g. Plate food");
        typeBox.getItems().addAll(FIXED, PER_UNIT);
        typeBox.setValue(FIXED);
        typeBox.setOnAction(e -> updateDurationFields());
        minutesField.setPromptText("Minutes");
        unitsField.setPromptText("Units");
        perUnitField.setPromptText("Minutes per unit");
        prereqBox.setPromptText("Depends on");
        Button addBtn = new Button("Add task");
        addBtn.setOnAction(e -> addTask());

        linkTaskBox.setPromptText("Task");
        linkPrereqBox.setPromptText("Must wait for");
        Button linkBtn = new Button("Link");
        linkBtn.setOnAction(e -> link());
        Button unlinkBtn = new Button("Unlink");
        unlinkBtn.setOnAction(e -> unlink());

        error.setTextFill(Color.web("#c62828"));
        error.setWrapText(true);

        getChildren().addAll(
                title, table, deleteBtn,
                new Separator(),
                new Label("Add a task"), nameField, typeBox, durationRow, prereqBox, addBtn,
                new Separator(),
                new Label("Add or remove a dependency"),
                new HBox(6, linkTaskBox, new Label("waits for"), linkPrereqBox),
                new HBox(6, linkBtn, unlinkBtn),
                error);
        updateDurationFields();
    }

    // called by MainApp after every change
    public void refresh(Workflow w) {
        table.getItems().setAll(w.getTasks());
        List<String> names = new ArrayList<>();
        for (Task t : w.getTasks()) {
            names.add(t.getName());
        }
        List<String> withNone = new ArrayList<>();
        withNone.add(NONE);
        withNone.addAll(names);
        prereqBox.getItems().setAll(withNone);
        linkTaskBox.getItems().setAll(names);
        linkPrereqBox.getItems().setAll(names);
    }

    // show "Minutes" for a fixed task, or "Units x Minutes per unit" for a per-unit task
    private void updateDurationFields() {
        // PER_UNIT.equals(...) instead of getValue().equals(...): no NullPointerException if nothing is picked
        if (PER_UNIT.equals(typeBox.getValue())) {
            durationRow.getChildren().setAll(unitsField, new Label("x"), perUnitField);
        } else {
            durationRow.getChildren().setAll(minutesField);
        }
    }

    private void addTask() {
        error.setText("");  // clear the old error message first
        String name = nameField.getText().trim();
        // validation: check here first to show a short message next to the form,
        // the model checks the same things again in case another class calls it
        if (name.isEmpty()) {
            error.setText("Enter a task name");
            return;
        }
        if (app.getWorkflow().contains(name)) {
            error.setText("That name is already taken");
            return;
        }

        Task task;
        try {
            if (PER_UNIT.equals(typeBox.getValue())) {
                task = new PerUnitTask(name, readPositive(unitsField, "Units"), readPositive(perUnitField, "Minutes per unit"));
            } else {
                task = new FixedTask(name, readPositive(minutesField, "Minutes"));
            }
            String pre = prereqBox.getValue();
            if (pre != null && !pre.equals(NONE)) {
                task.addPrerequisite(pre);
            }
        } catch (IllegalArgumentException ex) {
            error.setText(ex.getMessage());
            return;
        }

        // a lambda can only use variables that never change after being set (effectively final),
        // task is assigned inside the if/else, so copy it into a new variable first
        Task newTask = task;
        // only clear the form when the task was really added, so the user can fix a typo otherwise
        if (app.applyChange("Added " + name, w -> w.addTask(newTask))) {
            nameField.clear();
            minutesField.clear();
            unitsField.clear();
            perUnitField.clear();
            prereqBox.setValue(null);
        }
    }

    // read a whole number > 0 from a text field, or throw with a message for the error label
    private int readPositive(TextField field, String label) {
        int value;      // declared outside of try, so it can be used after the try catch block
        try {
            value = Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            // e.g. "", "abc", "2.5", or a number too big for int (> 2,147,483,647)
            throw new IllegalArgumentException(label + " must be a whole number");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(label + " must be greater than 0");
        }
        return value;
    }

    private void deleteSelected() {
        error.setText("");
        Task selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            error.setText("Select a task in the table first");
            return;
        }
        String name = selected.getName();
        app.applyChange("Deleted " + name, w -> w.removeTask(name));
    }

    private void link() {
        error.setText("");
        String task = linkTaskBox.getValue();
        String pre = linkPrereqBox.getValue();
        if (task == null || pre == null) {
            error.setText("Pick both tasks first");
            return;
        }
        app.applyChange(task + " now waits for " + pre, w -> w.addDependency(task, pre));
    }

    private void unlink() {
        error.setText("");
        String task = linkTaskBox.getValue();
        String pre = linkPrereqBox.getValue();
        if (task == null || pre == null) {
            error.setText("Pick both tasks first");
            return;
        }
        Task t = app.getWorkflow().getTask(task);
        if (t == null || !t.getPrerequisites().contains(pre)) {
            error.setText(task + " does not wait for " + pre);
            return;
        }
        app.applyChange(task + " no longer waits for " + pre, w -> w.removeDependency(task, pre));
    }
}
