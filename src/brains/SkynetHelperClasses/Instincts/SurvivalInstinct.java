package brains.SkynetHelperClasses.Instincts;

import brains.SkynetHelperClasses.GameInterfaces.*;

/**
 * Basic survival influence. Avoids all bullets and favors
 * directions that move farther from points of conflict
 */
public class SurvivalInstinct extends Instinct {

    private int weight;
    
    public SurvivalInstinct() {
        this(1);
    }
    
    public SurvivalInstinct(int weight) {
        this.weight = weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    /**
     * Tries to survive using point based rating system
     * [5, 15]     avoiding area of conflict
     * [5, 15]     leaving friendly line of sight
     * [30, 40]    leaving enemy line of sight
     * [80, 100]   avoiding incoming bullet
     * [-15, -5]   entering area of conflict
     * [-15, -5]   moving into line of sight of friendly
     * [-30, -40]  entering enemy line of sight
     * [-100, -80] entering into incoming bullet
     * 0.25    amplifier for predictions
     * @param m the move to be rated
     * @param s the state of the board
     * @return
     */
    @Override
    public double rateMove(Move m, GameState s) {
        int score = 0;
        if (m.getDirection() == Direction.EAST && m.getMoveType() == Move.MoveType.MOVE) {
            return 100;
        }
        return score;
    }

}
