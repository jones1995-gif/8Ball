package br.com.poolaim.overlay;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;

final class AimModel {
    static final String MODE_NONE = "NONE";
    static final String MODE_TABLE = "TABLE";
    static final String MODE_CUE = "CUE";
    static final String MODE_TARGET = "TARGET";
    static final String MODE_POCKET = "POCKET";

    final List<PointF> tableCorners = new ArrayList<>();
    PointF cueBall;
    PointF targetBall;
    PointF pocket;
    int bankCount = 0;
    boolean editing = true;
    String mode = MODE_NONE;
    String status = "Toque em MESA para marcar os 4 cantos.";
}
