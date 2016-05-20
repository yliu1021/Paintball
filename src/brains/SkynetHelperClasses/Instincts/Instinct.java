package brains.SkynetHelperClasses.Instincts;

import brains.SkynetHelperClasses.GameInterfaces.GameState;
import brains.SkynetHelperClasses.GameInterfaces.Move;

/**
 * An interface that affects the move that the Behavior class
 * chooses. Each influence should rate each move for the Behavior
 * class to choose from.
 */
public abstract class Instinct {

    /**
     * @return
     * returns the importance of an influence
     * can be used as a bias factor
     */
    public int getWeight() {
        return 1;
    }

    /**
     * Rates a move heuristically from on a
     * scale from -100 to 100.
     * @param m the move to be rated
     * @param s the state of the board
     * @return
     * values < 0 represent moves to be avoided,
     * values > 0 represent moves to go for,
     * values = 0 are neutral moves and have no effect
     * on the current influence
     */
    public abstract double rateMove(Move m, GameState s);

}
