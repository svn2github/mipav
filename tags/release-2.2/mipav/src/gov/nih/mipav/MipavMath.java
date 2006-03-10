package gov.nih.mipav;


import gov.nih.mipav.model.structures.Point3Df;


/**
 * Math functions not found in Java's Math class or they are slow.
 * 
 * @author not attributable
 * @version 1.0
 */
public class MipavMath {
    /**
     * Round the value of type float to the closest integer. Java's Math.round is incredibly slow.
     * @param a float the value to be rounded.
     * @return int Returns the closest integer.
     */
    public final static int round(final float a) {
        if (a < 0) {
            return ((int) (a - 0.5f));
        } else
            return ((int) (a + 0.5f));
    }

    /**
     * Round the value of type double to the closest integer. Java's Math.round is incredibly slow.
     * @param a float the value to be rounded.
     * @return int Returns the closest integer.
     */
    public final static int round(final double a) {
        if (a < 0) {
            return ((int) (a - 0.5d));
        } else
            return ((int) (a + 0.5d));
    }

    /**
     * Calculates the 2D euclidian distance between two points
     * @param x1 first x coordinate
     * @param x2 second x coordinate
     * @param y1 first y coordinate
     * @param y2 second y coordinate
     * @return returns the distance
     */
    public final static double distance(int x1, int x2, int y1, int y2) {
        return Math.sqrt( (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    /**
     * Finds the distance between two points.
     * @param x1 x coordinate of the first point
     * @param x2 x coordinate of the second point
     * @param y1 y coordinate of the first point
     * @param y2 y coordinate of the second point
     * @return the distance as a double
     */
    public static final double distance(float x1, float x2, float y1, float y2) {
        return Math.sqrt( (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    /**
     * Finds the distance between two points.
     * @param x1 x coordinate of the first point
     * @param x2 x coordinate of the second point
     * @param y1 y coordinate of the first point
     * @param y2 y coordinate of the second point
     * @return the distance as a double
     */
    public static final double distance(double x1, double x2, double y1, double y2) {
        return Math.sqrt( (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    /**
     * Calculates the 3D euclidian distance between two points
     * @param pt1 first point
     * @param pt2 second point
     * @return returns the distance
     */
    public final static double distance(Point3Df pt1, Point3Df pt2) {
        return Math.sqrt( (pt2.x - pt1.x) * (pt2.x - pt1.x) + (pt2.y - pt1.y) * (pt2.y - pt1.y) + (pt2.z - pt1.z)
                * (pt2.z - pt1.z));
    }

    /**
     * Returns the real length (adjusted by the image resolution) of the line between (x[0], y[0]) and (x[1], y[1]).
     * @param x array of the x coordinates
     * @param y array of the y coordinates
     * @param res resolutions in each dimension
     * @return returns the length
     */
    public static final double length(float x[], float y[], float res[]) {
        double length;
        // length is (x1-x0) squared times the x resolution
        // plus (y1-y0) squared times the y resolution
        length = Math.sqrt( ( (x[1] - x[0]) * (x[1] - x[0]) * (res[0]) * (res[0]))
                + ( (y[1] - y[0]) * (y[1] - y[0]) * (res[1]) * (res[1])));
        return length;
    }

    /**
     * Returns the real length (adjusted by the image resolution) of the line between (x0, y0) and (x1, y1).
     * @param x0 x coordinate of the first point
     * @param y0 x coordinate of the first point
     * @param x1 y coordinate of the second point
     * @param y1 y coordinate of the second point
     * @param res resolutions in each dimension
     * @return returns the length
     */
    public static final double length(float x0, float y0, float x1, float y1, float res[]) {
        double length;
        // length is (x1-x0) squared times the x resolution
        // plus (y1-y0) squared times the y resolution
        length = Math.sqrt( ( (x1 - x0) * (x1 - x0) * (res[0]) * (res[0]))
                + ( (y1 - y0) * (y1 - y0) * (res[1]) * (res[1])));
        return length;
    }

    /**
     * Returns the real length (adjusted by the image resolution) of the line between (x[0], y[0]) and (x[1], y[1]).
     * @param x array of the x coordinates
     * @param y array of the y coordinates
     * @param res resolutions in each dimension
     * @return returns the length
     */
    public static final double length(double x[], double y[], float res[]) {
        double length;
        // length is (x1-x0) squared times the x resolution
        // plus (y1-y0) squared times the y resolution
        length = Math.sqrt( ( (x[1] - x[0]) * (x[1] - x[0]) * (res[0]) * (res[0]))
                + ( (y[1] - y[0]) * (y[1] - y[0]) * (res[1]) * (res[1])));
        return length;
    }

    /**
     * Returns the real length (adjusted by the image resolution) of the line between (x0, y0) and (x1, y1).
     * @param x0 x coordinate of the first point
     * @param y0 x coordinate of the first point
     * @param x1 y coordinate of the second point
     * @param y1 y coordinate of the second point
     * @param res resolutions in each dimension
     * @return returns the length
     */
    public static final double length(double x0, double y0, double x1, double y1, float res[]) {
        double length;
        // length is (x1-x0) squared times the x resolution
        // plus (y1-y0) squared times the y resolution
        length = Math.sqrt( ( (x1 - x0) * (x1 - x0) * (res[0]) * (res[0]))
                + ( (y1 - y0) * (y1 - y0) * (res[1]) * (res[1])));
        return length;
    }
}
