package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.floor;

public class Server {
    private static ServerSocket serverSocket;
    private Board board;
    private final List<Socket> clients;
    private int numberOfPlayers;
    private int firstPlayerScore;
    private int secondPlayerScore;
    private BufferedReader bufferedReaderPlayer1;
    private BufferedReader bufferedReaderPlayer2;
    private BufferedWriter bufferedWriterPlayer1;
    private BufferedWriter bufferedWriterPlayer2;

    public Server(ServerSocket server, int width, int height, int mines) {
        serverSocket = server;
        this.board = new Board(width, height, mines);
        this.clients = new CopyOnWriteArrayList<>();
        this.numberOfPlayers = 0;
        this.firstPlayerScore = 0;
        this.secondPlayerScore = 0;
    }

    public void start() {
        new Thread(this::clientManagementLoop).start();
        new Thread(this::gameLoop).start();
    }
    private void clientManagementLoop() {
        while (true) {
            String board_data = board.getWidth() + " " + board.getHeight();
            if (numberOfPlayers >= 2) {
                        waitTime();

                        try {
                            int i = 0;
                            try {
                                bufferedWriterPlayer1.write(" ");
                                bufferedWriterPlayer1.flush();
                            } catch (Exception e) {
                                clients.set(i, serverSocket.accept());
                                bufferedWriterPlayer1 = new BufferedWriter(new OutputStreamWriter(clients.get(i).getOutputStream()));
                                bufferedReaderPlayer1 = new BufferedReader(new InputStreamReader(clients.get(i).getInputStream()));
                                sendMessage(bufferedWriterPlayer1, "Welcome Player 1!");
                                sendMessage(bufferedWriterPlayer1, board_data);

                            }

                            try {
                                bufferedWriterPlayer2.write(" ");
                                bufferedWriterPlayer2.flush();
                            } catch (Exception e) {
                                i = 1;
                                clients.set(i, serverSocket.accept());
                                bufferedWriterPlayer2 = new BufferedWriter(new OutputStreamWriter(clients.get(i).getOutputStream()));
                                bufferedReaderPlayer2 = new BufferedReader(new InputStreamReader(clients.get(i).getInputStream()));
                                sendMessage(bufferedWriterPlayer2, "Welcome Player 2!");
                                sendMessage(bufferedWriterPlayer2, board_data);
                                sendMessage(bufferedWriterPlayer2, board.toString());
                                sendMessage(bufferedWriterPlayer2, "END");
                                sendMessage(bufferedWriterPlayer2, "Player 1 score: " + firstPlayerScore + " Player 2 score: " + secondPlayerScore);
                            }

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
            } else {
                try {
                    final Socket socket = serverSocket.accept();
                    System.out.println("A new player has joined the game");
                    if (numberOfPlayers == 0) {
                        bufferedWriterPlayer1 = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        bufferedReaderPlayer1 = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        sendMessage(bufferedWriterPlayer1, "Welcome Player 1!");
                        sendMessage(bufferedWriterPlayer1, board_data);

                    } else if (numberOfPlayers == 1) {
                        bufferedWriterPlayer2 = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        bufferedReaderPlayer2 = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        sendMessage(bufferedWriterPlayer2, "Welcome Player 2!");
                        sendMessage(bufferedWriterPlayer2, board_data);
                        sendBoard();
                        sendResult();
                    }
                    numberOfPlayers++;
                    clients.add(socket);
                } catch (IOException e) {

                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void gameLoop() {
        boolean gameEnded = false;
        while (!gameEnded) {
            waitTime();
            if (numberOfPlayers >= 2) {
                System.out.println("Both players joined");
                    try {
                        processPlayerMove(bufferedReaderPlayer1, bufferedWriterPlayer1, 1);
                        if (checkWin()) break;
                        processPlayerMove(bufferedReaderPlayer2, bufferedWriterPlayer2, 2);
                        if (checkWin()) break;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        }
        closeServerSocket();
    }

    private void processPlayerMove(BufferedReader reader, BufferedWriter currentPlayerWriter, int player) throws IOException {
        while (true) {
            String move = reader.readLine();
            if (move == null) {
                return;
            }

            String[] parts = move.split(" ");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);

            if (isValidMove(x, y)) {
                sendMessage(currentPlayerWriter, "Shoot");
                int val = board.revealField(x, y);
                if (val == 1) {
                    if (player == 1) {
                        firstPlayerScore++;
                    } else {
                        secondPlayerScore++;
                    }
                }
                sendBoard();
                if (!checkWin()) {
                    sendResult();
                }
                break;
            } else {
                sendMessage(currentPlayerWriter, "Invalid move. Try again.");
            }
        }
    }

    public boolean checkWin() throws IOException {
        if (firstPlayerScore == floor(board.getMines() / 2.0) + 1) {
            sendMessage(bufferedWriterPlayer1, "Player 1 wins");
            sendMessage(bufferedWriterPlayer2, "Player 1 wins");
            return true;
        } else if (secondPlayerScore == floor(board.getMines() / 2) + 1) {
            sendMessage(bufferedWriterPlayer1, "Player 2 wins");
            sendMessage(bufferedWriterPlayer2, "Player 2 wins");
            return true;
        } else if ((firstPlayerScore == board.getMines() / 2.0) && (secondPlayerScore == board.getMines() / 2)) {
            sendMessage(bufferedWriterPlayer1, "Draw");
            sendMessage(bufferedWriterPlayer2, "Draw");
            return true;
        } else {
            return false;
        }
    }

    private void sendMessage(BufferedWriter writer, String s) throws IOException {
        writer.write(s);
        writer.newLine();
        writer.flush();
    }

    private boolean isValidMove(int x, int y) {
        return x >= 0 && x < board.getWidth() && y >= 0 && y < board.getHeight() && !board.checkFieldVisibility(x, y);
    }

    public void sendResult() throws IOException {
        sendMessage(bufferedWriterPlayer1, "Player 1 score: " + firstPlayerScore + " Player 2 score: " + secondPlayerScore);
        sendMessage(bufferedWriterPlayer2, "Player 1 score: " + firstPlayerScore + " Player 2 score: " + secondPlayerScore);
    }

    public void sendBoard() throws IOException {
        sendMessage(bufferedWriterPlayer1, board.toString());
        sendMessage(bufferedWriterPlayer1, "END");
        sendMessage(bufferedWriterPlayer2, board.toString());
        sendMessage(bufferedWriterPlayer2, "END");
    }

    private void closeServerSocket() {
        try {
            if (bufferedWriterPlayer1 != null)
                bufferedWriterPlayer1.close();

            if (bufferedReaderPlayer1 != null)
                bufferedReaderPlayer1.close();

            if (bufferedWriterPlayer2 != null)
                bufferedWriterPlayer2.close();

            if (bufferedReaderPlayer2 != null)
                bufferedReaderPlayer2.close();

            for (Socket client : clients) {
                if (client != null)
                    client.close();
            }

            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void waitTime() {
        try {
            TimeUnit.SECONDS.sleep(1);
        }catch (Exception e) {

        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket, 5, 8, 8);
        server.start();
    }
}
