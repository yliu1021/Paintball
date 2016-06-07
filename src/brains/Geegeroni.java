/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package brains;

import arena.Action;
import arena.Board;
import arena.Brain;
import arena.Direction;
import arena.Player;
import java.awt.Color;

/**
 *
 * @author Ben the Jewgoy
 */
public class Geegeroni implements Brain {
    int count = 0;
    boolean inposition = false;
    int myTeam = -1;
    int baseRow = 16;
    int baseCol = -1;
    int defenseDir;
    int away;
    int searchCol;
    int SearchRow;

    @Override
    public String getName() {
        return "Geegeroni";
    }

    @Override
    public String getCoder() {
        return "Ben Cohen aka Jewglypuff aka PikaJew aka the Pokejew Trainer";
    }

    @Override
    public Color getColor() {
        return new Color(255,102,0);
    }

    @Override
    public Action getMove(Player p, Board b) {

        if (myTeam == -1) {
            myTeam = p.getTeam();
            if (myTeam == 2)
                baseCol = 49;
                defenseDir = Direction.NORTHWEST;
                away = Direction.WEST;
                searchCol = -5;
            if (myTeam == 1)
                baseCol = 0;
                defenseDir = Direction.NORTHEAST;
                away = Direction.EAST;
                searchCol = 5;
        }
        
        //       Delcare yourself in position then start shooting diagonally        
        if (baseIsEmpty(b,p)) {
            if (inposition) {
                return new Action("S");
            }
            if (p.getCol() == baseCol && p.getRow() == baseRow - 2) {
                inposition = true;
                return new Action("T", defenseDir);
            }
            if (!(p.getCol() == baseCol && p.getRow() == baseRow - 2 && p.getDirection() == defenseDir)) {
                inposition = false;
            }
        }
        
//        When not in position, move into position
        if(!inposition && baseIsEmpty(b,p)){
            int dirToPos = Direction.getDirectionTowards(p.getRow(), p.getCol(),baseRow - 2, baseCol); 
                int[] nextSpace = Direction.getLocInDirection(p.getRow(),p.getCol(), dirToPos);
                if (b.isEmpty(nextSpace[0], nextSpace[1]))
                    return new Action("M", dirToPos);
                else
                    return new Action("M", 45* ((int) (Math.random() * 8)) );
        }
        
//        KILL THE TRESSPASSER
        if (!baseIsEmpty(b,p)) {
            inposition = false;
            int dirToBase = Direction.getDirectionTowards(p.getRow(), p.getCol(),baseRow , baseCol); 
            for (int i = baseRow - 3; i < baseRow + 3; i++) {
                for (int j = baseCol; j < baseCol + searchCol; j++){
                    if (b.get(i,j) instanceof Player && b.get(i,j).getTeam() != p.getTeam()) {
                        Player enemy = (Player)(b.get(i,j));
                        int dirToEnemy = Direction.getDirectionTowards
                              (p.getRow(), p.getCol(),enemy.getRow(), enemy.getCol()); 
                        int distToEnemy = Direction.moveDistance
                              (p.getRow(), p.getCol(),enemy.getRow(), enemy.getCol());
                        int[] nextSpace = Direction.getLocInDirection(p.getRow(),p.getCol(), dirToEnemy);
                        if(killSpot(b,p) && p.getCol() == baseCol){
                            return new Action("M", away);
                        }
                        if (distToEnemy == 2) {
                            if (dirToEnemy != p.getDirection())
                                return new Action("T", dirToEnemy);
                            else if (p.getDirection() == dirToBase) {
                                return new Action("M" , Direction.getDirectionTowards
                                    (p.getRow(), p.getCol(), p.getRow() , baseCol));
                            } else {
                                return new Action("S");
                            }
                        } else if (distToEnemy == 1) {
                            if (dirToEnemy != p.getDirection())
                                return new Action("T", dirToEnemy);
                            else
                                return new Action("M", dirToEnemy + 180);
                        }
                        
//                        if (b.isEmpty(nextSpace[0], nextSpace[1]))
//                            return new Action("M", dirToEnemy);     
                    }     
                }
            } 
        }
        return new Action("S");
    }


    public boolean baseIsEmpty(Board b, Player p){
       for (int i = baseRow - 3; i < baseRow + 3; i++) {
            for (int j = baseCol; j < baseCol + searchCol; j++) {
                if (b.get(i,j) instanceof Player && b.get(i,j).getTeam() != p.getTeam()) {
                   return false;
                    
                }
            }
  
        }
        return true;
    }
    
    public boolean killSpot(Board b, Player p){
        if (myTeam == 1) {
            if((b.get(baseRow, baseCol + 1) instanceof Player && b.get(baseRow, baseCol + 1).getTeam() != p.getTeam())
                    || (b.get(baseRow + 1, baseCol + 1) instanceof Player && b.get(baseRow + 1, baseCol + 1).getTeam() != p.getTeam())){
            return true;
            }   
        }
        if (myTeam == 2) {
            if((b.get(baseRow, baseCol - 1) instanceof Player && b.get(baseRow, baseCol - 1).getTeam() != p.getTeam())
                    || (b.get(baseRow + 1, baseCol - 1) instanceof Player && b.get(baseRow + 1, baseCol - 1).getTeam() != p.getTeam())){
            return true;
            }   
        }
        return false;
    }
    
//    public int defDir(Board b,Player p){
//        Player enemy;
//        if(myTeam == 1){
//            for (int col = 49; col > 0; col--) {
//                for (int row = 0; row < 32; row++) {
//                    if (b.get(row,col) instanceof Player && b.get(row,col).getTeam() != p.getTeam())
//                        enemy = (Player) b.get(row,col);
//                }
//            }
//        }
//        if(myTeam == 2){
//            for (int col = 0; col < 49; col++) {
//                for (int row = 0; row < 32; row++) {
//                    if (b.get(row,col) instanceof Player && b.get(row,col).getTeam() != p.getTeam())
//                        enemy = (Player) b.get(row,col);
//                }
//            }
//        }                
//        return Direction.getDirectionTowards(p.getRow(), p.getCol(), enemy.getRow(), enemy.getCol());
//    }
}


