package com.footballquantlab;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private final int BG=Color.rgb(13,23,30), PANEL=Color.rgb(28,48,57), MINT=Color.rgb(56,211,166), MUTED=Color.rgb(166,184,194);
    private LinearLayout root, resultPanel;
    private EditText homeName, awayName, homeXg, awayXg;
    private TextView pHome, pDraw, pAway, pUnder, pOver, simLabel, csvLabel, trackerLabel;
    private int lastHome=-1,lastAway=-1;
    private String lastFixture="";
    private static final int PICK_CSV=41;
    private final Random random=new Random();

    @Override public void onCreate(Bundle state){ super.onCreate(state); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG); buildUi(); refreshStorageLabels(); }

    private GradientDrawable shape(int color,int radius){ GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
    private TextView text(String value,int size,int color,boolean bold){ TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private LinearLayout column(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private void pad(View v,int n){v.setPadding(n,n,n,n);}
    private void gap(LinearLayout l,int h){ View v=new View(this); l.addView(v,new LinearLayout.LayoutParams(1,h)); }
    private void card(LinearLayout parent,View child){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,18); child.setBackground(shape(PANEL,28)); pad(child,22); parent.addView(child,p); }
    private EditText field(String hint,String value){ EditText e=new EditText(this); e.setSingleLine(true); e.setText(value); e.setHint(hint); e.setHintTextColor(MUTED); e.setTextColor(Color.WHITE); e.setTextSize(17); e.setPadding(16,12,16,12); e.setBackground(shape(Color.rgb(39,62,71),16)); return e; }
    private Button button(String label){ Button b=new Button(this); b.setText(label); b.setTextColor(Color.rgb(7,35,29)); b.setTextSize(15); b.setTypeface(null,Typeface.BOLD); b.setAllCaps(false); b.setBackground(shape(MINT,18)); return b; }

    private void buildUi(){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); root=column(); root.setBackgroundColor(BG); root.setPadding(22,22,22,30); scroll.addView(root); setContentView(scroll);
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setOrientation(LinearLayout.HORIZONTAL);
        TextView logo=text("▣",32,MINT,true); header.addView(logo); TextView title=text("  FOOTBALL QUANT LAB",22,Color.WHITE,true); header.addView(title,new LinearLayout.LayoutParams(0,-2,1)); TextView ready=text("ENGINE READY",12,MINT,true); ready.setBackground(shape(Color.rgb(14,57,46),30)); pad(ready,12); header.addView(ready); root.addView(header);
        gap(root,12); root.addView(text("MONTE CARLO  ·  MATCH ANALYTICS",15,MUTED,false)); gap(root,22);
        LinearLayout stats=new LinearLayout(this); stats.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout a=column(); a.setBackground(shape(PANEL,24)); pad(a,18); a.addView(text("SIMULATIONS",13,MUTED,false)); gap(a,10); simLabel=text("10,000",30,MINT,true); a.addView(simLabel); gap(a,8); a.addView(text("Per pertandingan",13,MUTED,false));
        LinearLayout b=column(); b.setBackground(shape(PANEL,24)); pad(b,18); b.addView(text("DATASET",13,MUTED,false)); gap(b,10); csvLabel=text("BELUM ADA",20,MINT,true); b.addView(csvLabel); gap(b,8); b.addView(text("Tersimpan di perangkat",12,MUTED,false));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-2,1); sp.setMargins(0,0,8,0); stats.addView(a,sp); LinearLayout.LayoutParams sp2=new LinearLayout.LayoutParams(0,-2,1); sp2.setMargins(8,0,0,0); stats.addView(b,sp2); root.addView(stats); gap(root,20);
        LinearLayout form=column(); form.addView(text("MATCH SETUP",14,MUTED,true)); gap(form,10);
        homeName=field("Home team","Manchester City"); form.addView(homeName); gap(form,8); awayName=field("Away team","Fulham"); form.addView(awayName); gap(form,8);
        LinearLayout xs=new LinearLayout(this); homeXg=field("Home xG","2.10"); awayXg=field("Away xG","0.85"); LinearLayout.LayoutParams xp=new LinearLayout.LayoutParams(0,-2,1); xp.setMargins(0,0,6,0); xs.addView(homeXg,xp); LinearLayout.LayoutParams yp=new LinearLayout.LayoutParams(0,-2,1); yp.setMargins(6,0,0,0); xs.addView(awayXg,yp); form.addView(xs); gap(form,12);
        Button run=button("RUN 10,000 SIMULATIONS"); run.setOnClickListener(v->simulate()); form.addView(run); card(root,form);
        resultPanel=column(); resultPanel.addView(text("MATCH PROBABILITY",14,MUTED,true)); gap(resultPanel,16);
        pHome=text("Home win     —",18,Color.WHITE,true); resultPanel.addView(pHome); gap(resultPanel,8); pDraw=text("Draw          —",18,Color.WHITE,true); resultPanel.addView(pDraw); gap(resultPanel,8); pAway=text("Away win      —",18,Color.WHITE,true); resultPanel.addView(pAway); gap(resultPanel,14); pUnder=text("Under 4.5     —",17,MUTED,false); resultPanel.addView(pUnder); gap(resultPanel,8); pOver=text("Over 4.5      —",17,MUTED,false); resultPanel.addView(pOver); card(root,resultPanel);
        LinearLayout actions=column(); actions.addView(text("DATA & TRACKER",14,MUTED,true)); gap(actions,8);
        Button csv=button("IMPORT CSV DATASET"); csv.setOnClickListener(v->pickCsv()); actions.addView(csv); gap(actions,8);
        Button save=button("SAVE PREDICTION TO TRACKER"); save.setOnClickListener(v->savePrediction()); actions.addView(save); gap(actions,8);
        Button actual=button("INPUT ACTUAL SCORE"); actual.setOnClickListener(v->enterActual()); actions.addView(actual); gap(actions,12);
        trackerLabel=text("Tracker: 0 predictions",14,MUTED,false); actions.addView(trackerLabel); card(root,actions);
        TextView foot=text("Probabilitas adalah estimasi statistik, bukan jaminan hasil.",12,MUTED,false); root.addView(foot);
    }

    private double number(EditText e,double fallback){ try {return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}catch(Exception ex){return fallback;} }
    private int poisson(double lambda){ double limit=Math.exp(-lambda), product=1; int k=0; do{k++;product*=random.nextDouble();}while(product>limit&&k<15);return k-1; }
    private void simulate(){
        double hx=number(homeXg,1.4), ax=number(awayXg,1.0); if(hx<=0||ax<=0||hx>6||ax>6){toast("xG harus di antara 0.01 dan 6.00");return;}
        int hw=0,dr=0,aw=0,under=0; int n=10000;
        for(int i=0;i<n;i++){int h=poisson(hx),a=poisson(ax);if(h>a)hw++;else if(h==a)dr++;else aw++;if(h+a<5)under++;}
        int homePct=Math.round(hw*100f/n),drawPct=Math.round(dr*100f/n),awayPct=100-homePct-drawPct,underPct=Math.round(under*100f/n);
        lastHome=homePct;lastAway=awayPct;lastFixture=homeName.getText().toString()+" vs "+awayName.getText().toString();
        pHome.setText("Home win     "+homePct+"%"); pDraw.setText("Draw          "+drawPct+"%"); pAway.setText("Away win      "+awayPct+"%"); pUnder.setText("Under 4.5     "+underPct+"%"); pOver.setText("Over 4.5      "+(100-underPct)+"%");
        simLabel.setText("10,000"); toast("Simulasi selesai");
    }
    private void pickCsv(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/*");startActivityForResult(i,PICK_CSV); }
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==PICK_CSV&&result==RESULT_OK&&data!=null){Uri uri=data.getData();try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=openFileOutput("dataset.csv",MODE_PRIVATE)){byte[] buf=new byte[8192];int len;while((len=in.read(buf))>0)out.write(buf,0,len);toast("CSV tersimpan permanen di aplikasi");refreshStorageLabels();}catch(Exception e){toast("Gagal menyimpan CSV");}}}
    private void refreshStorageLabels(){ File f=new File(getFilesDir(),"dataset.csv"); if(csvLabel!=null)csvLabel.setText(f.exists()?String.format(Locale.US,"%.1f KB",f.length()/1024f):"BELUM ADA"); int count=getPreferences(0).getInt("tracker_count",0);if(trackerLabel!=null)trackerLabel.setText("Tracker: "+count+" predictions"); }
    private JSONArray tracker(){try{return new JSONArray(getPreferences(0).getString("tracker","[]"));}catch(Exception e){return new JSONArray();}}
    private void savePrediction(){if(lastHome<0){toast("Jalankan simulasi dulu");return;}try{JSONArray arr=tracker();JSONObject row=new JSONObject();row.put("fixture",lastFixture);row.put("home_probability",lastHome);row.put("away_probability",lastAway);row.put("saved_at",System.currentTimeMillis());row.put("actual","");arr.put(row);getPreferences(0).edit().putString("tracker",arr.toString()).putInt("tracker_count",arr.length()).apply();refreshStorageLabels();toast("Prediksi disimpan ke tracker");}catch(Exception e){toast("Gagal menyimpan prediksi");}}
    private void enterActual(){JSONArray arr=tracker();if(arr.length()==0){toast("Belum ada prediksi tersimpan");return;}EditText score=new EditText(this);score.setHint("Contoh: 2-1");score.setTextColor(Color.WHITE);score.setHintTextColor(MUTED);new android.app.AlertDialog.Builder(this).setTitle("Hasil aktual: "+lastFixture).setView(score).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{String s=score.getText().toString().trim();if(!s.matches("\\d+\\s*[-:]\\s*\\d+")){toast("Format skor contoh 2-1");return;}try{JSONObject row=arr.getJSONObject(arr.length()-1);row.put("actual",s);getPreferences(0).edit().putString("tracker",arr.toString()).apply();toast("Hasil aktual disimpan");}catch(Exception e){toast("Gagal menyimpan hasil");}}).show();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
