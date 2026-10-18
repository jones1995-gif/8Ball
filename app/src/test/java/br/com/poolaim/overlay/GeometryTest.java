package br.com.poolaim.overlay;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class GeometryTest {
    private final Geometry.Rect table = new Geometry.Rect(0, 0, 100, 50);

    @Test
    public void directPathHasStartAndEnd() {
        List<List<Geometry.Point>> paths = Geometry.calculatePaths(
                new Geometry.Point(20, 20), new Geometry.Point(80, 30), table, 0);
        assertEquals(1, paths.size());
        assertEquals(2, paths.get(0).size());
        assertEquals(20.0, paths.get(0).get(0).x, 0.001);
        assertEquals(80.0, paths.get(0).get(1).x, 0.001);
    }

    @Test
    public void oneBankProducesFourCandidates() {
        List<List<Geometry.Point>> paths = Geometry.calculatePaths(
                new Geometry.Point(20, 20), new Geometry.Point(80, 30), table, 1);
        assertEquals(4, paths.size());
        for (List<Geometry.Point> path : paths) {
            assertTrue(path.size() >= 3);
            for (Geometry.Point p : path) {
                assertTrue(p.x >= -0.01 && p.x <= 100.01);
                assertTrue(p.y >= -0.01 && p.y <= 50.01);
            }
        }
    }

    @Test
    public void twoBanksProducesEightCandidates() {
        List<List<Geometry.Point>> paths = Geometry.calculatePaths(
                new Geometry.Point(20, 20), new Geometry.Point(80, 30), table, 2);
        assertEquals(8, paths.size());
        for (List<Geometry.Point> path : paths) assertTrue(path.size() >= 4);
    }

    @Test
    public void ghostBallCenterIsOneDiameterBehindTarget() {
        Geometry.Point ghost = Geometry.ghostBallCenter(
                new Geometry.Point(50, 25), new Geometry.Point(100, 25), 10);
        assertEquals(40.0, ghost.x, 0.001);
        assertEquals(25.0, ghost.y, 0.001);
    }
}
