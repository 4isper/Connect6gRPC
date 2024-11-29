package com.example.connect6grpc;

import com.example.connect6.Connect6Grpc;
import com.example.connect6.Connect6OuterClass;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Connect6GrpcServer {
    private final int[][] board = new int[19][19];
    private final int boardSize = 19;
    private int currentPlayer = 1;
    private int moveCount = 0;
    private final AtomicInteger playerCount = new AtomicInteger(0);
    private boolean firstMove = true;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(9090)
                .addService(new Connect6ServiceImpl())
                .build();

        System.out.println("Connect6 Server is starting...");
        server.start();
        System.out.println("Connect6 Server is running on port 9090.");
        server.awaitTermination();
    }

    static class Connect6ServiceImpl extends Connect6Grpc.Connect6ImplBase {
        private final Connect6GrpcServer game = new Connect6GrpcServer();

        @Override
        public void register(Connect6OuterClass.Empty request, StreamObserver<Connect6OuterClass.PlayerResponse> responseObserver) {
            int assignedPlayer = game.register();
            Connect6OuterClass.PlayerResponse response = Connect6OuterClass.PlayerResponse.newBuilder()
                    .setPlayer(assignedPlayer)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void placePiece(Connect6OuterClass.PlacePieceRequest request, StreamObserver<Connect6OuterClass.ActionResponse> responseObserver) {
            boolean success = game.placePiece(request.getX(), request.getY(), request.getPlayer());
            Connect6OuterClass.ActionResponse response = Connect6OuterClass.ActionResponse.newBuilder()
                    .setSuccess(success)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void getCurrentPlayer(Connect6OuterClass.Empty request, StreamObserver<Connect6OuterClass.PlayerResponse> responseObserver) {
            int currentPlayer = game.getCurrentPlayer();
            Connect6OuterClass.PlayerResponse response = Connect6OuterClass.PlayerResponse.newBuilder()
                    .setPlayer(currentPlayer)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void getBoard(Connect6OuterClass.Empty request, StreamObserver<Connect6OuterClass.BoardResponse> responseObserver) {
            Connect6OuterClass.BoardResponse.Builder boardResponseBuilder = Connect6OuterClass.BoardResponse.newBuilder();
            for (int[] row : game.getBoard()) {
                Connect6OuterClass.Row.Builder rowBuilder = Connect6OuterClass.Row.newBuilder();
                for (int cell : row) {
                    rowBuilder.addCells(cell);
                }
                boardResponseBuilder.addRows(rowBuilder);
            }
            responseObserver.onNext(boardResponseBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void checkWin(Connect6OuterClass.Empty request, StreamObserver<Connect6OuterClass.PlayerResponse> responseObserver) {
            int winner = game.checkWin();
            Connect6OuterClass.PlayerResponse response = Connect6OuterClass.PlayerResponse.newBuilder()
                    .setPlayer(winner)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void resetBoard(Connect6OuterClass.Empty request, StreamObserver<Connect6OuterClass.Empty> responseObserver) {
            game.resetBoard();
            responseObserver.onNext(Connect6OuterClass.Empty.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    private synchronized int register() {
        if (playerCount.get() >= 2) {
            return -1;
        }
        int assignedPlayer = playerCount.incrementAndGet();
        System.out.println("Player " + assignedPlayer + " has joined the game.");
        return assignedPlayer;
    }

    private synchronized boolean placePiece(int x, int y, int player) {
        if (player != currentPlayer) {
            System.out.println("Player " + player + " attempted to move out of turn.");
            return false;
        }

        if (x >= 0 && x < boardSize && y >= 0 && y < boardSize && board[x][y] == 0) {
            board[x][y] = player;
            moveCount++;

            if (moveCount == 1 && firstMove) {
                currentPlayer = 2;
                moveCount = 0;
                firstMove = false;
            } else if (moveCount == 2) {
                currentPlayer = (currentPlayer == 1) ? 2 : 1;
                moveCount = 0;
            }
            System.out.println("Player " + player + " placed a piece at (" + x + ", " + y + ").");
            return true;
        }
        System.out.println("Player " + player + " attempted to place a piece at invalid position (" + x + ", " + y + ").");
        return false;
    }

    private synchronized int getCurrentPlayer() {
        return currentPlayer;
    }

    private int[][] getBoard() {
        return board;
    }

    private synchronized int checkWin() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                int player = board[i][j];
                if (player != 0 && (checkDirection(i, j, player, 1, 0) ||
                        checkDirection(i, j, player, 0, 1) ||
                        checkDirection(i, j, player, 1, 1) ||
                        checkDirection(i, j, player, 1, -1))) {
                    return player;
                }
            }
        }
        return 0;
    }

    private boolean checkDirection(int x, int y, int player, int dx, int dy) {
        int count = 0;
        for (int i = 0; i < 6; i++) {
            int nx = x + i * dx;
            int ny = y + i * dy;
            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize && board[nx][ny] == player) {
                count++;
            } else {
                break;
            }
        }
        return count == 6;
    }

    private synchronized void resetBoard() {
        for (int[] row : board) {
            Arrays.fill(row, 0);
        }
        currentPlayer = 1;
        moveCount = 0;
        firstMove = true;
        System.out.println("Board has been reset.");
    }
}


