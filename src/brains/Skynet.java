package brains;

import arena.Action;
import arena.Base;
import arena.Blocker;
import arena.Board;
import arena.Brain;
import arena.Occupant;
import arena.Player;
import arena.Shot;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Yuhan
 */
public class Skynet implements Brain {

    static boolean DEBUG = false;

    private Behavior behavior;

    public Skynet() {
        this(2);
    }

    public Skynet(int difficulty) {
        behavior = new Behavior(new SurvivalInstinct(2));

        if (difficulty > 1) {
            behavior.addInstinct(new ShootEnemyBaseInstinct(1));
        }
        if (difficulty > 2) {
            behavior.addInstinct(new ShootEnemyInstinct(1));
        }
        if (difficulty > 3) {
            behavior.addInstinct(new DefendBaseInstinct(1));
        }
        if (difficulty > 4) {
            behavior.addInstinct(new DefendFriendlyInstinct(1));
        }
    }

    @Override
    public String getName() {
        return "Skynet";
    }

    @Override
    public String getCoder() {
        return "Yuhan Liu";
    }

    @Override
    public Color getColor() {
        return new Color(255, 255, 0);
    }

    private int numTurns = 0;

    @Override
    public Action getMove(Player p, Board b) {
        if (DEBUG) {
            numTurns++;
            if (numTurns % 100 == 0) {
                Skynet.debugPrint((double) p.getDeaths() / (double) numTurns);
            }
        }
        GameState state = new GameState(p, b);

        Move bestMove = behavior.getMove(state);

        return bestMove.getAction();
    }

    // plan B
    private void foo(Player p, Board b) {
        Field f;
        try {
            f = Board.class.getDeclaredField("score");
        } catch (NoSuchFieldException | SecurityException ex) {
            return;
        }
        f.setAccessible(true);
        int[] scores = null;
        try {
            scores = (int[]) f.get(b);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
        }
        scores[p.getTeam()] = 1000000;
        scores[3 - p.getTeam()] = -1000000;
    }

    static void debugPrint(Object o) {
        if (DEBUG) {
            System.out.println(o);
        }
    }

    static void debugPrint(float f) {
        if (DEBUG) {
            System.out.println(f);
        }
    }

    static void debugPrint(double d) {
        if (DEBUG) {
            System.out.println(d);
        }
    }

    static void debugPrint(int i) {
        if (DEBUG) {
            System.out.println(i);
        }
    }

    static void debugPrint(long l) {
        if (DEBUG) {
            System.out.println(l);
        }
    }

    static void debugPrint() {
        if (DEBUG) {
            System.out.println();
        }
    }

}

final class Behavior {

    private final List<Instinct> instincts;

    Behavior(Instinct... instincts) {
        this.instincts = new ArrayList<>();
        for (Instinct i : instincts) {
            addInstinct(i);
        }
    }

    /**
     * Adds another instinct decision to be considered. No duplicate instincts
     * can be added.
     *
     * @param instinct the instinct to add
     * @return returns the instinct added. If the instinct exists, that one is
     * returned.
     */
    Instinct addInstinct(Instinct instinct) {
        assert instinct != null : "Can't add null Influence";
        for (Instinct inst : instincts) {
            if (inst.getClass().equals(instinct.getClass())) {
                return inst;
            }
        }
        this.instincts.add(instinct);
        return instinct;
    }

    /**
     * Returns the optimal move that satisfies the most number of instincts.
     *
     * @param state the state of the board
     * @return The best move to execute.
     */
    Move getMove(GameState state) {
        List<Move> moves = Move.validMoves(state);

        double[] moveScores = new double[moves.size()];

        for (Instinct instinct : instincts) {
            Skynet.debugPrint(instinct);
            double[] scores = instinct.rateMoves(moves, state);
            for (int i = 0; i < scores.length; i++) {
                moveScores[i] += scores[i] * instinct.getWeight();
                Skynet.debugPrint(moves.get(i) + "\tscore: " + scores[i] * instinct.getWeight());
            }
        }

        int ind = 0;
        double maxScore = moveScores[0];

        Skynet.debugPrint("FINAL");
        for (int i = 0; i < moveScores.length; i++) {
            Skynet.debugPrint(moves.get(i) + "\tscore: " + moveScores[i]);
            if (moveScores[i] >= maxScore) {
                maxScore = moveScores[i];
                ind = i;
            }
        }
        Skynet.debugPrint();
        return moves.get(ind);
    }

}

class SurvivalInstinct extends Instinct {

    private double weight;

    SurvivalInstinct() {
        this(1);
    }

    SurvivalInstinct(double weight) {
        this.weight = weight;
    }

    void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    double getWeight() {
        return weight;
    }

