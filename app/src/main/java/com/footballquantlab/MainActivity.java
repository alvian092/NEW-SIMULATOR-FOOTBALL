package com.footballquantlab;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
  int bg=0xff07111f, panel=0xff122238, fieldBg=0xff091628, mint=0xff45d6b0, muted=0xff91a5ba, white=0xffedf4ff;
  LinearLayout root, statsBox, resultBox; Spinner homeSpinner,awaySpinner,leagueSpinner,maxSpinner;
  TextView homeXg,awayXg,status,summary,probH,probD,probA,probUnder,fixtureCount,successCount,skipCount,accuracyDc,accuracyPoi,trackerInfo;
  EditText rhoInput,fixtureDate; JSONArray matches=new JSONArray(),tracker=new JSONArray();
  ArrayList<String> teams=new ArrayList<>(),leagues=new ArrayList<>();
  ArrayList<JSONObject> selectedHome=new ArrayList<>(),selectedAway=new ArrayList<>();
  double hx=0,ax=0; int lastH=-1,lastD=-1,lastA=-1,lastU=-1; String lastFixture="";
  static final int PICK=77; Random rng=new Random();

  public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);loadLocal();build();refreshTeams();}
  GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(r);d.setStroke(1,0xff263b54);return d;}
  TextView txt(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
  LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,h));}
  void card(LinearLayout p,View v){LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,-2);q.bottomMargin=16;v.setPadding(20,20,20,20);v.setBackground(shape(panel,26));p.addView(v,q);}
  TextView label(String s){TextView t=txt(s.toUpperCase(Locale.ROOT),12,muted,true);return t;}
  EditText edit(String s){EditText e=new EditText(this);e.setSingleLine(true);e.setText(s);e.setTextColor(white);e.setHintTextColor(muted);e.setTextSize(16);e.setPadding(16,12,16,12);e.setBackground(shape(fieldBg,16));return e;}
  Button btn(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(null,Typeface.BOLD);b.setTextColor(primary?0xff06251e:white);b.setBackground(shape(primary?mint:panel,18));return b;}
  LinearLayout pair(View a,View b){LinearLayout l=new LinearLayout(this);l.setOrientation(0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.rightMargin=7;l.addView(a,p);LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,-2,1);q.leftMargin=7;l.addView(b,q);return l;}
  Spinner spinner(ArrayList<String> values){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);s.setAdapter(a);s.setBackground(shape(fieldBg,15));return s;}
  void build(){
    ScrollView sc=new ScrollView(this);root=col();root.setPadding(20,18,20,28);root.setBackgroundColor(bg);sc.addView(root);setContentView(sc);
    LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);TextView brand=txt("⚽ FOOTBALL  ANALYTICS LAB",17,mint,true);head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));TextView badge=txt("● OFFLINE DATASET",10,mint,true);badge.setBackground(shape(0xff10352f,30));badge.setPadding(12,10,12,10);head.addView(badge);root.addView(head);gap(root,28);
    root.addView(txt("Backtesting Per Tim",25,white,true));gap(root,12);root.addView(txt("Rolling 20 pertandingan terakhir per tim sebelum tanggal setiap pertemuan historis.",14,muted,false));gap(root,22);
    LinearLayout form=col();form.addView(pair(fieldGroup("TIM KANDANG",homeSpinner=spinner(teams)),fieldGroup("TIM TANDANG",awaySpinner=spinner(teams))));gap(form,22);
    form.addView(pair(fieldGroup("KOMPETISI",leagueSpinner=spinner(leagues)),fieldGroup("TANGGAL FIXTURE (YYYY-MM-DD)",fixtureDate=edit(new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())))));gap(form,22);
    form.addView(pair(fieldGroup("DC RHO",rhoInput=edit("-0.08")),fieldGroup("MAX GOALS",maxSpinner=spinner(new ArrayList<>(Arrays.asList("6","7","8","9","10"))))));gap(form,20);
    TextView dataNote=txt("xG otomatis dihitung dari 20 laga terakhir kedua tim. Tidak ada input xG manual.",13,muted,false);form.addView(dataNote);gap(form,18);
    Button run=btn("Jalankan 10.000 simulasi",true);run.setOnClickListener(v->simulate());form.addView(pair(run,btn("Export CSV",false)));((Button)((LinearLayout)form.getChildAt(form.getChildCount()-1)).getChildAt(1)).setOnClickListener(v->exportCsv());
    gap(form,18);summary=txt("Impor dataset pertandingan untuk memulai analisis.",13,muted,false);summary.setPadding(16,16,16,16);summary.setBackground(shape(0xff102b38,18));form.addView(summary);card(root,form);
    Button importBtn=btn("Import CSV dataset",false);importBtn.setOnClickListener(v->pickCsv());root.addView(importBtn);gap(root,18);
    LinearLayout grid=col();grid.addView(pair(metric("FIXTURE DITEMUKAN",fixtureCount=txt("0",24,white,true)),metric("BERHASIL DIUJI",successCount=txt("0",24,white,true))));grid.addView(pair(metric("DILEWATI",skipCount=txt("0",24,white,true)),metric("AKURASI 1X2 · DC",accuracyDc=txt("—",24,white,true))));grid.addView(metric("AKURASI 1X2 · POISSON",accuracyPoi=txt("—",24,white,true)));card(root,grid);
    resultBox=col();resultBox.addView(txt("Hasil simulasi pertandingan",19,white,true));gap(resultBox,16);
    probH=txt("Home win —",17,white,true);probD=txt("Draw —",17,white,true);probA=txt("Away win —",17,white,true);probUnder=txt("Under 4.5 —",16,muted,false);resultBox.addView(probH);gap(resultBox,10);resultBox.addView(probD);gap(resultBox,10);resultBox.addView(probA);gap(resultBox,14);resultBox.addView(probUnder);card(root,resultBox);
    LinearLayout tr=col();tr.addView(txt("Prediction Tracker",19,white,true));trackerInfo=txt("0 prediksi tersimpan",13,muted,false);tr.addView(trackerInfo);Button save=btn("Simpan prediksi",true);save.setOnClickListener(v->savePrediction());tr.addView(save);Button actual=btn("Input skor aktual",false);actual.setOnClickListener(v->enterActual());tr.addView(actual);card(root,tr);refreshTracker();
  }
  View fieldGroup(String title,View v){LinearLayout l=col();l.addView(label(title));gap(l,8);l.addView(v);return l;}
  View metric(String title,TextView value){LinearLayout l=col();l.setPadding(14,16,14,18);l.setBackground(shape(panel,22));l.addView(label(title));gap(l,10);l.addView(value);return l;}
  void pickCsv(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/csv");startActivityForResult(i,PICK);}
  protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK&&c==RESULT_OK&&d!=null)try(InputStream in=getContentResolver().openInputStream(d.getData());FileOutputStream out=openFileOutput("matches.csv",MODE_PRIVATE)){byte[]b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);loadLocal();refreshTeams();toast("Dataset disimpan offline");}catch(Exception e){toast("CSV gagal dibaca: "+e.getMessage());}}
  void loadLocal(){try{File f=new File(getFilesDir(),"matches.csv");if(!f.exists())return;BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(f)));String line=br.readLine();if(line==null)return;String[]h=parse(line);HashMap<String,Integer>ix=new HashMap<>();for(int i=0;i<h.length;i++)ix.put(h[i].trim().toLowerCase(Locale.ROOT),i);matches=new JSONArray();while((line=br.readLine())!=null){String[]v=parse(line);JSONObject o=new JSONObject();for(String k:ix.keySet()){int j=ix.get(k);if(j<v.length)o.put(k,v[j]);}if(valid(o))matches.put(o);}br.close();}catch(Exception e){matches=new JSONArray();}}
  boolean valid(JSONObject o){return get(o,"date")!=null&&get(o,"home_team")!=null&&get(o,"away_team")!=null&&num(o,"home_goals",-1)>=0&&num(o,"away_goals",-1)>=0;}
  String get(JSONObject o,String k){String s=o.optString(k,"").trim();return s.isEmpty()||s.equals("null")?null:s;}
  double num(JSONObject o,String k,double d){try{return Double.parseDouble(o.optString(k,"").replace(",","."));}catch(Exception e){return d;}}
  String[] parse(String line){ArrayList<String>a=new ArrayList<>();StringBuilder b=new StringBuilder();boolean q=false;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(q&&i+1<line.length()&&line.charAt(i+1)=='"'){b.append('"');i++;}else q=!q;}else if(c==','&&!q){a.add(b.toString());b.setLength(0);}else b.append(c);}a.add(b.toString());return a.toArray(new String[0]);}
  void refreshTeams(){teams.clear();leagues.clear();teams.add("Pilih tim");leagues.add("Semua kompetisi");HashSet<String>ts=new HashSet<>(),ls=new HashSet<>();for(int i=0;i<matches.length();i++){JSONObject o=matches.optJSONObject(i);String h=get(o,"home_team"),a=get(o,"away_team"),l=get(o,"competition");if(h!=null)ts.add(h);if(a!=null)ts.add(a);if(l!=null)ls.add(l);}ArrayList<String>sorted=new ArrayList<>(ts);Collections.sort(sorted);teams.addAll(sorted);ArrayList<String>sl=new ArrayList<>(ls);Collections.sort(sl);leagues.addAll(sl);((ArrayAdapter)homeSpinner.getAdapter()).notifyDataSetChanged();((ArrayAdapter)awaySpinner.getAdapter()).notifyDataSetChanged();((ArrayAdapter)leagueSpinner.getAdapter()).notifyDataSetChanged();summary.setText(matches.length()+" baris pertandingan terbaca. Dataset wajib memiliki date, home_team, away_team, home_goals, away_goals, home_xg, away_xg; competition opsional.");}
  ArrayList<JSONObject> last20(String team,String before,String league){ArrayList<JSONObject>a=new ArrayList<>();for(int i=0;i<matches.length();i++){JSONObject o=matches.optJSONObject(i);String date=get(o,"date");if(date==null||date.compareTo(before)>=0)continue;if(!league.equals("Semua kompetisi")&&!league.equals(get(o,"competition")))continue;if(team.equals(get(o,"home_team"))||team.equals(get(o,"away_team")))a.add(o);}Collections.sort(a,(x,y)->get(y,"date").compareTo(get(x,"date")));if(a.size()>20)return new ArrayList<>(a.subList(0,20));return a;}
  double[] attackDefense(ArrayList<JSONObject>a,String team){double attack=0,def=0;int n=0;for(JSONObject o:a){boolean home=team.equals(get(o,"home_team"));String own=home?"home_xg":"away_xg",opp=home?"away_xg":"home_xg";double x=num(o,own,-1),y=num(o,opp,-1);if(x<0||y<0)continue;attack+=x;def+=y;n++;}return n==0?new double[]{-1,-1}:new double[]{attack/n,def/n};}
  void simulate(){String h=(String)homeSpinner.getSelectedItem(),a=(String)awaySpinner.getSelectedItem();if(h==null||a==null||h.equals("Pilih tim")||a.equals("Pilih tim")||h.equals(a)){toast("Pilih dua tim yang berbeda");return;}String date=fixtureDate.getText().toString().trim();if(!date.matches("\\d{4}-\\d{2}-\\d{2}")){toast("Tanggal harus YYYY-MM-DD");return;}String league=(String)leagueSpinner.getSelectedItem();selectedHome=last20(h,date,league);selectedAway=last20(a,date,league);if(selectedHome.size()<20||selectedAway.size()<20){toast("Data belum cukup: masing-masing tim perlu 20 pertandingan sebelum tanggal fixture");return;}double[]ha=attackDefense(selectedHome,h),aa=attackDefense(selectedAway,a);if(ha[0]<0||aa[0]<0){toast("Kolom home_xg dan away_xg wajib terisi");return;}hx=(ha[0]+aa[1])/2.0;ax=(aa[0]+ha[1])/2.0;if(hx<=0||ax<=0){toast("xG dataset tidak valid");return;}int n=10000,hw=0,dr=0,aw=0,under=0;int max=Integer.parseInt((String)maxSpinner.getSelectedItem());for(int i=0;i<n;i++){int hg=poisson(hx),ag=poisson(ax);if(hg>ag)hw++;else if(hg==ag)dr++;else aw++;if(hg+ag<5)under++;}lastH=Math.round(hw*100f/n);lastD=Math.round(dr*100f/n);lastA=100-lastH-lastD;lastU=Math.round(under*100f/n);lastFixture=h+" vs "+a;probH.setText("Home win  "+lastH+"%");probD.setText("Draw  "+lastD+"%");probA.setText("Away win  "+lastA+"%");probUnder.setText("Under 4.5  "+lastU+"%   ·   Over 4.5  "+(100-lastU)+"%");summary.setText("20 laga "+h+" + 20 laga "+a+" dipakai. Estimasi xG: "+h+" "+String.format(Locale.US,"%.2f",hx)+" · "+a+" "+String.format(Locale.US,"%.2f",ax)+" | 10.000 simulasi.");fixtureCount.setText("1");successCount.setText("1");skipCount.setText("0");}
  int poisson(double l){double p=Math.exp(-l),v=1;int k=0;do{k++;v*=rng.nextDouble();}while(v>p&&k<20);return k-1;}
  void savePrediction(){if(lastH<0){toast("Jalankan simulasi dahulu");return;}try{JSONObject o=new JSONObject();o.put("fixture",lastFixture);o.put("home",lastH);o.put("draw",lastD);o.put("away",lastA);o.put("xg_home",hx);o.put("xg_away",ax);o.put("date",fixtureDate.getText().toString());tracker.put(o);getPreferences(0).edit().putString("tracker",tracker.toString()).apply();refreshTracker();toast("Prediksi disimpan");}catch(Exception e){toast("Gagal simpan");}}
  void enterActual(){if(tracker.length()==0){toast("Tracker kosong");return;}EditText score=edit("");score.setHint("Skor, contoh 2-1");new AlertDialog.Builder(this).setTitle("Masukkan skor aktual").setView(score).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{String s=score.getText().toString().trim();if(!s.matches("\\d+[-:]\\d+")){toast("Format skor contoh 2-1");return;}try{JSONObject o=tracker.getJSONObject(tracker.length()-1);o.put("actual",s);getPreferences(0).edit().putString("tracker",tracker.toString()).apply();toast("Skor aktual disimpan");}catch(Exception e){toast("Gagal");}}).show();}
  void refreshTracker(){try{tracker=new JSONArray(getPreferences(0).getString("tracker","[]"));trackerInfo.setText(tracker.length()+" prediksi tersimpan");}catch(Exception e){}}
  void exportCsv(){try{StringBuilder b=new StringBuilder("fixture,home_probability,draw_probability,away_probability,actual\\n");for(int i=0;i<tracker.length();i++){JSONObject o=tracker.getJSONObject(i);b.append(o.optString("fixture")).append(",").append(o.optInt("home")).append(",").append(o.optInt("draw")).append(",").append(o.optInt("away")).append(",").append(o.optString("actual","")).append("\\n");}File f=new File(getExternalFilesDir(null),"football-quant-tracker.csv");try(FileOutputStream out=new FileOutputStream(f)){out.write(b.toString().getBytes("UTF-8"));}toast("CSV diekspor: "+f.getAbsolutePath());}catch(Exception e){toast("Export gagal");}}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
