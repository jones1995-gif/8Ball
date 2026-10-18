package br.com.poolaim.overlay;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 9001;
    private TextView permissionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionStatus();
    }

    private void buildUi() {
        int pad = dp(20);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, pad);
        content.setBackgroundColor(0xFF101820);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);

        TextView title = new TextView(this);
        title.setText("POOL AIM OVERLAY");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, 0, 0, dp(8));
        content.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Ferramenta experimental de mira manual para 8 Ball Pool");
        subtitle.setTextColor(0xFFB0BEC5);
        subtitle.setTextSize(15);
        subtitle.setPadding(0, 0, 0, dp(18));
        content.addView(subtitle, matchWrap());

        permissionStatus = new TextView(this);
        permissionStatus.setTextColor(0xFFECEFF1);
        permissionStatus.setTextSize(14);
        permissionStatus.setPadding(dp(12), dp(12), dp(12), dp(12));
        permissionStatus.setBackgroundColor(0xFF263238);
        content.addView(permissionStatus, matchWrap());

        Button permissionButton = button("1. Permitir sobreposição");
        permissionButton.setOnClickListener(v -> openOverlaySettings());
        content.addView(permissionButton, buttonParams());

        Button startButton = button("2. Iniciar mira sobre o jogo");
        startButton.setOnClickListener(v -> startOverlay());
        content.addView(startButton, buttonParams());

        Button stopButton = button("Parar sobreposição");
        stopButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AimOverlayService.class);
            intent.setAction(AimOverlayService.ACTION_STOP);
            startService(intent);
            Toast.makeText(this, "Sobreposição encerrada.", Toast.LENGTH_SHORT).show();
        });
        content.addView(stopButton, buttonParams());

        TextView howTo = new TextView(this);
        howTo.setText("COMO USAR\n"
                + "• Conceda a permissão de sobreposição e inicie a mira.\n"
                + "• No painel flutuante, toque em MESA e marque os quatro cantos internos da mesa, no sentido horário.\n"
                + "• Marque BRANCA, ALVO e CAÇAPA tocando no centro de cada item.\n"
                + "• Escolha Direta, 1 Tabela ou 2 Tabelas.\n"
                + "• Use EDITAR/JOGAR para alternar entre posicionar pontos e deixar os toques passarem ao jogo.\n\n"
                + "LIMITAÇÕES DA VERSÃO 0.1\n"
                + "As posições são marcadas manualmente. As tabelas são aproximadas por um retângulo alinhado à tela. "
                + "O aplicativo ainda não reconhece as bolas automaticamente e não estima força, efeito ou ponto exato de parada.");
        howTo.setTextColor(0xFFD7E0E5);
        howTo.setTextSize(14);
        howTo.setPadding(0, dp(22), 0, dp(14));
        content.addView(howTo, matchWrap());

        TextView safety = new TextView(this);
        safety.setText("A sobreposição pode interceptar toques durante a edição. Desative a edição antes de jogar. "
                + "Use por sua conta e confira as regras do jogo antes de utilizar ferramentas externas.");
        safety.setTextColor(0xFFFFCC80);
        safety.setTextSize(12);
        content.addView(safety, matchWrap());

        setContentView(scroll);
    }

    private void refreshPermissionStatus() {
        if (permissionStatus == null) return;
        boolean granted = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
        permissionStatus.setText(granted
                ? "Permissão de sobreposição: CONCEDIDA"
                : "Permissão de sobreposição: ainda não concedida");
        permissionStatus.setTextColor(granted ? 0xFF80CBC4 : 0xFFFFCC80);
    }

    private void openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "A permissão já está concedida.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startOverlay() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Conceda primeiro a permissão de sobreposição.", Toast.LENGTH_LONG).show();
            openOverlaySettings();
            return;
        }
        Intent intent = new Intent(this, AimOverlayService.class);
        intent.setAction(AimOverlayService.ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
        else startService(intent);
        Toast.makeText(this, "Painel de mira iniciado.", Toast.LENGTH_SHORT).show();
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(8);
        return p;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
