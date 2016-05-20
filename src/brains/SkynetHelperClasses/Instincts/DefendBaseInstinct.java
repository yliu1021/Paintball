package brains.SkynetHelperClasses.Instincts;

import brains.SkynetHelperClasses.GameInterfaces.GameState;
import brains.SkynetHelperClasses.GameInterfaces.Move;

/**
 * Instinct class used to defend the base by
 shooting down incoming shots or blocking if
 necessary
 */
public class DefendBaseInstinct extends Instinct {

    private int weight;

    public DefendBaseInstinct() {
        this(1);
    }
    
    public DefendBaseInstinct(int weight) {
        this.weight = weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public double rateMove(Move m, GameState s) {
        return 0;
    }

}
