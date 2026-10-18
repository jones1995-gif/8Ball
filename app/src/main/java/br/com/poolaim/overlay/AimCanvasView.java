package br.com.poolaim.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.DashPathEffect;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

final class AimCanvasView extends View {
    interface TapListener { void onCanvasTap(float x, float y); }

    private final AimModel model;
    private final TapListener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    AimCanvasView(Context context, AimModel model, TapListener listener) {
        super(context);
        this.model = model;
        this.listener = listener;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && model.editing) {
            listener.onCanvasTap(event.getX(), event.getY());
            performClick();
            return true;
        }
        return model.editing;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTable(canvas);
        drawTrajectories(canvas);
        drawMarkers(canvas);
    }

    private void drawTable(Canvas canvas) {
        if (model.tableCorners.isEmpty()) return;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(0xAA80CBC4);
        Path path = new Path();
        PointF first = model.tableCorners.get(0);
        path.moveTo(first.x, first.y);
        for (int i = 1; i < model.tableCorners.size(); i++) {
            PointF p = model.tableCorners.get(i);
            path.lineTo(p.x, p.y);
        }
        if (model.tableCorners.size() == 4) path.close();
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(12));
        for (int i = 0; i < model.tableCorners.size(); i++) {
            PointF p = model.tableCorners.get(i);
            canvas.drawCircle(p.x, p.y, dp(4), paint);
            canvas.drawText("C" + (i + 1), p.x + dp(5), p.y - dp(5), paint);
        }
    }

    private void drawTrajectories(Canvas canvas) {
        if (model.tableCorners.size() != 4 || model.targetBall == null || model.pocket == null) return;
        Geometry.Rect table = getTableRect();
        if (table == null || !table.isValid()) return;
        Geometry.Point target = point(model.targetBall);
        Geometry.Point pocket = point(model.pocket);
        List<List<Geometry.Point>> paths = Geometry.calculatePaths(target, pocket, table, model.bankCount);
        if (paths.isEmpty()) return;

        List<Geometry.Point> best = paths.get(0);
        double bestLength = Geometry.pathLength(best);
        for (List<Geometry.Point> path : paths) {
            double length = Geometry.pathLength(path);
            if (length < bestLength) {
                best = path;
                bestLength = length;
            }
        }

        if (model.bankCount > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.4f));
            paint.setColor(0x554FC3F7);
            paint.setPathEffect(new DashPathEffect(new float[]{dp(6), dp(5)}, 0));
            canvas.drawLine(model.targetBall.x, model.targetBall.y, model.pocket.x, model.pocket.y, paint);
            paint.setPathEffect(null);
            for (List<Geometry.Point> path : paths) {
                if (path == best) continue;
                paint.setColor(0x77FFB74D);
                paint.setStrokeWidth(dp(1.8f));
                drawGeometryPath(canvas, path);
            }
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(model.bankCount == 0 ? 2.6f : 3.2f));
        paint.setColor(0xFFFFB74D);
        paint.setPathEffect(null);
        drawGeometryPath(canvas, best);

        if (model.cueBall != null) {
            Geometry.Point next = best.size() > 1 ? best.get(1) : pocket;
            double ballDiameter = Math.min(table.width(), table.height()) / 14.0;
            Geometry.Point ghost = Geometry.ghostBallCenter(target, next, ballDiameter);
            paint.setColor(0xEEFFF176);
            paint.setStrokeWidth(dp(2));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(model.cueBall.x, model.cueBall.y, (float) ghost.x, (float) ghost.y, paint);
            paint.setColor(0xAAFFF176);
            canvas.drawCircle((float) ghost.x, (float) ghost.y, (float) (ballDiameter / 2.0), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(12));
            canvas.drawText("MIRA", (float) ghost.x + dp(7), (float) ghost.y - dp(7), paint);
        }
    }

    private void drawGeometryPath(Canvas canvas, List<Geometry.Point> points) {
        if (points.size() < 2) return;
        Path path = new Path();
        path.moveTo((float) points.get(0).x, (float) points.get(0).y);
        for (int i = 1; i < points.size(); i++) {
            path.lineTo((float) points.get(i).x, (float) points.get(i).y);
        }
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 1; i < points.size() - 1; i++) {
            Geometry.Point p = points.get(i);
            canvas.drawCircle((float) p.x, (float) p.y, dp(3), paint);
        }
        paint.setStyle(Paint.Style.STROKE);
    }

    private void drawMarkers(Canvas canvas) {
        if (model.cueBall != null) drawBall(canvas, model.cueBall, Color.WHITE, "BRANCA");
        if (model.targetBall != null) drawBall(canvas, model.targetBall, 0xFFFFEB3B, "ALVO");
        if (model.pocket != null) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(0xFFE1BEE7);
            canvas.drawCircle(model.pocket.x, model.pocket.y, dp(9), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(12));
            canvas.drawText("CAÇAPA", model.pocket.x + dp(10), model.pocket.y + dp(4), paint);
        }
    }

    private void drawBall(Canvas canvas, PointF point, int color, String label) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawCircle(point.x, point.y, dp(7), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(0xFF263238);
        canvas.drawCircle(point.x, point.y, dp(7), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(dp(12));
        canvas.drawText(label, point.x + dp(10), point.y - dp(8), paint);
    }

    private Geometry.Rect getTableRect() {
        if (model.tableCorners.size() != 4) return null;
        float left = Float.MAX_VALUE, top = Float.MAX_VALUE;
        float right = -Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
        for (PointF p : model.tableCorners) {
            left = Math.min(left, p.x);
            top = Math.min(top, p.y);
            right = Math.max(right, p.x);
            bottom = Math.max(bottom, p.y);
        }
        return new Geometry.Rect(left, top, right, bottom);
    }

    private Geometry.Point point(PointF p) { return new Geometry.Point(p.x, p.y); }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

    void refresh() { invalidate(); }
}
