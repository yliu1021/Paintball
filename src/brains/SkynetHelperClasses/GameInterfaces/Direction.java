package brains.SkynetHelperClasses.GameInterfaces;

/**
 * Basic direction enum
 */
public enum Direction {
    NORTH,
    NORTHEAST,
    EAST,
    SOUTHEAST,
    SOUTH,
    SOUTHWEST,
    WEST,
    NORTHWEST;

    public static Direction fromDegrees(int degrees) {
        degrees %= 360;
        if (degrees < 360) {
            degrees += 360;
        }
        return Direction.values()[degrees / 45];
    }
    
    public int toDegrees() {
        return this.ordinal() * 45;
    }

    @Override
    public String toString() {
        String r = "";
        switch (this) {
            case NORTH:
                r = "N";
                break;
            case NORTHEAST:
                r = "NE";
                break;
            case EAST:
                r = "E";
                break;
            case SOUTHEAST:
                r = "SE";
                break;
            case SOUTH:
                r = "S";
                break;
            case SOUTHWEST:
                r = "SW";
                break;
            case WEST:
                r = "W";
                break;
            case NORTHWEST:
                r = "NW";
                break;
        }
        return r;
    }
}
