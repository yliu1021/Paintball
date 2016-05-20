package brains.SkynetHelperClasses.Prediction;

import brains.SkynetHelperClasses.GameInterfaces.GameState;
import brains.SkynetHelperClasses.Prediction.Prediction;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A singleton class used to predict states based on
 * previous states
 */
public class MarkovChain {

    private static MarkovChain chain;

    public static MarkovChain getChain() {
        if (chain == null) {
            chain = new MarkovChain();
        }
        return chain;
    }

    private Queue<GameState> states;
    private StateProcessor processor;

    private MarkovChain() {
        states = new ConcurrentLinkedQueue<>();
        processor = new StateProcessor(this, states);
        processor.start();
    }

    /**
     * Stops the StateProcessor from processing more states
     */
    public void stopProcessing() {
        processor.stopRunning();
    }

    /**
     * Updates the state of the game.
     * New state will be added to preexisting states
     * for analysis.
     * Restarts the StateProcessor if it's been stopped
     * @param newState
     */
    public void addState(GameState newState) {
        states.add(newState);
        processor.refresh();
    }

    private Prediction[] playerPredictions;

    public void setNumPlayers(int numPlayers) {
        playerPredictions = new Prediction[numPlayers];
    }

    /**
     * Predicts the next state based on past states
     * @return
     * returns the predicted state
     */
    public synchronized GameState predictNextState() {
        return null;
    }

}
