import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.util.Optional;

public class LibraryManagementSystem extends Application {

    private TableView<Book> table;
    private ObservableList<Book> books;
    private FilteredList<Book> filteredBooks;

    private TextField searchField;

    private Label totalLabel;
    private Label availableLabel;
    private Label issuedLabel;

    private static final String FILE_NAME = "library_data.dat";

    @Override
    public void start(Stage stage) {

        books = FXCollections.observableArrayList();
        loadBooks();

        // ================= TABLE =================

        table = new TableView<>();

        TableColumn<Book, String> idColumn = new TableColumn<>("Book ID");
        idColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));

        TableColumn<Book, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getTitle()));

        TableColumn<Book, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getAuthor()));

        TableColumn<Book, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));

        TableColumn<Book, String> issuedToColumn = new TableColumn<>("Issued To");
        issuedToColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getIssuedTo()));

        TableColumn<Book, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().isIssued() ? "Issued" : "Available"
                ));

        idColumn.setPrefWidth(100);
        titleColumn.setPrefWidth(180);
        authorColumn.setPrefWidth(160);
        categoryColumn.setPrefWidth(130);
        issuedToColumn.setPrefWidth(150);
        statusColumn.setPrefWidth(100);

        table.getColumns().addAll(
                idColumn,
                titleColumn,
                authorColumn,
                categoryColumn,
                issuedToColumn,
                statusColumn
        );

        // ================= SEARCH =================

        searchField = new TextField();
        searchField.setPromptText("Search by ID, title, author or category");

        filteredBooks = new FilteredList<>(books, book -> true);

        table.setItems(filteredBooks);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {

            String search = newValue.toLowerCase().trim();

            filteredBooks.setPredicate(book -> {

                if (search.isEmpty()) {
                    return true;
                }

                return book.getId().toLowerCase().contains(search)
                        || book.getTitle().toLowerCase().contains(search)
                        || book.getAuthor().toLowerCase().contains(search)
                        || book.getCategory().toLowerCase().contains(search);
            });
        });

        // ================= BUTTONS =================

        Button addButton = new Button("Add Book");
        Button issueButton = new Button("Issue Book");
        Button returnButton = new Button("Return Book");
        Button deleteButton = new Button("Delete Book");
        Button refreshButton = new Button("Refresh");

        addButton.setOnAction(e -> addBook());
        issueButton.setOnAction(e -> issueBook());
        returnButton.setOnAction(e -> returnBook());
        deleteButton.setOnAction(e -> deleteBook());
        refreshButton.setOnAction(e -> refreshTable());

        HBox buttons = new HBox(
                10,
                addButton,
                issueButton,
                returnButton,
                deleteButton,
                refreshButton
        );

        // ================= STATISTICS =================

        totalLabel = new Label();
        availableLabel = new Label();
        issuedLabel = new Label();

        HBox statistics = new HBox(
                30,
                totalLabel,
                availableLabel,
                issuedLabel
        );

        // ================= LAYOUT =================

        Label heading = new Label("Library Management System");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        VBox root = new VBox(
                15,
                heading,
                searchField,
                buttons,
                table,
                statistics
        );

        root.setPadding(new Insets(20));

        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        updateStatistics();

        Scene scene = new Scene(root, 1000, 650);

        stage.setTitle("Library Management System");
        stage.setScene(scene);
        stage.show();
    }

    // ================= ADD BOOK =================

    private void addBook() {

        Dialog<Book> dialog = new Dialog<>();

        dialog.setTitle("Add Book");
        dialog.setHeaderText("Enter Book Details");

        ButtonType addButtonType =
                new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                addButtonType,
                ButtonType.CANCEL
        );

        TextField idField = new TextField();
        TextField titleField = new TextField();
        TextField authorField = new TextField();
        TextField categoryField = new TextField();

        idField.setPromptText("Book ID");
        titleField.setPromptText("Title");
        authorField.setPromptText("Author");
        categoryField.setPromptText("Category");

        GridPane grid = new GridPane();

        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Book ID:"), 0, 0);
        grid.add(idField, 1, 0);

        grid.add(new Label("Title:"), 0, 1);
        grid.add(titleField, 1, 1);

        grid.add(new Label("Author:"), 0, 2);
        grid.add(authorField, 1, 2);

        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {

            if (button == addButtonType) {

                if (idField.getText().trim().isEmpty()
                        || titleField.getText().trim().isEmpty()
                        || authorField.getText().trim().isEmpty()
                        || categoryField.getText().trim().isEmpty()) {

                    showAlert("Error", "Please fill all fields.");
                    return null;
                }

                return new Book(
                        idField.getText().trim(),
                        titleField.getText().trim(),
                        authorField.getText().trim(),
                        categoryField.getText().trim()
                );
            }

            return null;
        });

        Optional<Book> result = dialog.showAndWait();

        result.ifPresent(book -> {

            for (Book b : books) {

                if (b.getId().equalsIgnoreCase(book.getId())) {

                    showAlert(
                            "Error",
                            "Book ID already exists."
                    );

                    return;
                }
            }

            books.add(book);

            saveBooks();

            refreshTable();
        });
    }

    // ================= ISSUE BOOK =================

    private void issueBook() {

        Book selectedBook = table.getSelectionModel().getSelectedItem();

        if (selectedBook == null) {

            showAlert(
                    "No Selection",
                    "Please select a book first."
            );

            return;
        }

        if (selectedBook.isIssued()) {

            showAlert(
                    "Already Issued",
                    "This book is already issued."
            );

            return;
        }

        TextInputDialog dialog =
                new TextInputDialog();

        dialog.setTitle("Issue Book");
        dialog.setHeaderText(
                "Issue: " + selectedBook.getTitle()
        );

        dialog.setContentText("Issued To:");

        Optional<String> result =
                dialog.showAndWait();

        result.ifPresent(name -> {

            if (!name.trim().isEmpty()) {

                selectedBook.setIssued(true);
                selectedBook.setIssuedTo(name.trim());

                saveBooks();
                refreshTable();
            }
        });
    }

    // ================= RETURN BOOK =================

    private void returnBook() {

        Book selectedBook =
                table.getSelectionModel().getSelectedItem();

        if (selectedBook == null) {

            showAlert(
                    "No Selection",
                    "Please select a book first."
            );

            return;
        }

        if (!selectedBook.isIssued()) {

            showAlert(
                    "Not Issued",
                    "This book is currently available."
            );

            return;
        }

        selectedBook.setIssued(false);
        selectedBook.setIssuedTo("");

        saveBooks();

        refreshTable();
    }

    // ================= DELETE BOOK =================

    private void deleteBook() {

        Book selectedBook =
                table.getSelectionModel().getSelectedItem();

        if (selectedBook == null) {

            showAlert(
                    "No Selection",
                    "Please select a book first."
            );

            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmation.setTitle("Delete Book");
        confirmation.setHeaderText("Delete Book?");
        confirmation.setContentText(
                "Are you sure you want to delete "
                        + selectedBook.getTitle() + "?"
        );

        Optional<ButtonType> result =
                confirmation.showAndWait();

        if (result.isPresent()
                && result.get() == ButtonType.OK) {

            books.remove(selectedBook);

            saveBooks();

            refreshTable();
        }
    }

    // ================= REFRESH =================

    private void refreshTable() {

        table.refresh();

        updateStatistics();
    }

    // ================= STATISTICS =================

    private void updateStatistics() {

        int total = books.size();

        int issued = 0;

        for (Book book : books) {

            if (book.isIssued()) {
                issued++;
            }
        }

        int available = total - issued;

        totalLabel.setText("Total Books: " + total);
        availableLabel.setText("Available: " + available);
        issuedLabel.setText("Issued: " + issued);
    }

    // ================= SAVE =================

    private void saveBooks() {

        try (ObjectOutputStream output =
                     new ObjectOutputStream(
                             new FileOutputStream(FILE_NAME))) {

            output.writeObject(
                    new java.util.ArrayList<>(books)
            );

        } catch (IOException e) {

            showAlert(
                    "Save Error",
                    "Could not save library data."
            );
        }
    }

    // ================= LOAD =================

    private void loadBooks() {

        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return;
        }

        try (ObjectInputStream input =
                     new ObjectInputStream(
                             new FileInputStream(file))) {

            java.util.ArrayList<Book> loadedBooks =
                    (java.util.ArrayList<Book>) input.readObject();

            books.addAll(loadedBooks);

        } catch (Exception e) {

            System.out.println(
                    "No previous library data found."
            );
        }
    }

    // ================= ALERT =================

    private void showAlert(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // ================= MAIN =================

    public static void main(String[] args) {

        launch(args);
    }
}

// ==================================================
// BOOK CLASS
// ==================================================

class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String author;
    private String category;
    private String issuedTo;
    private boolean issued;

    public Book(
            String id,
            String title,
            String author,
            String category
    ) {

        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.issuedTo = "";
        this.issued = false;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public String getIssuedTo() {
        return issuedTo;
    }

    public boolean isIssued() {
        return issued;
    }

    public void setIssued(boolean issued) {
        this.issued = issued;
    }

    public void setIssuedTo(String issuedTo) {
        this.issuedTo = issuedTo;
    }
}