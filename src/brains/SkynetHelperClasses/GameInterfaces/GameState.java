package brains.SkynetHelperClasses.GameInterfaces;

import arena.Board;

import arena.Occupant;
import arena.Player;
import arena.Shot;
import arena.Base;
import arena.Blocker;

/**
 * Represents an immutable game state from the perspective of the
 * current player.
 */
public class GameState   {
    
    private static int NUMCOLS;
    private static int NUMROWS;
    
    private _Occupant[][] occupants;
    
    private Location playerLocation;
    
    public GameState(Player p, Board b) {
        NUMCOLS = b.numCols();
        NUMROWS = b.numRows();
        
        playerLocation = new Location(p.getRow(), p.getCol());
        
        occupants = new _Occupant[NUMROWS][NUMCOLS];
        
        for (int r = 0; r < NUMROWS; r++) {
            for (int c = 0; c < NUMCOLS; c++) {
                Occupant o = b.get(r, c);
                _Occupant o2 = null;
                if (o instanceof Player) {
                    o2 = new _Player((Player)o);
                } else if (o instanceof Shot) {
                    o2 = new _Shot((Shot)o);
                } else if (o instanceof Base) {
                    o2 = new _Base((Base)o);
                } else if (o instanceof Blocker) {
                    o2 = new _Blocker((Blocker)o);
                }
                occupants[r][c] = o2;
            }
        }
    }
    
    public boolean canMove(Direction direction) {
        Location dest = playerLocation.adjLocationInDir(direction);
        int r = dest.getRow();
        int c = dest.getCol();
        return r >= 0 && r < NUMROWS &&
               c >= 0 && c < NUMCOLS &&
               occupants[r][c] == null;
    }

    public boolean canShoot() {
        return turnsUntillShoot() == 0;
    }
    
    public int turnsUntillShoot() {
        _Player p = (_Player)occupants[playerLocation.getRow()][playerLocation.getCol()];
        return p.turnsUntilShoot;
    }
    
    // private classes
    private abstract class _Occupant {
        Location location;
        
        _Occupant(Location location) {
            this.location = location;
        }
    }
    
    private abstract class _Directional extends _Occupant {
        Direction direction;

        _Directional(Location location, Direction direction) {
            super(location);
            this.direction = direction;
        }
        
        boolean facingLocation(Location l) {
            int dX = l.getRow() - this.location.getRow();
            int dY = l.getCol() - this.location.getCol();
            if (dX == 0) {
                if (dY < 0) {
                    return direction == Direction.WEST;
                } else {
                    return direction == Direction.EAST;
                }
            }
            if (dY == 0) {
                if (dX < 0) {
                    return direction == Direction.NORTH;
                } else {
                    return direction == Direction.SOUTH;
                }
            }
            
            if (Math.abs(dX) != Math.abs(dY)) {
                return false;
            } else {
                if (dX < 0) {
                    if (dY < 0) {
                        return direction == Direction.NORTHWEST;
                    } else {
                        return direction == Direction.NORTHEAST;
                    }
                } else {
                    if (dY < 0) {
                        return direction == Direction.SOUTHWEST;
                    } else {
                        return direction == Direction.SOUTHEAST;
                    }
                }
            }
        }
    }
    
    private class _Player extends _Directional {
        int turnsUntilShoot;
        int kills;
        int frags;
        int deaths;
        int enemyBaseHits;
        int selfBaseHits;
        int score;
        
        _Player(int row, int col, Direction direction) {
            super(new Location(row, col), direction);
        }
        
        _Player(Player p) {
            this(p.getRow(), p.getCol(), Direction.fromDegrees(p.getDirection()));
            turnsUntilShoot = p.getTurnsUntilShoot();
            kills = p.getKills();
            frags = p.getFrags();
            deaths = p.getDeaths();
            enemyBaseHits = p.getEnemyBaseHits();
            selfBaseHits = p.getSelfBaseHits();
            score = p.getScore();
        }
    }
    
    private class _Shot extends _Directional {
        _Player owner;
        boolean firstMove;
       
        _Shot(int row, int col, Direction direction) {
            super(new Location(row, col), direction);
        }
        
        _Shot(Shot s) {
            this(s.getRow(), s.getCol(), Direction.fromDegrees(s.getDirection()));
            owner = new _Player(s.getOwner());
            // if shot is right next to its owner
            firstMove = Math.abs(s.getRow() - owner.location.getRow()) == 1 && Math.abs(s.getCol() - owner.location.getCol()) == 1;
        }
        
        Location locationAfterTurn(int turns) {
            return null;
        }
    }
    
    private class _Blocker extends _Occupant {
        _Blocker(int row, int col) {
            super(new Location(row, col));
        }
        
        _Blocker(Blocker b) {
            this(b.getRow(), b.getCol());
        }
    }
    
    private class _Base extends _Occupant {
        int numHits;
        
        _Base(int row, int col) {
            super(new Location(row, col));
        }
        
        _Base(Base b) {
            this(b.getRow(), b.getCol());
            numHits = b.getNumHits();
        }
    }
    
}
