package brains.SkynetHelperClasses.Instincts;

import brains.SkynetHelperClasses.GameInterfaces.GameState;
import brains.SkynetHelperClasses.GameInterfaces.Move;

/**
 * Instinct class used to target enemy base.
 * Tries to get into line of fire and then shoots
 * if possible
 */
public class ShootBaseInstinct extends Instinct {

    private int weight;

    public ShootBaseInstinct() {
        this(1);
    }
    
    public ShootBaseInstinct(int weight) {
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
