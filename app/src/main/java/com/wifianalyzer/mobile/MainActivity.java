package com.wifianalyzer.mobile;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends Activity {
    private WifiManager wifi;
    private LinearLayout results;
    private TextView status;
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1); root.setPadding(dp(20),dp(30),dp(20),dp(20));
        root.setBackgroundColor(Color.rgb(13,20,33));
        TextView title = new TextView(this);
        title.setText("WiFi Analyzer"); title.setTextSize(28); title.setTextColor(Color.WHITE);
        root.addView(title);
        status = new TextView(this); status.setText("Scanner de redes próximas"); status.setTextColor(0xffaabbd0); status.setPadding(0,dp(12),0,dp(16)); root.addView(status);
        Button scan = new Button(this); scan.setText("ESCANEAR REDES"); root.addView(scan);
        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this); results.setOrientation(1); scroll.addView(results);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        scan.setOnClickListener(v -> scan());
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grants) {
        super.onRequestPermissionsResult(requestCode,permissions,grants);
        if (requestCode == 101 && grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED) scan();
    }
    private void scan() {
        results.removeAllViews();
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            status.setText("Permita o acesso à localização para listar redes."); return;
        }
        LocationManager lm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            status.setText("Ative a localização do celular para escanear."); return;
        }
        try {
            boolean requested=wifi.startScan();
            List<ScanResult> list=wifi.getScanResults();
            Collections.sort(list,(a,b)->Integer.compare(b.level,a.level));
            status.setText(list.size()+" redes encontradas"+(requested?"":" • exibindo último scan disponível"));
            for (ScanResult r:list) {
                TextView item=new TextView(this);
                String name=r.SSID == null || r.SSID.isEmpty()?"(Rede oculta)":r.SSID;
                String security=r.capabilities.contains("SAE")?"WPA3":r.capabilities.contains("WPA2")?"WPA2":r.capabilities.contains("WPA")?"WPA":r.capabilities.contains("WEP")?"WEP":r.capabilities.contains("ESS")?"Aberta":"Desconhecida";
                item.setText(name+"\n"+r.level+" dBm  •  "+r.frequency+" MHz  •  "+security);
                item.setTextColor(Color.WHITE); item.setTextSize(16); item.setPadding(dp(12),dp(16),dp(12),dp(16));
                results.addView(item);
                View line=new View(this);line.setBackgroundColor(0xff28364c);results.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
            }
            if(list.isEmpty()) status.setText("Nenhuma rede disponível. Aguarde e tente novamente.");
        } catch(SecurityException ex) { status.setText("Permissão de Wi-Fi/localização necessária."); }
    }
}
