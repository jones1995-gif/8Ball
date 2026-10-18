package br.com.poolaim.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Pure-Java table geometry helpers; independent from Android UI classes. */
public final class Geometry {
    private Geometry() {}

    public static final class Point {
        public final double x;
        public final double y;
        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    public static final class Rect {
        public final double left;
        public final double top;
        public final double right;
        public final double bottom;
        public Rect(double left, double top, double right, double bottom) {
            this.left = Math.min(left, right);
            this.right = Math.max(left, right);
            this.top = Math.min(top, bottom);
            this.bottom = Math.max(top, bottom);
        }
        public double width() { return right - left; }
        public double height() { return bottom - top; }
        public boolean isValid() { return width() > 1.0 && height() > 1.0; }
    }

    private static final class Event {
        final double t;
        Event(double t) { this.t = t; }
    }

    public static List<List<Point>> calculatePaths(Point start, Point end, Rect table, int bankCount) {
        List<List<Point>> paths = new ArrayList<>();
        if (start == null || end == null || table == null || !table.isValid()) return paths;
        if (bankCount <= 0) {
            List<Point> direct = new ArrayList<>();
            direct.add(start);
            direct.add(end);
            paths.add(direct);
            return paths;
        }
        if (bankCount > 2) return paths;

        for (int tileX = -bankCount; tileX <= bankCount; tileX++) {
            for (int tileY = -bankCount; tileY <= bankCount; tileY++) {
                if (Math.abs(tileX) + Math.abs(tileY) != bankCount) continue;
                Point image = imageInTile(end, table, tileX, tileY);
                List<Point> path = unfoldAndFold(start, image, end, table);
                if (path.size() >= bankCount + 2) paths.add(path);
            }
        }
        return paths;
    }

    public static Point ghostBallCenter(Point target, Point nextPathPoint, double ballDiameter) {
        if (target == null || nextPathPoint == null) return null;
        double dx = nextPathPoint.x - target.x;
        double dy = nextPathPoint.y - target.y;
        double length = Math.hypot(dx, dy);
        if (length < 0.001) return new Point(target.x, target.y);
        double diameter = Math.max(0, ballDiameter);
        return new Point(target.x - dx / length * diameter, target.y - dy / length * diameter);
    }

    public static double pathLength(List<Point> path) {
        double total = 0;
        for (int i = 1; i < path.size(); i++) {
            total += Math.hypot(path.get(i).x - path.get(i - 1).x,
                    path.get(i).y - path.get(i - 1).y);
        }
        return total;
    }

    private static Point imageInTile(Point point, Rect r, int tileX, int tileY) {
        double w = r.width();
        double h = r.height();
        double localX = point.x - r.left;
        double localY = point.y - r.top;
        double x = r.left + tileX * w + (tileX % 2 == 0 ? localX : w - localX);
        double y = r.top + tileY * h + (tileY % 2 == 0 ? localY : h - localY);
        return new Point(x, y);
    }

    private static List<Point> unfoldAndFold(Point start, Point image, Point actualEnd, Rect r) {
        List<Event> events = new ArrayList<>();
        double dx = image.x - start.x;
        double dy = image.y - start.y;
        double w = r.width();
        double h = r.height();

        if (Math.abs(dx) > 1e-8) {
            double minX = Math.min(start.x, image.x);
            double maxX = Math.max(start.x, image.x);
            int first = (int) Math.floor((minX - r.left) / w) + 1;
            int last = (int) Math.ceil((maxX - r.left) / w) - 1;
            for (int k = first; k <= last; k++) {
                double boundary = r.left + k * w;
                double t = (boundary - start.x) / dx;
                if (t > 1e-7 && t < 1 - 1e-7) events.add(new Event(t));
            }
        }
        if (Math.abs(dy) > 1e-8) {
            double minY = Math.min(start.y, image.y);
            double maxY = Math.max(start.y, image.y);
            int first = (int) Math.floor((minY - r.top) / h) + 1;
            int last = (int) Math.ceil((maxY - r.top) / h) - 1;
            for (int k = first; k <= last; k++) {
                double boundary = r.top + k * h;
                double t = (boundary - start.y) / dy;
                if (t > 1e-7 && t < 1 - 1e-7) events.add(new Event(t));
            }
        }

        Collections.sort(events, Comparator.comparingDouble(e -> e.t));
        List<Point> path = new ArrayList<>();
        path.add(start);
        for (Event event : events) {
            double ux = start.x + dx * event.t;
            double uy = start.y + dy * event.t;
            Point folded = new Point(fold(ux, r.left, r.right), fold(uy, r.top, r.bottom));
            addDistinct(path, folded);
        }
        addDistinct(path, actualEnd);
        return path;
    }

    private static double fold(double value, double min, double max) {
        double size = max - min;
        if (size <= 0) return min;
        double q = (value - min) / size;
        long tile = (long) Math.floor(q);
        double local = (q - tile) * size;
        return Math.floorMod(tile, 2L) == 0L ? min + local : max - local;
    }

    private static void addDistinct(List<Point> points, Point point) {
        if (points.isEmpty()) {
            points.add(point);
            return;
        }
        Point previous = points.get(points.size() - 1);
        if (Math.hypot(previous.x - point.x, previous.y - point.y) > 0.75) points.add(point);
    }
}
