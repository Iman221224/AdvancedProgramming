import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class GameController implements Initializable {
    @FXML
    Text timer;
    @FXML
    TextField name;
    @FXML
    TextField family;
    @FXML
    TextField city;
    @FXML
    TextField country;
    @FXML
    TextField object;
    @FXML
    TextField flower;
    @FXML
    TextField clothes;
    @FXML
    TextField food;
    @FXML
    TextField animal;
    @FXML
    TextField fruit;
    @FXML
    TextField car;
    @FXML
    Button startGame;
    @FXML
    Text start;
    @FXML
    TextField letter;
    @FXML
    Text turn;
    @FXML
    Button finish;
    @FXML
    Text totalScore;
    @FXML
    Button nextRound;
    @FXML
    Text result;
    @FXML
    ProgressIndicator progressIndicator;
    private int counter = 0;
    private static char letterChar;
    private static int countRound = 1;
    private boolean finisher = false;

    public static void setSubjects(TextField... textField) {
        for (int i = 0; i < textField.length; i++) {
            if (!MakeGameController.getSelected().contains(textField[i].getPromptText()))
                textField[i].setDisable(true);
        }
    }

    @FXML
    public void handleButtonAction() {
        if (MakeGameController.getMinute() == 0) {
            timer.setVisible(false);
            return;
        }
        Timer tm = new Timer(true);
        tm.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                counter += 1;
                int sec = counter % 60;
                int min = counter / 60;
                min %= 60;
                timer.setText(String.format("%02d:%02d", min, sec));
                progressIndicator.setProgress(((60.0 * min) + sec) / (MakeGameController.getMinute() * 60));
                if (min == MakeGameController.getMinute()) {
                    timer.setText("FINISH");
                    try {
                        tm.cancel();
                        tm.purge();
                        finisher = true;
                        finishReceived(name, family, clothes, car, city, country, flower, food, object, animal, fruit);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }
        }, 1000, 1000);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setFinish();
        setSubjects(name, family, clothes, car, city, country, flower, food, object, animal, fruit);
        if (Server.getTurn() == 2) {
            turn.setText("نوبت بازیکن2");
            start.setText("در انتظار تصمیم بازیکن دیگر...");
            startGame.setDisable(true);
            letter.setDisable(true);
            Runnable runnable = () -> {
                try {
                    letterChar = Server.getDis().readUTF().charAt(0);
                    start.setText(" حرف " + letterChar + " است ");
                    start.setFill(Color.BLUE);
                    handleButtonAction();
                    waitForFinish();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        } else {
            turn.setText("نوبت بازیکن1");
        }
    }

    public void goBack() throws Exception {
        Server.getServer().close();
        Main main = new Main();
        main.changeScene("makeGame.fxml");
        MakeGameController.getSelected().clear();
        MakeGameController.setCounter(0);
    }

    public void exit() {
        System.exit(0);
    }

    public void setStart() throws IOException {
        if (Server.getTurn() == 1) {
            if (letter.getText().equals("")) {
                start.setText("حرف را وارد نکرده اید");
                start.setFill(Color.RED);
            } else if (letter.getText().length() > 1) {
                start.setText("ورودی شما باید یک حرف باشد");
                start.setFill(Color.RED);
            } else {
                Server.getDos().writeUTF(String.valueOf(letter.getText().charAt(0)));
                letterChar = letter.getText().charAt(0);
                handleButtonAction();
                waitForFinish();
            }
        }
    }

    public void setFinish() {
        if (Server.isByTime())
            finish.setDisable(true);
        else {
            timer.setVisible(false);
            progressIndicator.setVisible(false);
        }
    }

    public static char getLetterChar() {
        return letterChar;
    }

    public void finisher() throws IOException {
        Server.getDos().writeUTF("finish");
        finisher = true;
    }

    public void finishReceived(TextField... textFields) throws IOException {
        if (!finisher)
            Server.getDos().writeUTF("finish");
        for (int i = 0; i < textFields.length; i++) {
            if (MakeGameController.getSelected().contains(textFields[i].getPromptText()))
                Server.judgment(textFields[i].getText(), textFields[i].getPromptText());
            textFields[i].setDisable(true);
        }
        totalScore.setText(Server.getPlayer1().getScore() + "");
        Server.getDos().writeInt(Server.getPlayer2().getScore());
        finisher = false;
    }

    public void waitForFinish() {
        if (Server.isByTime())
            return;
        Runnable runnable = () -> {
            String message = "null";
            while (!message.equals("finish")) {
                try {
                    if (Server.getDis().available() != 0) {
                        message = Server.getDis().readUTF();
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                finishReceived(name, family, clothes, car, city, country, flower, food, object, animal, fruit);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void setNextRound() throws Exception {
        if (countRound < MakeGameController.getRound()) {
            Server.setTurn();
            countRound++;
            Main main = new Main();
            main.changeScene("Game.fxml");
        } else {
            Server.getDos().writeInt(Server.getPlayer1().getScore());
            Server.getDos().writeInt(Server.getPlayer2().getScore());
            result.setText("Game finished!\nTotal scores\nplayer1: " + Server.getPlayer1().getScore() + "\nplayer2: " + Server.getPlayer2().getScore());
            result.setFill(Color.PURPLE);
            result.setVisible(true);
        }
    }
}
