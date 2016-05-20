/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package arena;

/**
 *
 * @author tweis0306
 */
public class Board {
    private int[] score = {0, 0, 0};
    private Occupant[][] grid;

    Board(int numRows, int numCols) {
        grid = new Occupant[numRows][numCols];
    }
    
    public int numRows() {
        return grid.length;
    }
    
    public int numCols() {
        return grid[0].length;
    }
    
    public boolean isValid(int row, int col) {
        return row >= 0 && row < numRows()
                && col >= 0 && col < numCols();
    }
    
    public boolean isEmpty(int row, int col) {
        return isValid(row, col) && grid[row][col] == null;
    }
    
    boolean add(Occupant occ, int row, int col) {
        if (!isEmpty(row, col)) return false;
        grid[row][col] = occ;
        return true;
    }
    
    public Occupant get(int row, int col) {
        if (!isValid(row, col))
            throw new ArrayIndexOutOfBoundsException
                    ("Board row = " + row + ", col = " + col);
        return grid[row][col];
    }
    
    Occupant move(int fromRow, int fromCol,
            int toRow, int toCol) {
        if (!isValid(fromRow, fromCol))
            throw new IllegalArgumentException(
            "Illegal grid location: " + fromRow + ", " + fromCol);
        if (!isValid(toRow, toCol))
            throw new IllegalArgumentException(
            "Illegal grid location: " + toRow + ", " + toCol);
        Occupant temp = grid[toRow][toCol];
        grid[toRow][toCol] = grid[fromRow][fromCol];
        grid[fromRow][fromCol] = null;
        return temp;
    }
    
    Occupant remove(int row, int col) {
        if (!isValid(row, col))
            throw new IllegalArgumentException(
            "Illegal grid location: " + row + ", " + col);
        Occupant occ = grid[row][col];
        grid[row][col] = null;
        return occ;
    }
    
    void scorePoints(int team, int points) {
        score[team] += points;
    }
    
    void losePoints(int team, int points) {
        score[team] -= points;
    }
    
    void resetScores() {
        score[1] = 0;
        score[2] = 0;
    }
    
    int getScore(int team) {
        return score[team];
    }
           
}
