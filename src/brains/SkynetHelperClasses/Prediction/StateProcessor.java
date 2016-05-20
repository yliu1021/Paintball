package brains.SkynetHelperClasses.Prediction;

import brains.SkynetHelperClasses.GameInterfaces.GameState;

import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Created by Yuhan on 5/15/16.
 */
public class StateProcessor extends Thread {

    public static final boolean OPT_TEAMMATES = false;
    public static final boolean OPT_ENEMIES   = false;
    public static final boolean OPT_SHOTS     = false;
    public static final boolean OPT_BARRIERS  = false;
    public static final boolean OPT_POINTS    = false;

    public static final int MAX_STATES = 100;

    private final MarkovChain predChain;
    private final Queue<GameState> states;
    private Thread thread;

    private boolean processing = true;

    public StateProcessor(MarkovChain predChain, Queue<GameState> states) {
        this.predChain = predChain;
        this.states = states;
    }

    @Override
    public void run() {
        GameState previousState = null;
        while (processing) {
            System.out.println("size" + states.size());
            while (states.isEmpty()) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }

            // remove excess states
            int size = states.size();
            if (size > MAX_STATES) {
                int diff = size - MAX_STATES - 1;
                while (diff-- > 0)
                    states.remove();
                previousState = states.poll();
            }

            try {
                GameState newState;
                synchronized (states) {
                    newState = states.poll();
                }
                if (previousState != null) {
                    processState(previousState, newState);
                }
                previousState = newState;
            } catch (NoSuchElementException e) {
                System.out.println("Tried to remove element from empty queue\nError in concurrency code");
            }
        }
    }

    @Override
    public void start() {
        if (this.thread == null) {
            this.thread = new Thread(this, "StateProcessorThread");
        }
        this.thread.start();
    }

    /**
     * Terminates thread
     */
    public synchronized void stopRunning() {
        processing = false;
    }

    /**
     * Restarts/refreshes the processing queue
     */
    public synchronized void refresh() {
        processing = true;
        notify();
    }

    private void processState(GameState oldState, GameState newState) {

    }
}
