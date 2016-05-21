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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Yuhan
 */
public class Skynet implements Brain {

    private Behavior behavior;

    public Skynet() {
        this(1);
    }

    public Skynet(int difficulty) {
        behavior = new Behavior(new SurvivalInstinct());

        if (difficulty > 1) {
            behavior.addInstinct(new ShootEnemyInstinct());
        }
        if (difficulty > 2) {
            behavior.addInstinct(new ShootEnemyBaseInstinct());
        }
        if (difficulty > 3) {
            behavior.addInstinct(new DefendBaseInstinct());
        }
        if (difficulty > 4) {
            behavior.addInstinct(new DefendFriendlyInstinct());
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
        return Color.MAGENTA;
    }

    @Override
    public Action getMove(Player p, Board b) {
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

}

final class Behavior {

    private final List<Instinct> instincts;

    public Behavior(Instinct... influences) {
        instincts = new ArrayList<>();
        for (Instinct i : influences) {
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
    public Instinct addInstinct(Instinct instinct) {
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
    public Move getMove(GameState state) {
        List<Move> moves = Move.validMoves(state);

        double[] moveScores = new double[moves.size()];

        for (Instinct instinct : instincts) {
            double[] scores = instinct.rateMoves(moves, state);
            for (int j = 0; j < scores.length; j++) {
                moveScores[j] += scores[j] * instinct.getWeight();
            }
        }

        int ind = 0;
        double maxScore = moveScores[0];

        for (int i = 0; i < moveScores.length; i++) {
            if (moveScores[i] >= maxScore) {
                maxScore = moveScores[i];
                ind = i;
            }
        }

        return moves.get(ind);
    }

}

class SurvivalInstinct extends Instinct {

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
    public double getWeight() {
        return weight;
    }

    /**
     * negative values used to try and promote moves that move the player away
     * from bullets.
     *
     * @param m the move to be rated
     * @param s the state of the board
     * @return [-150, 0]
     */
    @Override
    public double[] rateMoves(List<Move> moves, GameState state) {
        double[] scores = new double[moves.size()];

        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            Location dst = move.locationAfterMove(state.location(), state);
            GameState endState;
            double score = rateLocation(dst, state);
            if (move.getMoveType() == Move.MoveType.SHOOT) {
                if (((GameState._Directional) state.getOccupant(state.location())).facingLocation(state.getFriendlyBase().location)) {
                    score -= 1000;
                } else if (((GameState._Directional) state.getOccupant(state.location())).facingLocation(state.getEnemyBase().location)) {
                    score += 2;
                }
                score += 15;
            }
            if (move.getMoveType() == Move.MoveType.TURN) {
                if (dst.distanceTo(state.getEnemyBase().location) < 3) {
                    if (move.getDirection().facingLocation(dst, state.getEnemyBase().location)) {
                        score += 2;
                    }
                }
                if (dst.getCol() > GameState.NUMCOLS / 2) {
                    if (dst.getRow() > GameState.NUMROWS / 2) {
                        score += move.getDirection() == Direction.NORTHWEST ? 2 : 0;
                    } else {
                        score += move.getDirection() == Direction.SOUTHWEST ? 2 : 0;
                    }
                } else if (dst.getRow() > GameState.NUMROWS / 2) {
                    score += move.getDirection() == Direction.NORTHEAST ? 2 : 0;
                } else {
                    score += move.getDirection() == Direction.SOUTHEAST ? 2 : 0;
                }
            }
            if (move.getMoveType() == Move.MoveType.MOVE) {
                if (dst.distanceTo(state.getEnemyBase().location) < state.location().distanceTo(state.getEnemyBase().location)) {
                    score += 2;
                }
            }
            scores[i] = score;
        }

        return scores;
    }

    private double rateLocation(Location l, GameState state) {
        double score = 0;
        List<Location> facingLocations = state.getDirectionalsFacing(l);

        if (facingLocations.isEmpty()) {
            return 0;
        } else {
            for (Location location : facingLocations) {
                if (state.isShot(location, 0)) {
                    score += location.distanceTo(l) - 50;
                } else if (state.isPlayer(location, 3 - state.team())) {
                    score += Math.min(0, location.distanceTo(l) - 6);
                    if (state.getPlayer(location).facingLocation(state.location())) {
                        score -= 10;
                    }
                } else if (state.isPlayer(location, state.team())) {
                    score += Math.min(0, location.distanceTo(l) - 2);
                }
            }
        }

        return score;
    }

}

class ShootEnemyInstinct extends Instinct {

    @Override
    public double[] rateMoves(List<Move> m, GameState s) {
        double[] scores = new double[m.size()];

        return scores;
    }

}

class ShootEnemyBaseInstinct extends Instinct {

    @Override
    public double[] rateMoves(List<Move> m, GameState s) {
        double[] scores = new double[m.size()];

        return scores;
    }

}

class DefendBaseInstinct extends Instinct {

    @Override
    public double[] rateMoves(List<Move> m, GameState s) {
        double[] scores = new double[m.size()];

        return scores;
    }

}

class DefendFriendlyInstinct extends Instinct {

    @Override
    public double[] rateMoves(List<Move> m, GameState s) {
        double[] scores = new double[m.size()];

        return scores;
    }

}

class GameState {

    public static int NUMCOLS;
    public static int NUMROWS;

    private _Occupant[][] occupants;

    private _Base enemyBase;
    private _Base friendlyBase;
    
    private _Player player;
    
    public GameState(Player p, Board b) {
        NUMCOLS = b.numCols();
        NUMROWS = b.numRows();
        
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

    // Methods for accessing information about the
    // current player
    public Location location() {
        return player.location;
    }

    public int team() {
        return player.team;
    }

    public Direction direction() {
        return player.direction;
    }
    
    public boolean canMove(Move move) {
        if (move.getMoveType() != Move.MoveType.MOVE) {
            return true;
        }
        Location dest = player.location.adjLocationInDir(move.getDirection());
        int r = dest.getRow();
        int c = dest.getCol();
        return isValid(r, c)
                && (!isBase(r, c, 0) && !isBlocker(r, c) && !isPlayer(r, c, 0));
    }

    public boolean canShoot() {
        return turnsUntilShoot() == 0;
    }

    public int turnsUntilShoot() {
        return player.turnsUntilShoot;
    }

    // Methods for accessing occupants on the board
    public _Base getFriendlyBase() {
        return friendlyBase;
    }

    public _Base getEnemyBase() {
        return enemyBase;
    }
    
    public _Occupant getOccupant(Location l) {
        return getOccupant(l.getRow(), l.getCol());
    }

    public _Occupant getOccupant(int row, int col) {
        if (isValid(row, col)) {
            return occupants[row][col];
        }
        return null;
    }
    
    public _Shot getShot(Location l) {
        return getShot(l.getRow(), l.getCol());
    }
    
    public _Shot getShot(int row, int col) {
        _Occupant o = getOccupant(row, col);
        if (o instanceof _Shot) {
            return (_Shot)o;
        } else {
            return null;
        }
    }
    
    public _Blocker getBlocker(Location l) {
        return getBlocker(l.getRow(), l.getCol());
    }
    
    public _Blocker getBlocker(int row, int col) {
        _Occupant o = getOccupant(row, col);
        if (o instanceof _Blocker) {
            return (_Blocker)o;
        } else {
            return null;
        }
    }
    
    public _Player getPlayer(Location l) {
        return getPlayer(l.getRow(), l.getCol());
    }
    
    public _Player getPlayer(int row, int col) {
        _Occupant o = getOccupant(row, col);
        if (o instanceof _Player) {
            return (_Player)o;
        } else {
            return null;
        }
    }
    
    public boolean isShot(Location l) {
        return isShot(l, 0);
    }
    
    public boolean isShot(Location l, int team) {
        return isShot(l.getRow(), l.getCol(), team);
    }
    
    public boolean isShot(int row, int col) {
        return isShot(row, col, 0);
    }
    
    public boolean isShot(int row, int col, int team) {
        _Occupant o = occupants[row][col];
        return (o instanceof _Shot) &&
               (team != 0 ? ((_Shot)o).team == team : true);
    }
    
    public boolean isBlocker(Location l) {
        return isBlocker(l.getRow(), l.getCol());
    }
    
    public boolean isBlocker(int row, int col) {
        _Occupant o = occupants[row][col];
        return (o instanceof _Blocker);
    }

    public boolean isPlayer(Location l) {
        return isPlayer(l, 0);
    }
    
    public boolean isPlayer(Location l, int team) {
        return isPlayer(l.getRow(), l.getCol(), team);
    }

    public boolean isPlayer(int row, int col) {
        return isPlayer(row, col, 0);
    }
    
    public boolean isPlayer(int row, int col, int team) {
        _Occupant o = occupants[row][col];
        return (o instanceof _Player) &&
               (team != 0 ? ((_Player)o).team == team : true);
    }
    
    public boolean isBase(Location l, int team) {
        return isBase(l.getRow(), l.getCol(), team);
    }

    public boolean isBase(int row, int col, int team) {
        _Occupant o = occupants[row][col];
        return (o instanceof _Base) &&
               (team != 0 ? ((_Base)o).team == team : true);
    }

    /**
     * Returns a list of locations of occupants facing the location l
     *
     * @param l location to check from
     * @return A list of locations that indicate where the occupants are
     */
    public List<Location> getDirectionalsFacing(Location l) {
        List<Location> locations = new ArrayList<>();
        if (!isValid(l)) {
            return locations;
        }

        for (Direction direction : Direction.values()) {
            List<_Occupant> occupantsInDirection = getOccupantsInDirectionFrom(l, direction);
            for (_Occupant occupant : occupantsInDirection) {
                if (occupant instanceof _Directional) {
                    _Directional directional = (_Directional)occupant;
                    if (directional.facingLocation(l)) {
                        locations.add(directional.location);
                    }
                }
            }
        }
        
        return locations;
    }
    
    /**
     * Returns a list of occupants in the player's line of sight
     * @return 
     * Returns a list of occupants if there is one, else returns an empty list
     */
    public List<_Occupant> getOccupantsInLOS() {
        return getOccupantsInDirection(direction());
    }

    /**
     * Returns a list of occupants from the current location in a certain direction
     * @param direction the direction to scan in
     * @return 
     * Returns a list of occupants that is in a certain direction
     */
    public List<_Occupant> getOccupantsInDirection(Direction direction) {
        return getOccupantsInDirectionFrom(location(), direction);
    }
    
    /**
     * Returns a list of occupants that stem in a certain direction from a location
     * @param location location to start search from
     * @param direction direction to search in
     * @return 
     * Returns a list of occupants
     */
    public List<_Occupant> getOccupantsInDirectionFrom(Location location, Direction direction) {        
        Location offset = direction.directionOffset();
        
        List<_Occupant> results = new ArrayList<>();
        
        Location l = location.plus(offset);
        
        while (isValid(l)) {
            _Occupant occupant = getOccupant(l);
            if (occupant != null) {
                results.add(occupant);
            }
            l.add(offset);
        }
        
        return results;
    }
    
    public boolean isValid(Location l) {
        return isValid(l.getRow(), l.getCol());
    }

    public boolean isValid(int row, int col) {
        return (row >= 0 && row < NUMROWS) &&
               (col >= 0 && col < NUMCOLS);
    }

    // 'private' classes
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
         * Returns the number of player turns it takes for a bullet to reach
         * to or beyond a location
         * @param l
         * @return 
         * Returns the number of turns. Returns -1 if it will never reach.
         */
        int turnsToReach(Location l) {
            if (!facingLocation(l)) {
                return -1;
            }
            int distance = location.distanceTo(l);
            if (firstMove) {
                distance++;
            }
            return (distance + 1) / 2;
        }
        
    }

    class _Blocker extends _Occupant {

        _Blocker(int row, int col) {
            super(new Location(row, col), 0);
        }

        _Blocker(Blocker b) {
            this(b.getRow(), b.getCol());
        }
        
    }

    class _Base extends _Occupant {

        int numHits;

        _Base(int row, int col, int team) {
            super(new Location(row, col), team);
        }

        _Base(Base b) {
            this(b.getRow(), b.getCol(), b.getTeam());
            numHits = b.getNumHits();
        }
        
    }

}

class Move {

    /**
     * Returns all possible moves given a state
     *
     * @param s state of the board
     * @return An array of valid moves
     */
    public static List<Move> validMoves(GameState s) {
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

    public enum MoveType {

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

    public Move(MoveType moveType, Direction direction) {
        this.moveType = moveType;
        this.direction = direction;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public Direction getDirection() {
        return direction;
    }

    public Location locationAfterMove(Location location, GameState state) {
        assert location != null : "Location is null";
        if (state.canMove(this) && moveType == MoveType.MOVE) {
            Location dst = location.adjLocationInDir(direction);
            return dst;
        }
        return location;
    }

    // interfacing with Direction and Action.
    public Action getAction() {
        return new Action(moveType.toString(), direction.toDegrees());
    }

    @Override
    public String toString() {
        return moveType.toString() + " in direction: " + direction.toString();
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

    public int toDegrees() {
        return this.ordinal() * 45;
    }

    public boolean isNorth() {
        int deg = toDegrees();
        return deg < 90 || deg > 270;
    }
    
    public boolean isSouth() {
        int deg = toDegrees();
        return deg > 90 && deg < 270;
    }
    
    public boolean isEast() {
        int deg = toDegrees();
        return deg > 0 && deg < 180;
    }
    
    public boolean isWest() {
        int deg = toDegrees();
        return deg > 180 && deg < 360;
    }
    
    public Location directionOffset() {
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
     * @param from
     * @param dst
     * @return 
     * Returns a boolean if from is facing dst with direction. Returns false
     * if from == dst
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
     * @param from starting position
     * @param dst ending position
     * @return 
     * Returns an approximate direction, null if dst == from
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
}

class Location {

    private int row;
    private int col;

    public Location(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public Location adjLocationInDir(Direction dir) {
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
     * @param l
     * @return 
     * Returns a rounded Pythagorean distance.
     */
    int rawDistanceTo(Location l) {
        return (int) Math.sqrt((l.getCol() - this.getCol()) * (l.getCol() - this.getCol())
                + (l.getRow() - this.getRow()) * (l.getRow() - this.getRow()));
    }
    
    /**
     * Changes the current location by an offset
     * @param offset
     * @return 
     */
    void add(Location offset) {
        this.row += offset.row;
        this.col += offset.col;
    }
    
    /**
     * Returns the sum of this and l (treats as 2 vectors) as a new Location
     * @param l
     * @return 
     * Returns a new Location equal to the sum of this and l
     */
    Location plus(Location l) {
        return new Location(this.row + l.row, this.col + l.col);
    }
    
    /**
     * Returns the direction from this to l
     * @param l
     * @return 
     * Returns a direction from this to l
     */
    Direction directionTo(Location l) {
        return Direction.getGeneralDirection(this, l);
    }
    
}

abstract class Instinct {

    /**
     * @return returns the importance of an influence can be used as a bias
     * factor
     */
    public double getWeight() {
        return 1;
    }

    /**
     * Rates a list of moves
     *
     * @param m the moves to be rated
     * @param s the state of the board
     * @return an array of scores, each corresponding to the move in m
     */
    public abstract double[] rateMoves(List<Move> m, GameState s);

}