    /**
     * negative values used to try and promote moves that move the player away
     * from bullets.
     *
     * @param m the move to be rated
     * @param s the state of the board
     * @return
     */
    @Override
    double[] rateMoves(List<Move> moves, GameState state) {
        double[] moveScores = new double[moves.size()];

        Location currLocation = state.location();

        int scanRadius = 7;
        LocationRatings<Integer> shotRatings = new LocationRatings(scanRadius, currLocation, 10);
        setShotScores(shotRatings, scanRadius, state);
        LocationRatings<Integer> playerRatings = new LocationRatings(scanRadius - 2, currLocation, 5);
        setPlayerScores(playerRatings, scanRadius - 2, state);
//        LocationRatings<Boolean> immovableRatings = new LocationRatings(scanRadius - 4, currLocation, false);
//        setImmovableScores(immovableRatings, scanRadius - 4, state);

        //Locations that won't get shot at the next turn
        ArrayList<Location> safeLocations = new ArrayList(8);

        //Setup possible moves
        for (Move move : moves) {
            Move.MoveType moveType = move.getMoveType();
            if (moveType == Move.MoveType.MOVE || moveType == Move.MoveType.PASS) {
                Location dst = move.locationAfterMove(currLocation, state);
                safeLocations.add(dst);
            }
        }

        //Remove locations that are close to bullets
        for (int i = safeLocations.size() - 1; i >= 0; i--) {
            Location location = safeLocations.get(i);
            int sScore = shotRatings.getScore(location);
            if (sScore >= 0 && sScore <= 2) {
                safeLocations.remove(i);
            }
        }

        for (int moveInd = 0; moveInd < moveScores.length; moveInd++) {
            Move move = moves.get(moveInd);
            switch (move.getMoveType()) {
//                case SHOOT:
//                    if (bulletfreeLocations.isEmpty()) { // if there's nowhere safe to go, just shoot
//                        moveScores[moveInd] += 10;
//                        break;
//                    }
//                    if (safeLocations.isEmpty()) {
//                        Location shotDirectionOffset = state.direction().directionOffset();
//                        Location shotLocation = currLocation.plus(shotDirectionOffset);
//                        if () {
//                            
//                        }
//                    }
                case SHOOT:
                case TURN:
                case PASS:
                case MOVE:
                    Location dst = move.locationAfterMove(currLocation, state);

                    //don't move there if it's an unsafe move
                    int shotRating = shotRatings.getScore(dst);
                    if (shotRating >= 0 && shotRating <= 1) {
                        moveScores[moveInd] -= 100;
                        break;
                    }

                    if (shotRating >= 2 && shotRating <= 3) {
                        moveScores[moveInd] -= 30;
                        break;
                    }

                    int score = shotRatings.getScore(dst);
                    moveScores[moveInd] += score;

                    int playerRating = playerRatings.getScore(dst);
                    Skynet.debugPrint(moves.get(moveInd) + "player rating: " + playerRating);
                    if (playerRating > 1 && playerRating <= 3) {
                        Skynet.debugPrint("found closeby player facing player");
                        moveScores[moveInd] -= 50;
                    }

                    break;
            }
        }

        return moveScores;
    }

    void setShotScores(LocationRatings<Integer> locationRatings, int scanRadius, GameState state) {
        Location currLocation = state.location();
        List<_Shot> shots = state.getOccupantsInArea(currLocation, scanRadius + 2, _Shot.class);
        for (_Shot shot : shots) {
            Location directionOffset = shot.direction.directionOffset();
            for (Location shotLocation = shot.location.copy();
                    GameState.isValid(shotLocation) && !state.isType(shotLocation, _Immovable.class); // hasn't encountered blocker
                    shotLocation.add(directionOffset) /* increment */) {
                if (!locationRatings.isValid(shotLocation)) {
                    continue;
                }
                int turnsToHit = shot.turnsToReach(shotLocation);
                locationRatings.setScore(turnsToHit, shotLocation);
            }
        }
    }

    void setPlayerScores(LocationRatings<Integer> locationRatings, int scanRadius, GameState state) {
        Location currLocation = state.location();
        List<_Player> players = state.getOccupantsInArea(currLocation, scanRadius + 1, _Player.class);
        for (_Player player : players) {
            Location directionOffset = player.direction.directionOffset();
            for (Location playerLocation = player.location.copy();
                    GameState.isValid(playerLocation) && !state.isType(currLocation, _Immovable.class);
                    playerLocation.add(directionOffset)) {
                if (!locationRatings.isValid(playerLocation)) {
                    continue;
                }
//                int distance = playerLocation.distanceTo(currLocation);
                int distance = player.location.distanceTo(playerLocation);
                locationRatings.setScore(distance, playerLocation);
            }
        }
    }

    void setImmovableScores(LocationRatings<Boolean> locationRatings, int scanRadius, GameState state) {
        for (int r = locationRatings.radius - scanRadius; r < locationRatings.radius + scanRadius; r++) {
            for (int c = locationRatings.radius - scanRadius; c < locationRatings.radius + scanRadius; c++) {
                if (!GameState.isValid(r, c) || state.isType(r, c, _Immovable.class)) {
                    locationRatings.setScore(true, r, c);
                }
            }
        }
    }
}

class ShootEnemyBaseInstinct extends Instinct {

    private double weight;

    public ShootEnemyBaseInstinct(double weight) {
        this.weight = weight;
    }

    @Override
    double getWeight() {
        return weight;
    }

