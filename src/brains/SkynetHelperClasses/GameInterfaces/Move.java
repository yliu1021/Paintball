package brains.SkynetHelperClasses.GameInterfaces;

import arena.Action;

import java.util.ArrayList;

/**
 * Created by Yuhan on 5/14/16.
 */
public class Move {

    /**
     * Returns all possible moves given a state
     * @param s state of the board
     * @return
     * An array of valid moves
     */
    public static Object[] validMoves(GameState s) {
        ArrayList<Move> moves = new ArrayList<>(18);

        // add pass move
        moves.add(new Move(MoveType.PASS, null));

        // add shoot move if posible
        if (s.canShoot())
            moves.add(new Move(MoveType.SHOOT, null));

        // add all turn moves
        for (Direction d : Direction.values())
            moves.add(new Move(MoveType.TURN, d));

        // add possible move directions
        for (Direction d : Direction.values()) {
            if (s.canMove(d))
                moves.add(new Move(MoveType.MOVE, d));
        }

        return moves.toArray();
    }

    public enum MoveType {
        TURN,
        MOVE,
        SHOOT,
        PASS;
        
        @Override
        public String toString() {
            String m = "";
            switch (this) {
                case TURN:
                    m = "Turn";
                    break;
                case MOVE:
                    m = "Move";
                    break;
                case SHOOT:
                    m = "Shoot";
                    break;
                case PASS:
                    m = "Pass";
                    break;
            }
            return m;
        }
    }
    
    private final MoveType moveType;
    private final Direction direction;

    public Move(MoveType moveType, Direction direction) {
        this.moveType = moveType;
        this.direction = direction;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public Direction getDirection() {
        return direction;
    }
    
    // interfacing with Direction and Action.
    public Action getAction() {
        return new Action(moveType.toString(), direction.toDegrees());
    }
    
    @Override
    public String toString() {
        return "Move: " + moveType.toString() + " in direction: " + direction.toString();
    }
    
}