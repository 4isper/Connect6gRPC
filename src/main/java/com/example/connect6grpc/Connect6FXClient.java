package com.example.connect6grpc;

import com.example.connect6.Connect6Grpc;
import com.example.connect6.Connect6OuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Connect6FXClient extends Application {
    private final int boardSize = 19;
    private final Circle[][] boardCircles = new Circle[boardSize][boardSize];
    private Connect6Grpc.Connect6BlockingStub gameStub;
    private int playerNumber = 0;
    private int moveCount = 0;
    private boolean firstMove = true;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Connect6 Game");

        Pane boardPane = new Pane();
        boardPane.setStyle("-fx-background-color: #d8a679;");

        for (int i = 0; i < boardSize; i++) {
            Line hLine = new Line(30, 30 + i * 30, 30 + (boardSize - 1) * 30, 30 + i * 30);
            boardPane.getChildren().add(hLine);

            Line vLine = new Line(30 + i * 30, 30, 30 + i * 30, 30 + (boardSize - 1) * 30);
            boardPane.getChildren().add(vLine);
        }

        int[] hosPoints = {3, 9, 15};
        for (int x : hosPoints) {
            for (int y : hosPoints) {
                Circle hosPoint = new Circle(30 + x * 30, 30 + y * 30, 3, Color.BLACK);
                boardPane.getChildren().add(hosPoint);
            }
        }
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Circle circle = new Circle(10, Color.TRANSPARENT);
                circle.setCenterX(30 + j * 30);
                circle.setCenterY(30 + i * 30);
                int x = i;
                int y = j;
                circle.setOnMouseClicked(e -> makeMove(x, y));
                boardCircles[i][j] = circle;
                boardPane.getChildren().add(circle);
            }
        }

        Scene scene = new Scene(boardPane, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        try {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                    .usePlaintext()
                    .build();
            gameStub = Connect6Grpc.newBlockingStub(channel);

            playerNumber = gameStub.register(Connect6OuterClass.Empty.newBuilder().build()).getPlayer();
            if (playerNumber == -1) {
                showError("Game already has two players.");
                System.exit(0);
            }
            showInfo("You are Player " + playerNumber);
            updateBoard();
            startAutoUpdate();
        } catch (Exception e) {
            showError("Cannot connect to server: " + e.getMessage());
        }
    }

    private void startAutoUpdate() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateBoard()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void makeMove(int x, int y) {
        try {
            int serverCurrentPlayer = gameStub.getCurrentPlayer(Connect6OuterClass.Empty.newBuilder().build()).getPlayer();

            if (serverCurrentPlayer != playerNumber) {
                showInfo("Not your turn.");
                return;
            }

            Connect6OuterClass.PlacePieceRequest request = Connect6OuterClass.PlacePieceRequest.newBuilder()
                    .setX(x)
                    .setY(y)
                    .setPlayer(playerNumber)
                    .build();

            if (firstMove && playerNumber == 1) {
                if (gameStub.placePiece(request).getSuccess()) {
                    updateBoard();
                    firstMove = false;
                    moveCount = 0;
                } else {
                    showInfo("Invalid move. Try again.");
                }
            } else {
                if (moveCount < 2 && gameStub.placePiece(request).getSuccess()) {
                    moveCount++;
                    updateBoard();

                    if (moveCount == 2) {
                        int winner = gameStub.checkWin(Connect6OuterClass.Empty.newBuilder().build()).getPlayer();
                        if (winner != 0) {
                            showInfo("Player " + winner + " wins!");
                            gameStub.resetBoard(Connect6OuterClass.Empty.newBuilder().build());
                            resetGame();
                        } else {
                            moveCount = 0;
                        }
                    }
                } else {
                    showInfo("Invalid move. Try again.");
                }
            }
        } catch (Exception e) {
            showError("Error during move: " + e.getMessage());
        }
    }

    private void updateBoard() {
        try {
            Connect6OuterClass.BoardResponse boardResponse = gameStub.getBoard(Connect6OuterClass.Empty.newBuilder().build());
            for (int i = 0; i < boardSize; i++) {
                for (int j = 0; j < boardSize; j++) {
                    int cellValue = boardResponse.getRows(i).getCells(j);
                    if (cellValue == 1) {
                        boardCircles[i][j].setFill(Color.BLACK);
                    } else if (cellValue == 2) {
                        boardCircles[i][j].setFill(Color.WHITE);
                    } else {
                        boardCircles[i][j].setFill(Color.TRANSPARENT);
                    }
                }
            }
        } catch (Exception e) {
            showError("Error updating board: " + e.getMessage());
        }
    }

    private void resetGame() {
        firstMove = true;
        moveCount = 0;
        updateBoard();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
