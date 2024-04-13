package org.example.parseremails;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javafx.concurrent.Task;
import javafx.application.Platform;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller implements Initializable {
    public static String query;
    public static int pages;
    public static int page;
    public static HashSet<String> websites = new HashSet<>();
    public static HashSet<String> emails = new HashSet<>();
    @FXML
    public AnchorPane anchorPane;
    @FXML
    public TextField requestField;
    @FXML
    public TextField numberField;
    @FXML
    public Button findButton;
    @FXML
    public ProgressBar progressBar;
    @FXML
    public Label statusInfo;
    @FXML
    public Label finish;
    @FXML
    public Label emailsFound;
    @FXML
    public Label checkSaves;
    @FXML
    public Button parseAgain;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        progressBar.setVisible(false);
        statusInfo.setVisible(false);
        findButton.setDisable(true);
        finish.setVisible(false);
        emailsFound.setVisible(false);
        checkSaves.setVisible(false);
        parseAgain.setVisible(false);

        requestField.setOnKeyReleased(event -> findButton.setDisable(requestField.getText().isEmpty() || numberField.getText().isEmpty()));
        numberField.setOnKeyReleased(event -> findButton.setDisable(requestField.getText().isEmpty() || numberField.getText().isEmpty()));

        parseAgain.setOnAction(event -> {
            finish.setVisible(false);
            emailsFound.setVisible(false);
            checkSaves.setVisible(false);
            parseAgain.setVisible(false);
            requestField.setText("");
            numberField.setText("");
            requestField.setDisable(false);
            numberField.setDisable(false);
            findButton.setVisible(true);
            findButton.setDisable(true);
        });
        findButton.setOnAction(event -> {
            findButton.setVisible(false);
            requestField.setDisable(true);
            numberField.setDisable(true);
            statusInfo.setVisible(true);
            progressBar.setVisible(true);

            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    query = requestField.getText() + " emails";
                    pages = Integer.parseInt(numberField.getText());
                    findPages(query, pages);
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                statusInfo.setVisible(false);
                progressBar.setVisible(false);
                finish.setVisible(true);
                emailsFound.setVisible(true);
                parseAgain.setVisible(true);

                if (!emails.isEmpty()) {
                    emailsFound.setText(emails.size() + " emails were found");
                    checkSaves.setVisible(true);
                } else {
                    emailsFound.setText("No emails found");
                    checkSaves.setVisible(false);
                }
            });

            task.setOnFailed(e -> {
                Throwable exception = task.getException();
                findButton.setVisible(true);
                statusInfo.setVisible(false);
                progressBar.setVisible(false);
                requestField.setDisable(false);
                numberField.setDisable(false);
                if (exception instanceof NumberFormatException) {
                    alert("Wrong input. This field must contain only the numbers");
                } else {
                    alert(exception.getMessage());
                }
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });
    }
    public void findPages(String query, int pages) {
        try {
            page = 0;
            setProgressBarAnimation();
            while (page < pages) {
                Document doc = Jsoup.connect("https://www.google.com/search?q=" + query + "&start=" + (pages * 10)).get();
                Elements links = doc.select("a[href]");
                for (Element link: links) {
                    String url = link.attr("abs:href");
                    if (url.startsWith("http")) {
                        websites.add(url);
                    }
                }
                page++;
            }
        } catch (UnknownHostException e) {
            alert("The Internet connection is lost");
        } catch (IOException e) {
            alert(e.getMessage());
        }

        for (String website: websites) {
            parseEmails(website);
        }

        try {
            saveEmails(emails);
        } catch (IOException e) {
            alert(e.getMessage());
        }
    }
    public void parseEmails(String website) {
        try {
            Document doc = Jsoup.connect(website).get();
            String text = doc.text();
            Pattern pattern = Pattern.compile("[\\w!#$%&'+=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@([a-zA-Z0-9-]+\\.)[a-zA-Z]+((\\.)([a-zA-Z]){2})*");
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                emails.add(matcher.group());
            }
        } catch (UnknownHostException e) {
            alert("The internet connection is lost");
        } catch (IOException ignored) {}
    }
    public void saveEmails(HashSet<String> emails) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
        Date currentDate = new Date();
        String formattedDate = dateFormat.format(currentDate);
        String saveName = "saves/save" + formattedDate + ".txt";

        if (!emails.isEmpty()) {
            BufferedWriter bw;
            Path saveFile = Paths.get(saveName);
            bw = Files.newBufferedWriter(saveFile, Charset.defaultCharset());
            bw.write("......EMAIL LIST......");
            bw.newLine();

            for (String email: emails) {
                bw.write(email);
                bw.newLine();
            }

            bw.close();
        }
    }
    public void alert(String errorMessage) {
        Platform.runLater(() -> {
            Alert dialogWindow = new Alert(Alert.AlertType.ERROR);
            dialogWindow.setTitle("Error");
            dialogWindow.setHeaderText(null);
            dialogWindow.setContentText(errorMessage);
            dialogWindow.showAndWait();
        });
    }
    public void setProgressBarAnimation() {
        progressBar.setPrefWidth(200);
        progressBar.setProgress(0);
        KeyValue kv1 = new KeyValue(progressBar.progressProperty(), 1);
        KeyFrame kf1 = new KeyFrame(Duration.seconds(2), kv1);


        KeyValue kv2 = new KeyValue(progressBar.progressProperty(), 0);
        KeyFrame kf2 = new KeyFrame(Duration.seconds(2), kv2);

        Timeline timeline = new Timeline(kf1, kf2);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}