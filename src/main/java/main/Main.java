package main;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.io.RandomAccessFile;
import javafx.application.Application;

public class Main extends Application {
    static Webpage[] getRandomPages(int size) throws IOException {
        String randomWiki = "http://en.wikipedia.org/wiki/Special:Random";
        Webpage[] pages = new Webpage[size];
        for (int i = 0; i < pages.length; i++) {
            Document d = Jsoup.connect(randomWiki).get();
            String link = d.location();
            String title = d.title();
            RandomAccessFile file = new RandomAccessFile("/Users/jamielafarr/Java/365/projects/persistent/src/main/files/" + title + ".txt", "rw");
            BTree tree = new BTree(file);
            pages[i] = new Webpage(file, tree, link);
        }
        return pages;
    }
    Stage curr;
    Scene s1, s2;
    public static void main(String[] args) throws IOException {
        Webpage[] pages = getRandomPages(100);
        for (Webpage w : pages) {
            System.out.println("Title: " + w.title);
            System.out.println("Link: " + w.link);
            w.addAllWordsToTree();
        }
//        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Webpage[] pages = getRandomPages(100);
        stage.setTitle("TF-IDF");
        curr = stage;
        Label link = new Label("Link");
        TextField f = new TextField();
        Button b = new Button("Get similar page");
        VBox layout1 = new VBox(40);
        layout1.getChildren().addAll(link, f, b);
        s1 = new Scene(layout1, 200, 200);
        b.setOnAction(e -> {
            String l = f.getText();
            try {
                RandomAccessFile inputFile = new RandomAccessFile("/Users/jamielafarr/Java/365/projects/persistent/src/main/files/" + l + ".txt", "rw");
                BTree t = new BTree(inputFile);
                Webpage userInput = new Webpage(inputFile, t, l); // need file, tree, link
                Webpage w = userInput.getBestMatch(pages);
                Hyperlink hp = new Hyperlink();
                hp.setText(w.link);
                hp.setContentDisplay(ContentDisplay.BOTTOM);
                Label after  = new Label("Try this page: ");
                after.setContentDisplay(ContentDisplay.TOP);
                VBox v2 = new VBox();
                v2.getChildren().addAll(after, hp);
                s2 = new Scene(v2, 200, 200);
                curr.setScene(s2);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        });
        stage.setScene(s1);
        stage.show();
    }
}
