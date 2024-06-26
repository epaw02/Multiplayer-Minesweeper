package org.example;

import lombok.*;
import java.util.*;

@Getter
@Setter
public class Board {
    private List<Field> gameBoard = new ArrayList<>();
    int width;
    int height;
    int mines;

    public Board(int width, int height, int mines) {
        this.width = width;
        this.height = height;
        this.mines = mines;

        for(int i = 0; i < (width * height) - mines; i++)
            gameBoard.add(Field.builder().value(0).visibility(Visibility.HIDDEN).status(Status.FIELD).build());
        for(int i = 0; i < mines; i++)
            gameBoard.add(Field.builder().value(-1).visibility(Visibility.HIDDEN).status(Status.BOMB).build());
        Collections.shuffle(gameBoard);
        evaluateBoard();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for(int i = 0; i < width*height; i++) {
            if(i%width == 0)
                out.append('\n');
            out.append(gameBoard.get(i).toString() + " ");
        }
        return out.toString();
    }

    public void evaluateBoard() {
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                int coordinate = getCoordinate(x,y);
                assert(coordinate != -1);
                Field newField = gameBoard.get(coordinate);
                newField.evalValue(getNeighbours(x,y));
                gameBoard.set(coordinate,newField);
            }
        }
    }

    public List<Field> getNeighbours(int x, int y) {
        List<Field> neighbours = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            for(int e = -1; e <= 1; e++) {
                int coordinate = getCoordinate(x+i,y+e);
                if(coordinate != -1)
                    neighbours.add(gameBoard.get(coordinate));
            }
        }
        return neighbours;
    }

    public boolean checkFieldVisibility(int x, int y){
        int coordinate = getCoordinate(x, y);
        Field field = gameBoard.get(coordinate);
        return field.isVisible();
    }

    public int getCoordinate(int x, int y) {
        if(x >= width || y >= height || x < 0  || y < 0)
            return -1;
        return y * width + x;
    }

    public int revealField(int x, int y) {
        int coordinate = getCoordinate(x,y);
        Field newField = gameBoard.get(coordinate);
        if (newField.getValue() == 0)
            revealZeros(x,y);
        else
            newField.setVisibility(Visibility.VISIBLE);
        gameBoard.set(coordinate,newField);
        return newField.status == Status.BOMB ? 1 : 0;
    }

    private void revealZeros(int x, int y) {
        int coordinate = getCoordinate(x,y);
        if (coordinate == -1) return;
        Field newField = gameBoard.get(coordinate);
        if (newField.value != 0 || newField.isVisible()) return;
        newField.setVisibility(Visibility.VISIBLE);
        gameBoard.set(coordinate,newField);
        for (int i = -1; i <= 1; i++) {
            for(int e = -1; e <= 1; e++) {
                int newCoordinate = getCoordinate(x+i,y+e);
                if(newCoordinate != -1)
                    revealZeros(x+i,y+e);
            }
        }
    }
}
