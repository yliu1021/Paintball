package brains.SkynetHelperClasses.GameInterfaces;

/**
 * Basic Location class
 */
public class Location {

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
        int d = dir.ordinal();
        int r = 0;
        int c = 0;

        if (d > Direction.NORTH.ordinal() && d < Direction.SOUTH.ordinal()) {
            r = 1;
        } else if (d > Direction.SOUTH.ordinal()) {
            r = -1;
        }

        if (d > Direction.EAST.ordinal() && d < Direction.WEST.ordinal()) {
            c = 1;
        } else if (d < Direction.EAST.ordinal() || d > Direction.WEST.ordinal()) {
            c = -1;
        }

        return new Location(row + r, col + c);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other.getClass().equals(this.getClass())) {
            Location o = (Location)other;
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
}
