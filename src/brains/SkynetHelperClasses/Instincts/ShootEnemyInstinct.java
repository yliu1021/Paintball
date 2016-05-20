package brains.SkynetHelperClasses.Instincts;

import brains.SkynetHelperClasses.GameInterfaces.GameState;
import brains.SkynetHelperClasses.GameInterfaces.Move;

/**
 * Instinct class used to target enemies by shooting
 when they are in line of sight or when they are
 approaching line of sight
 */
public class ShootEnemyInstinct extends Instinct {

    private int weight;

    public ShootEnemyInstinct() {
        this(1);
    }
    
    public ShootEnemyInstinct(int weight) {
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
