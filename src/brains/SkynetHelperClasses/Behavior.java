package brains.SkynetHelperClasses;

import brains.SkynetHelperClasses.GameInterfaces.*;
import brains.SkynetHelperClasses.Instincts.Instinct;

import java.util.*;

/**
 * Finds an optimal move by iterating through
 * and rating all possible moves using the
 * different Influences affecting it.
 */
public final class Behavior {

    private final List<Instinct> influenceList;

    public Behavior(Instinct... influences) {
        influenceList = new ArrayList<>();
        for (Instinct i : influences) {
            addInfluence(i);
        }
    }

    /**
     * Adds another influence decision to be considered.
     * No duplicate influences will be added.
     * @param influence the influence to add
     * @return returns the influence added. If the
     * influence exists, that one is returned.
     */
    public Instinct addInfluence(Instinct influence) {
        assert influence != null : "Can't add null Influence";
        for (Instinct inf : influenceList) {
            if (inf.getClass().equals(influence.getClass()))
                return inf;
        }
        this.influenceList.add(influence);
        return influence;
    }


    /**
     * Returns the optimal move that satisfies the most number
     * of influences.
     * @param state the state of the board
     * @return
     * The best move to execute.
     */
    public Move getMove(GameState state) {
        Object[] moves = Move.validMoves(state);

        int ind = 0;
        int maxScore = 0;

        for (Instinct influence : influenceList) {
            maxScore += influence.rateMove((Move)moves[ind], state) * influence.getWeight();
        }

        for (int i = 1; i < moves.length; i++) {
            int score = 0;
            for (Instinct influence : influenceList) {
                score += influence.rateMove((Move)moves[i], state) * influence.getWeight();
            }
            if (score >= maxScore) {
                maxScore = score;
                ind = i;
            }
        }
        
        return (Move)moves[ind];
    }

}