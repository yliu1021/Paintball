package brains.SkynetHelperClasses.Instincts;

import brains.SkynetHelperClasses.GameInterfaces.GameState;
import brains.SkynetHelperClasses.GameInterfaces.Move;

/**
 * Instinct class used to defend friendlies by attempting
 to shoot bullets headed their way
 */
public class DefendFriendlyInstinct extends Instinct {

    private int weight;

    public DefendFriendlyInstinct() {
        this(1);
    }

    public DefendFriendlyInstinct(int weight) {
        this.weight = weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public double rateMove(Move m, GameState s) {
        return 0;
    }

}
