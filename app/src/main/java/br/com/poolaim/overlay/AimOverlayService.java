package br.com.poolaim.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AimOverlayService extends Service implements AimCanvasView.TapListener {
    public static final String ACTION_START = "br.com.poolaim.overlay.START";
    public static final String ACTION_STOP = "br.com.poolaim.overlay.STOP";
    private static final String CHANNEL_ID = "pool_aim_overlay";
    private static final int NOTIFICATION_ID = 3175;

    private WindowManager windowManager;
    private AimModel model;
    private AimCanvasView canvasView;
    private LinearLayout panel;
    private TextView statusView;
    private WindowManager.LayoutParams canvasParams;
    private WindowManager.LayoutParams panelParams;
    private boolean viewsAdded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        model = new AimModel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startOngoingNotification();
        if (!viewsAdded) addOverlayViews();
        return START_STICKY;
    }

    private void startOngoingNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Pool Aim Overlay",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Notificação enquanto a sobreposição de mira está ativa");
            manager.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle("Pool Aim Overlay ativo")
                .setContentText("Mira manual e projeção geométrica na tela")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void addOverlayViews() {
        canvasView = new AimCanvasView(this, model, this);
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        if (!model.editing) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        canvasParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT);
        canvasParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(canvasView, canvasParams);

        panel = buildPanel();
        panelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        panelParams.y = dp(36);
        windowManager.addView(panel, panelParams);
        viewsAdded = true;
        refreshUi();
    }

    private LinearLayout buildPanel() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(5), dp(4), dp(5), dp(5));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE619252C);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0xAA80CBC4);
        root.setBackground(bg);

        statusView = new TextView(this);
        statusView.setTextColor(0xFFFFFFFF);
        statusView.setTextSize(11);
        statusView.setPadding(dp(4), dp(2), dp(4), dp(4));
        root.addView(statusView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(row1, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        addButton(row1, "EDITAR / JOGAR", this::toggleEditing);
        addButton(row1, "MESA", () -> selectMode(AimModel.MODE_TABLE));
        addButton(row1, "BRANCA", () -> selectMode(AimModel.MODE_CUE));
        addButton(row1, "ALVO", () -> selectMode(AimModel.MODE_TARGET));
        addButton(row1, "CAÇAPA", () -> selectMode(AimModel.MODE_POCKET));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(row2, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        addButton(row2, "DIRETA", () -> setBanks(0));
        addButton(row2, "1 TABELA", () -> setBanks(1));
        addButton(row2, "2 TABELAS", () -> setBanks(2));
        addButton(row2, "LIMPAR", this::clearAll);
        addButton(row2, "FECHAR", this::stopSelf);
        return root;
    }

    private void addButton(LinearLayout row, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(9);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(34));
        button.setMinimumHeight(dp(34));
        button.setPadding(dp(2), 0, dp(2), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        row.addView(button, params);
        button.setOnClickListener(v -> action.run());
    }

    private void selectMode(String mode) {
        model.mode = mode;
        setEditing(true);
        if (AimModel.MODE_TABLE.equals(mode)) {
            model.tableCorners.clear();
            model.status = "MESA: toque os cantos 1/4, 2/4, 3/4 e 4/4 em sentido horário.";
        } else if (AimModel.MODE_CUE.equals(mode)) {
            model.status = "BRANCA: toque no centro da bola branca.";
        } else if (AimModel.MODE_TARGET.equals(mode)) {
            model.status = "ALVO: toque no centro da bola que deseja encaçapar.";
        } else if (AimModel.MODE_POCKET.equals(mode)) {
            model.status = "CAÇAPA: toque no centro da caçapa desejada.";
        }
        refreshUi();
    }

    private void setBanks(int count) {
        model.bankCount = count;
        model.status = count == 0 ? "Trajetória direta selecionada."
                : count == 1 ? "Mostrando opções com 1 tabela."
                : "Mostrando opções com 2 tabelas.";
        refreshUi();
    }

    private void toggleEditing() {
        setEditing(!model.editing);
        model.mode = AimModel.MODE_NONE;
        model.status = model.editing
                ? "EDIÇÃO ativa: escolha MESA, BRANCA, ALVO ou CAÇAPA."
                : "JOGAR ativo: os toques passam ao jogo; use o painel para reativar edição.";
        refreshUi();
    }

    private void setEditing(boolean editing) {
        model.editing = editing;
        if (canvasView != null && canvasParams != null) {
            int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            if (!editing) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            canvasParams.flags = flags;
            try { windowManager.updateViewLayout(canvasView, canvasParams); } catch (Exception ignored) {}
        }
    }

    private void clearAll() {
        model.tableCorners.clear();
        model.cueBall = null;
        model.targetBall = null;
        model.pocket = null;
        model.bankCount = 0;
        model.mode = AimModel.MODE_NONE;
        setEditing(true);
        model.status = "Tudo limpo. Toque em MESA para calibrar novamente.";
        refreshUi();
    }

    @Override
    public void onCanvasTap(float x, float y) {
        PointF point = new PointF(x, y);
        if (AimModel.MODE_TABLE.equals(model.mode)) {
            model.tableCorners.add(point);
            if (model.tableCorners.size() >= 4) {
                model.mode = AimModel.MODE_NONE;
                model.status = "Mesa marcada. Agora escolha BRANCA, ALVO e CAÇAPA.";
            } else {
                model.status = "MESA: canto " + (model.tableCorners.size() + 1) + "/4. Siga em sentido horário.";
            }
        } else if (AimModel.MODE_CUE.equals(model.mode)) {
            model.cueBall = point;
            model.mode = AimModel.MODE_NONE;
            model.status = "Bola branca marcada. Agora escolha ALVO e CAÇAPA.";
        } else if (AimModel.MODE_TARGET.equals(model.mode)) {
            model.targetBall = point;
            model.mode = AimModel.MODE_NONE;
            model.status = "Bola-alvo marcada. Escolha CAÇAPA para projetar a trajetória.";
        } else if (AimModel.MODE_POCKET.equals(model.mode)) {
            model.pocket = point;
            model.mode = AimModel.MODE_NONE;
            model.status = "Caçapa marcada. Escolha Direta, 1 Tabela ou 2 Tabelas.";
        } else {
            model.status = "Escolha primeiro MESA, BRANCA, ALVO ou CAÇAPA no painel.";
        }
        refreshUi();
    }

    private void refreshUi() {
        if (statusView != null) {
            String edit = model.editing ? "EDITANDO" : "JOGANDO";
            statusView.setText(edit + " • " + model.status);
        }
        if (canvasView != null) canvasView.refresh();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onDestroy() {
        if (windowManager != null) {
            if (panel != null) {
                try { windowManager.removeView(panel); } catch (Exception ignored) {}
            }
            if (canvasView != null) {
                try { windowManager.removeView(canvasView); } catch (Exception ignored) {}
            }
        }
        viewsAdded = false;
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE);
        else stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
