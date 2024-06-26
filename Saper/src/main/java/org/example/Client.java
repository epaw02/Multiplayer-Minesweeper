package org.example;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class Client {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    Scanner scanner = new Scanner(System.in);

    private Boolean myTurn;
    private Integer width;
    private Integer height;
    private char[][] boardArray;
    private String score = "";
    private String name = "";
    private Integer xDimension;
    private Integer yDimension;
    private boolean sendDimensions = false;

    private int playerNum;
    private JFrame mainFrame = new JFrame("Main frame");
    private JButton[][] boardGUI;
    private JPanel mainPanel = new JPanel();
    private JPanel boardPanel = new JPanel();
    private JPanel scorePanel = new JPanel();
    private JPanel namePanel = new JPanel();

    public Client(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            close();
            throw new RuntimeException(e);
        }
    }

    public void makeMove(int x, int y) {
        lock.lock();
        try {
            bufferedWriter.write(x + " " + y);
            System.out.println("shooting at" + x + " " + y);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            String line = bufferedReader.readLine();
            if (line.contains("Shoot")) {
                sendDimensions = true;
                condition.signalAll();
            } else {
                System.out.println(line);
            }
        } catch (IOException e) {
            close();
            throw new RuntimeException(e);
        }
        finally {
            lock.unlock();
        }
    }

    public void listenForMessage() {
        try {
            String line =bufferedReader.readLine();
            System.out.println(line);
            if (line.contains("1")){
                name = "You are Player 1";
                myTurn = true;
                playerNum = 1;
            } else  {
                name = "You are Player 2";
                myTurn = false;
                playerNum =2;
            }
        } catch (IOException e) {
            close();
            throw new RuntimeException(e);
        }
    }

    public void listenForResults(){
        try {
            String line =bufferedReader.readLine();
            System.out.println(line);
            score = line;
            if (line.contains("win") || line.contains("Draw")) {
                System.out.println("Game finished");
                JOptionPane.showMessageDialog(null, line, "Game Result", JOptionPane.INFORMATION_MESSAGE);
                close();
                System.exit(0);
            }
        } catch (IOException e) {
            close();
            throw new RuntimeException(e);
        }
    }

    public void listenForMove() {
        try {
            String line;
            StringBuilder boardState = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("END")) {
                    break;
                }
                boardState.append(line).append("\n");
            }
            System.out.println("Board state:");
            System.out.println(boardState.toString());


            String[] lines = boardState.toString().split("\n");
            boardArray = new char[height][width];
            for (int i = 0; i < height; i++) {
                boardArray[i] = lines[i+1].replace(" ", "").toCharArray();
            }
        } catch (IOException e) {
            close();
            throw new RuntimeException(e);
        }
    }

    public void listenForBoard(){
        try {
            String line =bufferedReader.readLine();
            if(line.isEmpty())
                return;
            String[] dimensions = line.split(" ");
            width = Integer.parseInt(dimensions[0]);
            height = Integer.parseInt(dimensions[1]);
            System.out.println("Board width and height: " + width + " " + height);
        } catch (IOException e) {
            close();
            throw new RuntimeException(e);
        }
    }

    public void handleGUI(){
        mainFrame.getContentPane().removeAll();
        mainPanel.revalidate();
        mainPanel.repaint();
        mainFrame.setSize(600, 600);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainPanel = new JPanel(new BorderLayout());
        boardPanel = new JPanel(new GridLayout(height, width));
        scorePanel = new JPanel(new FlowLayout());
        namePanel = new JPanel(new FlowLayout());
        boardGUI = new JButton[height][width];
        for (int i = 0; i<height; i++){
            for (int j = 0; j<width; j++){
                boardGUI[i][j] = new JButton(String.valueOf(boardArray[i][j]));
                int tempY = i;
                int tempX = j;
                boardGUI[i][j].addActionListener(e -> {
                    onButtonClick(tempY, tempX);
                });
                boardPanel.add(boardGUI[i][j]);
            }
        }

        JLabel nameLabel = new JLabel(name);
        namePanel.add(nameLabel, BorderLayout.CENTER);

        JLabel scoreLabel = new JLabel(score);
        scorePanel.add(scoreLabel, BorderLayout.CENTER);

        mainPanel.add(boardPanel, BorderLayout.CENTER);
        mainPanel.add(scorePanel, BorderLayout.SOUTH);
        mainFrame.add(mainPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
        mainFrame.setVisible(true);
        mainFrame.addWindowListener(new WindowAdapter() {
                                        @Override
                                        public void windowClosing(WindowEvent e) {
                                            System.out.println("CLOSING CONNECTION!");
                                            close();
                                            System.out.println("Connection Closed");
                                            System.exit(0);
                                        }
                                    }

        );
    }

    public void handleButtons(boolean available){
        for (int i = 0; i<height; i++){
            for (int j = 0; j<width; j++){
                if (boardArray[i][j] == '*') {
                    boardGUI[i][j].setEnabled(available);
                }
                else {
                    boardGUI[i][j].setEnabled(false);
                }
            }
        }
    }

    private void onButtonClick(int i, int j) {
        System.out.println("Button clicked at: (" + i + ", " + j + ")");
        yDimension = i;
        xDimension = j;
        makeMove(xDimension, yDimension);
    }

    public void start() {
        listenForMessage();
        listenForBoard();
        while (socket.isConnected()) {
            listenForMove();
            listenForResults();
            handleGUI();
            handleButtons(false);
            if(myTurn){
                handleButtons(true);
                lock.lock();
                try {
                    while (!sendDimensions) {
                        try {
                            condition.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                System.out.println("Wait for the opponent");
            }
            myTurn = !myTurn;
            sendDimensions = false;
        }
    }

    public void close() {
        try {
            if (socket != null)
                socket.close();

            if (bufferedReader != null)
                bufferedReader.close();

            if (bufferedWriter != null)
                bufferedWriter.close();

            if (scanner != null)
                scanner.close();
            mainFrame.dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 1234);
            Client client = new Client(socket);
            client.start();
        } catch (IOException e) {
            System.err.println("Failed to start client: " + e.getMessage());
        }
    }
}
