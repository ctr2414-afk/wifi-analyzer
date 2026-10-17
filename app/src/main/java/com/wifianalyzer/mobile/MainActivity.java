package com.wifianalyzer.mobile;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private final int BG=0xff0b1220, PANEL=0xff18243a, WHITE=0xfff1f5ff, MUTED=0xffaabbd0, ACCENT=0xff38bdf8, GREEN=0xff4ade80, YELLOW=0xfffbbf24;
    private WifiManager wifi;
    private LinearLayout content;
    private TextView banner;
    private List<ScanResult> networks = new ArrayList<>();
    private int currentTab=0;
    private int d(float n){return (int)(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(d(radius));return b;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private LinearLayout vertical(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private void addGap(LinearLayout l,int height){
        View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,d(height)));
    }
    private LinearLayout card(LinearLayout parent){
        LinearLayout c=vertical();c.setPadding(d(16),d(14),d(16),d(14));c.setBackground(shape(PANEL,16));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=d(12);parent.addView(c,p);return c;
    }
    private void line(LinearLayout c,String a,String b){
        c.addView(text(a,13,MUTED,false));c.addView(text(b,16,WHITE,true));addGap(c,9);
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        wifi=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        LinearLayout root=vertical();root.setPadding(d(16),d(22),d(16),d(12));root.setBackgroundColor(BG);
        TextView heading=text("WiFi Analyzer",28,WHITE,true);root.addView(heading);
        TextView subtitle=text("Scanner e diagnóstico de segurança",13,MUTED,false);root.addView(subtitle);addGap(root,14);
        banner=text("Toque em Atualizar para analisar redes próximas",13,ACCENT,false);root.addView(banner);addGap(root,12);
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);
        String[] names={"Redes","Conexão","Análise"};
        for(int i=0;i<names.length;i++){
            final int n=i;Button b=new Button(this);b.setText(names[i]);b.setTextSize(12);b.setAllCaps(false);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,d(52),1);tabs.addView(b,bp);
            b.setOnClickListener(v->{currentTab=n;render();});
        }
        root.addView(tabs);addGap(root,12);
        Button refresh=new Button(this);refresh.setText("ATUALIZAR ANÁLISE");root.addView(refresh);
        refresh.setOnClickListener(v->scan());
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
        content=vertical();content.setPadding(0,d(14),0,d(20));scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView foot=text("Os resultados dependem das permissões, do Android e dos limites de varredura.",11,MUTED,false);
        root.addView(foot);setContentView(root);
        requestPermissionsIfNeeded();render();
    }
    private boolean hasLocation(){
        return Build.VERSION.SDK_INT<23 || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermissionsIfNeeded(){
        ArrayList<String> missing=new ArrayList<>();
        if(!hasLocation())missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)!=PackageManager.PERMISSION_GRANTED)
            missing.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        if(!missing.isEmpty())requestPermissions(missing.toArray(new String[0]),100);
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(requestCode,permissions,grants);
        if(requestCode==100 && hasLocation())scan();else render();
    }
    private boolean locationEnabled(){
        LocationManager lm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        return lm!=null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)||lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }
    private void scan(){
        if(wifi==null){banner.setText("Este dispositivo não oferece serviço Wi-Fi.");return;}
        if(!hasLocation()){banner.setText("Permita a localização para visualizar redes.");requestPermissionsIfNeeded();return;}
        if(!locationEnabled()){banner.setText("Ative a localização do Android e tente novamente.");return;}
        if(!wifi.isWifiEnabled()){banner.setText("Ative o Wi-Fi para iniciar a análise.");return;}
        try{
            boolean started=wifi.startScan();
            networks=new ArrayList<>(wifi.getScanResults());
            Collections.sort(networks,(a,b)->Integer.compare(b.level,a.level));
            banner.setText(networks.size()+" redes disponíveis"+(started?" • solicitação enviada":" • resultados em cache (limite do Android)"));
            render();
        }catch(SecurityException ex){banner.setText("Permissões insuficientes para varredura.");}
        catch(Exception ex){banner.setText("Falha ao consultar Wi-Fi. Tente novamente.");}
    }
    private String security(ScanResult r){
        String c=r.capabilities==null?"":r.capabilities.toUpperCase();
        if(c.contains("SAE"))return "WPA3 / SAE";
        if(c.contains("OWE"))return "Enhanced Open (OWE)";
        if(c.contains("WPA2"))return "WPA2";
        if(c.contains("WPA"))return "WPA";
        if(c.contains("WEP"))return "WEP (inseguro)";
        if(c.contains("ESS"))return "Aberta (sem senha)";
        return "Não identificado";
    }
    private int channel(int freq){
        if(freq==2484)return 14;
        if(freq>=2412 && freq<=2472)return (freq-2407)/5;
        if(freq>=5000 && freq<=5895)return (freq-5000)/5;
        if(freq>=5955 && freq<=7115)return (freq-5950)/5;
        return 0;
    }
    private String band(int f){return f>=5925?"6 GHz":f>=4900?"5 GHz":"2,4 GHz";}
    private String quality(int dbm){return dbm>=-55?"Excelente":dbm>=-67?"Boa":dbm>=-75?"Regular":"Fraca";}
    private String ssid(ScanResult r){return r.SSID==null||r.SSID.isEmpty()?"Rede oculta":r.SSID;}
    private void render(){
        if(content==null)return;content.removeAllViews();
        if(currentTab==0)renderNetworks();
        else if(currentTab==1)renderConnection();
        else renderAnalysis();
    }
    private void renderNetworks(){
        LinearLayout intro=card(content);intro.addView(text("REDES DETECTADAS",14,ACCENT,true));
        intro.addView(text(networks.size()+" pontos de acesso visíveis",22,WHITE,true));
        if(networks.isEmpty()){
            LinearLayout c=card(content);c.addView(text("Toque em ATUALIZAR ANÁLISE. Wi-Fi, localização e permissões precisam estar ativos.",15,MUTED,false));return;
        }
        for(ScanResult r:networks){
            LinearLayout c=card(content);
            c.addView(text(ssid(r),18,WHITE,true));addGap(c,5);
            c.addView(text(quality(r.level)+"  •  "+r.level+" dBm",14,r.level>=-67?GREEN:YELLOW,false));
            c.addView(text(band(r.frequency)+"  •  Canal "+channel(r.frequency)+"  •  "+r.frequency+" MHz",13,MUTED,false));
            c.addView(text(security(r),13,ACCENT,false));
        }
    }
    private void renderConnection(){
        LinearLayout c=card(content);c.addView(text("CONEXÃO ATUAL",15,ACCENT,true));addGap(c,12);
        if(wifi==null||!wifi.isWifiEnabled()){c.addView(text("Wi-Fi desativado ou indisponível.",16,WHITE,false));return;}
        try{
            WifiInfo info=wifi.getConnectionInfo();
            if(info==null || info.getNetworkId()==-1){c.addView(text("Nenhuma conexão Wi-Fi identificada.",16,WHITE,false));return;}
            String name=info.getSSID();if(name==null||name.equals("<unknown ssid>"))name="SSID não disponível";
            line(c,"Nome da rede",name.replace("\"",""));
            line(c,"Intensidade",info.getRssi()+" dBm • "+quality(info.getRssi()));
            line(c,"Velocidade de link",info.getLinkSpeed()+" Mbps");
            line(c,"Frequência",info.getFrequency()+" MHz");
            int ip=info.getIpAddress();
            line(c,"IPv4 local",(ip&255)+"."+((ip>>8)&255)+"."+((ip>>16)&255)+"."+((ip>>24)&255));
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n=cm.getActiveNetwork();NetworkCapabilities nc=cm.getNetworkCapabilities(n);
            line(c,"Conexão validada pelo Android",nc!=null&&nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"Sim":"Não confirmada");
        }catch(Exception e){c.addView(text("Não foi possível ler os dados. Verifique as permissões.",14,MUTED,false));}
        LinearLayout note=card(content);note.addView(text("O endereço IP mostrado é local. A velocidade de link não equivale à velocidade da internet.",13,MUTED,false));
    }
    private void renderAnalysis(){
        LinearLayout summary=card(content);summary.addView(text("RESUMO DE SEGURANÇA",15,ACCENT,true));addGap(summary,8);
        int open=0,wep=0,wpa=0,wpa3=0;
        for(ScanResult r:networks){
            String s=security(r);
            if(s.startsWith("Aberta"))open++;
            if(s.startsWith("WEP"))wep++;
            if(s.equals("WPA"))wpa++;
            if(s.startsWith("WPA3"))wpa3++;
        }
        line(summary,"Redes analisadas",""+networks.size());
        line(summary,"Redes abertas",""+open);
        line(summary,"WEP / WPA antigos",""+(wep+wpa));
        line(summary,"Redes WPA3 detectadas",""+wpa3);
        LinearLayout tips=card(content);tips.addView(text("RECOMENDAÇÕES",15,ACCENT,true));addGap(tips,10);
        tips.addView(text("• Prefira WPA3 ou WPA2-AES na sua própria rede.\n\n• Desative WPS no roteador, quando possível.\n\n• Use senha longa e exclusiva e mantenha o firmware atualizado.\n\n• Evite transmitir dados sensíveis em redes abertas sem proteção adicional.\n\n• Canais compartilhados podem causar interferência; a lista não mede tráfego real.",14,WHITE,false));
        LinearLayout channels=card(content);channels.addView(text("OCUPAÇÃO OBSERVADA POR FAIXA",15,ACCENT,true));addGap(channels,10);
        int a=0,b=0,d6=0;
        for(ScanResult r:networks){if(r.frequency>=5925)d6++;else if(r.frequency>=4900)b++;else a++;}
        line(channels,"2,4 GHz",a+" redes");
        line(channels,"5 GHz",b+" redes");
        line(channels,"6 GHz",d6+" redes");
        channels.addView(text("Contagem de pontos de acesso visíveis, não de dispositivos conectados.",12,MUTED,false));
    }
}