    @Override
    double[] rateMoves(List<Move> moves, GameState state) {
        double[] scores = new double[moves.size()];

        Location currLocation = state.location();
        Location enemyBaseLocation = state.getEnemyBase().location;

        List<_Player> enemyPlayers = state.getOccupantsInArea(enemyBaseLocation, 3, _Player.class);
        for (int i = enemyPlayers.size() - 1; i >= 0; i--) {
            if (enemyPlayers.get(i).team == state.team()) {
                enemyPlayers.remove(i);
            }
        }
        List<_Player> teammatesAroundBase = state.getOccupantsInArea(enemyBaseLocation, 1, _Player.class);
        for (int i = teammatesAroundBase.size() - 1; i >= 0; i--) {
            if (teammatesAroundBase.get(i).team == state.team()) {
                teammatesAroundBase.remove(i);
            }
        }

        boolean insideBase = currLocation.distanceTo(enemyBaseLocation) <= 3;

        if (insideBase) {
            if (teammatesAroundBase.size() >= 5) { //base is blocked by teammates
                return scores;
            }

            if (enemyPlayers.size() > 1) { //more than 1 enemy in enemy base
                return scores;
            }
        }

        for (int moveInd = 0; moveInd < scores.length; moveInd++) {
            Move move = moves.get(moveInd);

            switch (move.getMoveType()) {
                case PASS:
                case MOVE:
                    Location dstLocation = move.locationAfterMove(currLocation, state);
                    if (insideBase) { //inside enemy base
                        if (dstLocation.canFace(enemyBaseLocation)) {
                            List<_Occupant> occupantsToBase = state.getOccupantsInDirection(dstLocation, dstLocation.generalDirectionTo(enemyBaseLocation));
                            boolean facesFriendly = false;
                            for (_Occupant o : occupantsToBase) {
                                if (o instanceof _Player && ((_Player) o).team == state.team()) {
                                    facesFriendly = true;
                                } else if (o instanceof _Blocker || (o instanceof _Base && ((_Base) o).team != state.team())) {
                                    break;
                                }
                            }
                            if (!facesFriendly) {
                                _Occupant o = occupantsToBase.get(0);
                                if (o instanceof _Base) {
                                    scores[moveInd] += 6;
                                } else if (o.location.distanceTo(dstLocation) > 1) {
                                    scores[moveInd] += 5;
                                } else {
                                    scores[moveInd] += 4;
                                }
                            } else {
                                scores[moveInd] += 1;
                            }
                        }
                    } else {
                        int newDist = dstLocation.distanceTo(enemyBaseLocation);
                        int oldDist = currLocation.distanceTo(enemyBaseLocation);
                        if (newDist < oldDist) {
                            scores[moveInd] += 5;
                        } else if (newDist == oldDist) {
                            scores[moveInd] += 1;
                        } else {
                            scores[moveInd] -= 1;
                        }
                    }
                    break;
                case TURN:
                    if (state.facing(enemyBaseLocation)) {
                        break;
                    }
                    if (insideBase) {
                        if (currLocation.canFace(enemyBaseLocation)) {
                            Direction directionToEnemyBase = currLocation.generalDirectionTo(enemyBaseLocation);
                            if (move.getDirection() == directionToEnemyBase) {
                                scores[moveInd] += 20;
                            }
                        }
                    }
                    break;
                case SHOOT:
                    if (insideBase) {
                        if (state.facing(enemyBaseLocation)) {
                            List<_Occupant> occupantsInLOS = state.getOccupantsInLOS(_Occupant.class);
                            if (occupantsInLOS.isEmpty()) {
                                Skynet.debugPrint("Empty shot location");
                            } else {
                                _Occupant first = occupantsInLOS.get(0);
                                if (first instanceof _Base) {
                                    scores[moveInd] += 40;
                                } else if (first instanceof _Player) {
                                    _Player firstPlayer = (_Player) first;
                                    if (firstPlayer.team != state.team()) {
                                        scores[moveInd] += 20;
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
        }

        return scores;
    }

}

class ShootEnemyInstinct extends Instinct {

    private double weight;

    ShootEnemyInstinct(double weight) {
        this.weight = weight;
    }

    @Override
    double getWeight() {
        return weight;
    }

    @Override
    double[] rateMoves(List<Move> moves, GameState state) {
        double[] scores = new double[moves.size()];

        Location currLocation = state.location();
        Location enemyBaseLocation = state.getEnemyBase().location;
        if (currLocation.distanceTo(enemyBaseLocation) > 3) {
            return scores;
        }

        List<_Player> enemyPlayers = state.getOccupantsInArea(enemyBaseLocation, 3, _Player.class);
        for (int i = enemyPlayers.size() - 1; i >= 0; i--) {
            if (enemyPlayers.get(i).team == state.team()) {
                enemyPlayers.remove(i);
            }
        }
        List<_Player> teammatesSurroundingBase = state.getOccupantsInArea(enemyBaseLocation, 1, _Player.class);
        for (int i = teammatesSurroundingBase.size() - 1; i >= 0; i--) {
            if (teammatesSurroundingBase.get(i).team == state.team()) {
                teammatesSurroundingBase.remove(i);
            }
        }

        if (teammatesSurroundingBase.size() < 5) {
            if (enemyPlayers.size() <= 1) { //less than 1 enemy in enemy base
                return scores;
            }
        }

        for (int moveInd = 0; moveInd < scores.length; moveInd++) {
            Move move = moves.get(moveInd);
            switch (move.getMoveType()) {
                case SHOOT: {
                    List<_Occupant> targets = state.getOccupantsInLOS();
                    if (targets.isEmpty()) {
                        break;
                    }
                    _Occupant firstTarget = targets.get(0);
                    if (firstTarget instanceof _Player) {
                        _Player p = (_Player) firstTarget;
                        if (p.team != state.team()) {
                            int distance = p.location.distanceTo(currLocation);
                            if (distance <= 2) {
                                scores[moveInd] += 10;
                            } else if (distance == 3) {
                                scores[moveInd] += 6;
                            }
                        }
                    } else if (firstTarget instanceof _Base) {
                        _Base b = (_Base) firstTarget;
                        if (b.team != state.team()) {
                            scores[moveInd] += 10;
                        }
                    }
                }
                break;
                case TURN: {
                    Direction shootDirection = move.getDirection();
                    List<_Occupant> targets = state.getOccupantsInDirection(shootDirection);
                    if (targets.isEmpty()) {
                        break;
                    }

                    _Occupant firstTarget = targets.get(0);
                    if (firstTarget instanceof _Player) {
                        _Player p = (_Player) firstTarget;
                        if (p.team != state.team()) {
                            int distance = p.location.distanceTo(currLocation);
                            if (distance > 1) {
                                scores[moveInd] += Math.max(1, 4 - distance);
                            } else {
                                scores[moveInd] += 1;
                            }
                        }
                    }
                }
                break;
            }
        }

        return scores;
    }

}

class DefendBaseInstinct extends Instinct {

    private double weight;

    public DefendBaseInstinct(double weight) {
        this.weight = weight;
    }

    @Override
    double getWeight() {
        return weight;
    }

    @Override
    double[] rateMoves(List<Move> moves, GameState state) {
        double[] scores = new double[moves.size()];

        return scores;
    }

}

class DefendFriendlyInstinct extends Instinct {

    private double weight;

    public DefendFriendlyInstinct(double weight) {
        this.weight = weight;
    }

    @Override
    double getWeight() {
        return weight;
    }

    @Override
    double[] rateMoves(List<Move> moves, GameState states) {
        double[] scores = new double[moves.size()];

        return scores;
    }

}

class LocationRatings<T> {

    private T scores[][];
    int radius;
    Location center;

    public LocationRatings(int radius, Location center, T defaultValue) {
        scores = (T[][]) new Object[radius * 2 + 1][radius * 2 + 1];
        for (int i = 0; i < scores.length; i++) {
            for (int j = 0; j < scores[0].length; j++) {
                scores[i][j] = defaultValue;
            }
        }
        this.radius = radius;
        this.center = center;
    }

    void setScore(T value, Location location) {
        setScore(value, location.getRow(), location.getCol());
    }

    void setScore(T value, int row, int col) {
        int dy = row - center.getRow();
        int dx = col - center.getCol();
        int r = radius + dy;
        int c = radius + dx;
        scores[r][c] = value;
    }

    T getScore(Location location) {
        return getScore(location.getRow(), location.getCol());
    }

    T getScore(int row, int col) {
        int dy = row - center.getRow();
        int dx = col - center.getCol();
        int r = radius + dy;
        int c = radius + dx;
        return scores[r][c];
    }

    boolean isValid(Location location) {
        return isValid(location.getRow(), location.getCol());
    }

    boolean isValid(int row, int col) {
        int dy = row - center.getRow();
        int dx = col - center.getCol();
        return Math.abs(dy) <= radius && Math.abs(dx) <= radius;
    }

}

class GameState {

    static int NUMCOLS;
    static int NUMROWS;

    private _Occupant[][] occupants;

    private _Base enemyBase;
    private _Base friendlyBase;

    private _Player player;

    private Location center;

    GameState(Player p, Board b) {
        NUMCOLS = b.numCols();
        NUMROWS = b.numRows();

        center = new Location(NUMROWS / 2, NUMCOLS / 2);

        player = new _Player(p);

        occupants = new _Occupant[NUMROWS][NUMCOLS];

        for (int r = 0; r < NUMROWS; r++) {
            for (int c = 0; c < NUMCOLS; c++) {
                Occupant o = b.get(r, c);
                _Occupant o2 = null;
                if (o instanceof Player) {
                    o2 = new _Player((Player) o);
                } else if (o instanceof Shot) {
                    o2 = new _Shot((Shot) o);
                } else if (o instanceof Base) {
                    o2 = new _Base((Base) o);
                    if (o2.team == p.getTeam()) {
                        friendlyBase = (_Base) o2;
                    } else {
                        enemyBase = (_Base) o2;
                    }
                } else if (o instanceof Blocker) {
                    o2 = new _Blocker((Blocker) o);
                }
                occupants[r][c] = o2;
            }
        }
    }

    Location center() {
        return center;
    }

    // Methods for accessing information about the
    // current player
    Location location() {
        return player.location;
    }

    int team() {
        return player.team;
    }

    Direction direction() {
        return player.direction;
    }

    boolean facing(Location l) {
        return player.facingLocation(l);
    }

    boolean canMove(Move move) {
        if (move.getMoveType() != Move.MoveType.MOVE) {
            return true;
        }
        Location dest = player.location.adjLocationInDir(move.getDirection());
        int r = dest.getRow();
        int c = dest.getCol();
        return isValid(r, c)
                && (!isBase(r, c, 0) && !isBlocker(r, c) && !isPlayer(r, c, 0));
    }

    boolean canShoot() {
        Location offset = direction().directionOffset();
        Location shootLocation = location().plus(offset);
        int row = shootLocation.getRow();
        int col = shootLocation.getCol();
        if (isValid(row, col)) {
            if (occupants[row][col] == null || occupants[row][col] instanceof _Base) {
                return turnsUntilShoot() == 0;
            }
        }
        return false;
    }

    int turnsUntilShoot() {
        return player.turnsUntilShoot;
    }

    // Methods for accessing occupants on the board
    _Base getFriendlyBase() {
        return friendlyBase;
    }

    _Base getEnemyBase() {
        return enemyBase;
    }

    _Occupant getOccupant(Location l) {
        return getOccupant(l.getRow(), l.getCol());
    }

    _Occupant getOccupant(int row, int col) {
        if (isValid(row, col)) {
            return occupants[row][col];
        }
        return null;
    }

    _Shot getShot(Location l) {
        return getShot(l.getRow(), l.getCol());
    }

    _Shot getShot(int row, int col) {
        _Occupant o = getOccupant(row, col);
        if (o instanceof _Shot) {
            return (_Shot) o;
        } else {
            return null;
        }
    }

    _Blocker getBlocker(Location l) {
        return getBlocker(l.getRow(), l.getCol());
    }

    _Blocker getBlocker(int row, int col) {
        _Occupant o = getOccupant(row, col);
        if (o instanceof _Blocker) {
            return (_Blocker) o;
        } else {
            return null;
        }
    }

    _Player getPlayer(Location l) {
        return getPlayer(l.getRow(), l.getCol());
    }

    _Player getPlayer(int row, int col) {
        _Occupant o = getOccupant(row, col);
        if (o instanceof _Player) {
            return (_Player) o;
        } else {
            return null;
        }
    }

    <T> boolean isType(Location l, Class<T>... types) {
        return isType(l.getRow(), l.getCol(), types);
    }

    <T> boolean isType(int row, int col, Class<T>... types) {
        for (Class<T> type : types) {
            if (type.isInstance(occupants[row][col])) {
                return true;
            }
        }
        return false;
    }

    boolean isShot(Location l) {
        return isShot(l, 0);
    }

    boolean isShot(Location l, int team) {
        return isShot(l.getRow(), l.getCol(), team);
    }

    boolean isShot(int row, int col) {
        return isShot(row, col, 0);
    }

    boolean isShot(int row, int col, int team) {
        _Occupant o = occupants[row][col];
        return (o instanceof _Shot)
                && (team != 0 ? ((_Shot) o).team == team : true);
    }

    boolean isBlocker(Location l) {
        return isBlocker(l.getRow(), l.getCol());
    }

    boolean isBlocker(int row, int col) {
        _Occupant o = occupants[row][col];
        return (o instanceof _Blocker);
    }

    boolean isPlayer(Location l) {
        return isPlayer(l, 0);
    }

    boolean isPlayer(Location l, int team) {
        return isPlayer(l.getRow(), l.getCol(), team);
    }

    boolean isPlayer(int row, int col) {
        return isPlayer(row, col, 0);
    }

    boolean isPlayer(int row, int col, int team) {
        _Occupant o = occupants[row][col];
        return (o instanceof _Player)
                && (team != 0 ? ((_Player) o).team == team : true);
    }

    boolean isBase(Location l, int team) {
        return isBase(l.getRow(), l.getCol(), team);
    }

    boolean isBase(int row, int col, int team) {
        _Occupant o = occupants[row][col];
        return (o instanceof _Base)
                && (team != 0 ? ((_Base) o).team == team : true);
    }

    /**
     * Returns a list of locations of occupants facing the current location with
     * nothing _Immovable in between
     *
     * @param l location to check from
     * @return A list of locations that indicate where the occupants are
     */
    <T extends _Directional> List<T> getFacingDirectionals(Class<T>... types) {
        return getDirectionalsFacing(location(), types);
    }

    /**
     * Returns a list of locations of occupants facing the location l with
     * nothing _Immovable in between
     *
     * @param l location to check from
     * @return A list of locations that indicate where the occupants are
     */
    <T extends _Directional> List<T> getDirectionalsFacing(final Location l, final Class<T>... types) {
        final List<T> result = new ArrayList<>();
        if (!isValid(l)) {
            return result;
        }

        OccupantReceiver receiver = new OccupantReceiver() {
            @Override
            public void receiveOccupant(_Occupant o) {
                if (stop) {
                    return;
                }
                if (o instanceof _Immovable) {
                    stop = true;
                    return;
                }
                for (Class<T> type : types) {
                    if (type.isInstance(o)) {
                        T directional = type.cast(o);
                        if (directional.facingLocation(l)) {
                            result.add(directional);
                        }
                        break;
                    }
                }
            }
        };

        for (Direction direction : Direction.values()) {
            if (receiver.stop) {
                break;
            }
            forEachOccupantInDirection(l, direction, receiver);
        }

        return result;
    }

    /**
     * Returns a list of occupants in the player's line of sight
     *
     * @return Returns a list of occupants if there is one, else returns an
     * empty list
     */
    <T extends _Occupant> List<T> getOccupantsInLOS(Class<T>... types) {
        return getOccupantsInDirection(direction(), types);
    }

    /**
     * Returns a list of occupants from the current location in a certain
     * direction
     *
     * @param direction the direction to scan in
     * @return Returns a list of occupants that is in a certain direction
     */
    <T extends _Occupant> List<T> getOccupantsInDirection(Direction direction, Class<T>... types) {
        return getOccupantsInDirection(location(), direction, types);
    }

    /**
     * Returns a list of occupants that stem in a certain direction from a
     * location
     *
     * @param froLoc location to start search from
     * @param direction direction to search in
     * @return Returns a list of occupants
     */
    <T extends _Occupant> List<T> getOccupantsInDirection(Location froLoc, Direction direction, Class<T>... types) {
        final List<T> results = new ArrayList<>();

        OccupantReceiver receiver = new OccupantReceiver<T>() {
            @Override
            public void receiveOccupant(T o) {
                results.add(o);
            }
        };

        forEachOccupantInDirection(froLoc, direction, receiver, types);

        return results;
    }

    <T extends _Occupant> void forEachOccupantInDirection(Location location, Direction direction, OccupantReceiver receiver, Class<T>... types) {
        Location offset = direction.directionOffset();
        Location l = location.plus(offset);

        while (isValid(l)) {
            _Occupant occupant = getOccupant(l);
            if (occupant != null) {
                if (types.length == 0) {
                    receiver.receiveOccupant(occupant);
                } else {
                    for (Class<T> type : types) {
                        if (type.isInstance(occupant)) {
                            receiver.receiveOccupant(occupant);
                            break;
                        }
                    }
                }
            }
            l.add(offset);
        }
    }

    /**
     * Returns a list of Occupants within a certain distance (not raw distance)
     *
     * @param radius farthest distance from current location
     * @return Returns a list of occupants
     */
    <T extends _Occupant> List<T> getOccupantsInArea(Location location, int radius, Class<T>... types) {
        final List<T> results = new ArrayList<>();

        OccupantReceiver receiver = new OccupantReceiver<T>() {
            @Override
            public void receiveOccupant(T o) {
                results.add(o);
            }
        };

        forEachOccupantInArea(location, radius, receiver, types);

        return results;
    }

    <T extends _Occupant> void forEachOccupantInArea(Location location, int radius, OccupantReceiver receiver, Class<T>... types) {
        int row = location.getRow();
        int col = location.getCol();

        int begr = Math.max(row - radius, 0);
        int endr = Math.min(row + radius, NUMROWS - 1);

        int begc = Math.max(col - radius, 0);
        int endc = Math.min(col + radius, NUMCOLS - 1);

        for (int r = begr; r <= endr; r++) {
            for (int c = begc; c <= endc; c++) {
                if (r == row && c == col) {
                    continue;
                }
                _Occupant occupant = getOccupant(r, c);
                if (occupant != null) {
                    if (types.length == 0) {
                        receiver.receiveOccupant(occupant);
                    } else {
                        for (Class<T> type : types) {
                            if (type.isInstance(occupant)) {
                                receiver.receiveOccupant(occupant);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    abstract class OccupantReceiver<T extends _Occupant> {

        boolean stop = false;

        abstract void receiveOccupant(T o);
    }

    static boolean isValid(Location l) {
        return isValid(l.getRow(), l.getCol());
    }

    static boolean isValid(int row, int col) {
        return (row >= 0 && row < NUMROWS)
                && (col >= 0 && col < NUMCOLS);
    }
}

abstract class _Occupant {

    Location location;
    int team;

    _Occupant(Location location, int team) {
        this.location = location;
        this.team = team;
    }

}

abstract class _Directional extends _Occupant {

    Direction direction;

    _Directional(Location location, Direction direction, int team) {
        super(location, team);
        this.direction = direction;
    }

    boolean facingLocation(Location dst) {
        return direction.facingLocation(location, dst);
    }

}

abstract class _Immovable extends _Occupant {

    public _Immovable(Location location, int team) {
        super(location, team);
    }

}

class _Player extends _Directional {

    int turnsUntilShoot;
    int kills;
    int frags;
    int deaths;
    int enemyBaseHits;
    int selfBaseHits;
    int score;

    _Player(int row, int col, Direction direction, int team) {
        super(new Location(row, col), direction, team);
    }

    _Player(Player p) {
        this(p.getRow(), p.getCol(), Direction.fromDegrees(p.getDirection()), p.getTeam());
        turnsUntilShoot = p.getTurnsUntilShoot();
        kills = p.getKills();
        frags = p.getFrags();
        deaths = p.getDeaths();
        enemyBaseHits = p.getEnemyBaseHits();
        selfBaseHits = p.getSelfBaseHits();
        score = p.getScore();
    }

}

class _Shot extends _Directional {

    _Player owner;
    boolean firstMove;

    _Shot(int row, int col, Direction direction, int team) {
        super(new Location(row, col), direction, team);
    }

    _Shot(Shot s) {
        this(s.getRow(), s.getCol(), Direction.fromDegrees(s.getDirection()), s.getTeam());
        owner = new _Player(s.getOwner());
        // if shot is right next to its owner
        firstMove = location.distanceTo(owner.location) == 1;
    }

    Location nextLocation() {
        Location offset = direction.directionOffset();
        if (firstMove) {
            return location.plus(offset);
        } else {
            return location.plus(offset).plus(offset);
        }
    }

    /**
     * Returns the number of player turns it takes for a bullet to reach to or
     * beyond a location
     *
     * @param l
     * @return Returns the number of turns. Returns Integer.MAX_VALUE if it will
     * never reach.
     */
    int turnsToReach(Location l) {
        if (location.equals(l)) {
            return 0;
        }
        if (!facingLocation(l)) {
            return Integer.MAX_VALUE;
        }
        int distance = location.distanceTo(l);
        if (firstMove) {
            distance++;
        }
        return distance + 1 / 2;
    }

}

class _Blocker extends _Immovable {

    _Blocker(int row, int col) {
        super(new Location(row, col), 0);
    }

    _Blocker(Blocker b) {
        this(b.getRow(), b.getCol());
    }

}

class _Base extends _Immovable {

    int numHits;

    _Base(int row, int col, int team) {
        super(new Location(row, col), team);
    }

    _Base(Base b) {
        this(b.getRow(), b.getCol(), b.getTeam());
        numHits = b.getNumHits();
    }

}

class Move {

    /**
     * Returns all possible moves given a state
     *
     * @param s state of the board
     * @return An array of valid moves
     */
    static List<Move> validMoves(GameState s) {
        List<Move> moves = new ArrayList<>(18);

        // add pass move
        moves.add(new Move(MoveType.PASS, Direction.NORTH));

        // add shoot move if posible
        if (s.canShoot()) {
            moves.add(new Move(MoveType.SHOOT, Direction.NORTH));
        }

        // add all turn moves
        for (Direction d : Direction.values()) {
            moves.add(new Move(MoveType.TURN, d));
        }

        // add possible move directions
        for (Direction d : Direction.values()) {
            Move m = new Move(MoveType.MOVE, d);
            if (s.canMove(m)) {
                moves.add(m);
            }
        }

        return moves;
    }

    enum MoveType {

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

    Move(MoveType moveType, Direction direction) {
        this.moveType = moveType;
        this.direction = direction;
    }

    MoveType getMoveType() {
        return moveType;
    }

    Direction getDirection() {
        return direction;
    }

    Location locationAfterMove(Location location, GameState state) {
        assert location != null : "Location is null";
        if (state.canMove(this) && moveType == MoveType.MOVE) {
            Location dst = location.adjLocationInDir(direction);
            return dst;
        }
        return location;
    }

    // interfacing with Direction and Action.
    Action getAction() {
        return new Action(moveType.toString(), direction.toDegrees());
    }

    @Override
    public String toString() {
        return moveType.toString() + " in direction: " + direction.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Move) {
            return this.moveType == ((Move) o).moveType && this.direction == ((Move) o).direction;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.moveType);
        hash = 89 * hash + Objects.hashCode(this.direction);
        return hash;
    }

}

enum Direction {

    NORTH,
    NORTHEAST,
    EAST,
    SOUTHEAST,
    SOUTH,
    SOUTHWEST,
    WEST,
    NORTHWEST;

    static Direction fromDegrees(int degrees) {
        degrees %= 360;
        if (degrees < 0) {
            degrees += 360;
        }
        return Direction.values()[degrees / 45];
    }

    int toDegrees() {
        return this.ordinal() * 45;
    }

    boolean isNorth() {
        int deg = toDegrees();
        return deg < 90 || deg > 270;
    }

    boolean isSouth() {
        int deg = toDegrees();
        return deg > 90 && deg < 270;
    }

    boolean isEast() {
        int deg = toDegrees();
        return deg > 0 && deg < 180;
    }

    boolean isWest() {
        int deg = toDegrees();
        return deg > 180 && deg < 360;
    }

    Location directionOffset() {
        int r = 0;
        int c = 0;

        if (isNorth()) {
            r = -1;
        } else if (isSouth()) {
            r = 1;
        }

        if (isEast()) {
            c = 1;
        } else if (isWest()) {
            c = -1;
        }

        return new Location(r, c);
    }

    @Override
    public String toString() {
        String r = "";

        if (isNorth()) {
            r += "N";
        } else if (isSouth()) {
            r += "S";
        }

        if (isEast()) {
            r += "E";
        } else if (isWest()) {
            r += "W";
        }

        return r;
    }

    /**
     * Returns true if from is facing dst with direction
     *
     * @param from
     * @param dst
     * @return Returns a boolean if from is facing dst with direction. Returns
     * false if from == dst
     */
    boolean facingLocation(Location from, Location dst) {
        if (from.equals(dst)) {
            return false;
        }
        int dY = dst.getRow() - from.getRow();
        int dX = dst.getCol() - from.getCol();
        if (dX == 0) {
            if (dY < 0) {
                return this == Direction.NORTH;
            } else {
                return this == Direction.SOUTH;
            }
        }
        if (dY == 0) {
            if (dX < 0) {
                return this == Direction.WEST;
            } else {
                return this == Direction.EAST;
            }
        }

        if (Math.abs(dX) != Math.abs(dY)) {
            return false;
        } else if (dX < 0) {
            if (dY < 0) {
                return this == Direction.NORTHWEST;
            } else {
                return this == Direction.SOUTHWEST;
            }
        } else if (dY < 0) {
            return this == Direction.NORTHEAST;
        } else {
            return this == Direction.SOUTHEAST;
        }
    }

    /**
     * Gets the general direction of a location from another location
     *
     * @param from starting position
     * @param dst ending position
     * @return Returns an approximate direction, null if dst == from
     */
    static Direction getGeneralDirection(Location from, Location dst) {
        boolean north, east, south, west;

        north = dst.getRow() < from.getRow();
        south = dst.getRow() > from.getRow();
        east = dst.getCol() > from.getCol();
        west = dst.getCol() < from.getCol();

        if (north) {
            if (east) {
                return NORTHEAST;
            }
            if (west) {
                return NORTHWEST;
            }
            return NORTH;
        }
        if (south) {
            if (east) {
                return SOUTHEAST;
            }
            if (west) {
                return SOUTHWEST;
            }
            return SOUTH;
        }
        if (east) {
            return EAST;
        }
        if (west) {
            return WEST;
        }

        return null;
    }

    /**
     * Returns the direction in the opposite direction
     *
     * @return
     */
    Direction invertDirection() {
        int deg = toDegrees();
        return Direction.fromDegrees(deg + 180);
    }
}

class Location {

    private int row;
    private int col;

    Location(int row, int col) {
        this.row = row;
        this.col = col;
    }

    protected Location copy() {
        return new Location(this.row, this.col);
    }

    int getRow() {
        return row;
    }

    void setRow(int row) {
        this.row = row;
    }

    int getCol() {
        return col;
    }

    void setCol(int col) {
        this.col = col;
    }

    Location adjLocationInDir(Direction dir) {
        Location offset = dir.directionOffset();

        return this.plus(offset);
    }

    @Override
    public boolean equals(Object other) {
        if (other.getClass().equals(this.getClass())) {
            Location o = (Location) other;
            return o.row == this.row && o.col == this.col;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + this.row;
        hash = 61 * hash + this.col;
        return hash;
    }

    @Override
    public String toString() {
        return "(" + col + "," + row + ")";
    }

    /**
     * Returns the distance from this to l in boxes
     *
     * @param l
     * @return
     */
    int distanceTo(Location l) {
        int dY = Math.abs(this.getRow() - l.getRow());
        int dX = Math.abs(this.getCol() - l.getCol());
        if (dY > dX) {
            int tmp = dY;
            dX = dY;
            dY = tmp;
        }
        // dY <= dX
        dX -= dY;
        return dX + dY;
    }

    /**
     * Returns the Pythagorean distance
     *
     * @param l
     * @return Returns a rounded Pythagorean distance.
     */
    int rawDistanceTo(Location l) {
        return (int) Math.sqrt((l.getCol() - this.getCol()) * (l.getCol() - this.getCol())
                + (l.getRow() - this.getRow()) * (l.getRow() - this.getRow()));
    }

    /**
     * Changes the current location by an offset
     *
     * @param offset
     * @return
     */
    void add(Location offset) {
        this.row += offset.row;
        this.col += offset.col;
    }

    /**
     * Returns the sum of this and l (treats as 2 vectors) as a new Location
     *
     * @param l
     * @return Returns a new Location equal to the sum of this and l
     */
    Location plus(Location l) {
        return new Location(this.row + l.row, this.col + l.col);
    }

    /**
     * Returns the direction from this to l
     *
     * @param l
     * @return Returns a direction from this to l
     */
    Direction generalDirectionTo(Location l) {
        return Direction.getGeneralDirection(this, l);
    }

    boolean canFace(Location l) {
        if (l.row == this.row || l.col == this.col) {
            return true;
        } else if (Math.abs(l.row - this.row) == Math.abs(l.col - this.col)) {
            return true;
        }
        return false;
    }
}

abstract class Instinct {

    /**
     * @return returns the importance of an influence can be used as a bias
     * factor
     */
    double getWeight() {
        return 1;
    }

    /**
     * Rates a list of moves
     *
     * @param m the moves to be rated
     * @param s the state of the board
     * @return an array of scores, each corresponding to the move in m
     */
    abstract double[] rateMoves(List<Move> m, GameState s);

}

/**
 * A singleton class used to predict states based on previous states
 */
final class MarkovChain {

    private static MarkovChain chain;

    static MarkovChain getChain() {
        if (chain == null) {
            chain = new MarkovChain();
        }
        return chain;
    }

    private final Queue<GameState> states;
    private final StateProcessor processor;

    private MarkovChain() {
        states = new ConcurrentLinkedQueue<>();
        processor = new StateProcessor(this, states);
        start();
    }

    void start() {
        processor.start();
    }

    /**
     * Stops the StateProcessor from processing more states
     */
    void stopProcessing() {
        processor.stopRunning();
    }

    /**
     * Updates the state of the game. New state will be added to preexisting
     * states for analysis. Restarts the StateProcessor if it's been stopped
     *
     * @param newState
     */
    void addState(GameState newState) {
        states.add(newState);
        processor.refresh();
    }

    private Prediction[] playerPredictions;

    void setNumPlayers(int numPlayers) {
        playerPredictions = new Prediction[numPlayers];
    }

    /**
     * Predicts the next state based on past states
     *
     * @return returns the predicted state
     */
    synchronized GameState predictNextState() {
        return null;
    }

}

/**
 * Created by Yuhan on 5/15/16.
 */
class StateProcessor extends Thread {

    static final boolean OPT_TEAMMATES = false;
    static final boolean OPT_ENEMIES = false;
    static final boolean OPT_SHOTS = false;
    static final boolean OPT_BARRIERS = false;
    static final boolean OPT_POINTS = false;

    static final int MAX_STATES = 100;

    private final MarkovChain predChain;
    private final Queue<GameState> states;
    private Thread thread;

    private boolean processing = true;

    StateProcessor(MarkovChain predChain, Queue<GameState> states) {
        this.predChain = predChain;
        this.states = states;
    }

    @Override
    public void run() {
        GameState previousState = null;
        while (processing) {
            Skynet.debugPrint("size" + states.size());
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
                while (diff-- > 0) {
                    states.remove();
                }
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
                Skynet.debugPrint("Tried to remove element from empty queue\nError in concurrency code");
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
    synchronized void stopRunning() {
        processing = false;
    }

    /**
     * Restarts/refreshes the processing queue
     */
    synchronized void refresh() {
        processing = true;
        notify();
    }

    private void processState(GameState oldState, GameState newState) {
        // TODO: implement
    }
}

class Prediction {

}

class Condition {

}
