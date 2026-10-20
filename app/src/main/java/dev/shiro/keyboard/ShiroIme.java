package dev.shiro.keyboard;

import android.inputmethodservice.InputMethodService;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.*;

public class ShiroIme extends InputMethodService {
    private LinearLayout root;
    private boolean shift=false, symbols=false, sensitive=false, busy=false;
    private String panel="", result="", status="";
    private final StringBuilder draft=new StringBuilder();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Future<?> pending;
    private volatile HttpURLConnection connection;
    private int generation=0, bg, fg, keyColor, accent;
    private Typeface face=Typeface.DEFAULT;
    private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
    @Override public boolean onEvaluateFullscreenMode(){return false;}
    @Override public View onCreateInputView(){root=new LinearLayout(this);root.setOrientation(1);draw();return root;}
    private void cancel(){generation++;if(pending!=null)pending.cancel(true);HttpURLConnection c=connection;if(c!=null)c.disconnect();busy=false;}
    @Override public void onStartInput(EditorInfo info, boolean restarting){super.onStartInput(info,restarting);cancel();draft.setLength(0);panel="";result="";status="";shift=false;symbols=false;
        int cls=info.inputType&InputType.TYPE_MASK_CLASS, variation=info.inputType&InputType.TYPE_MASK_VARIATION;
        sensitive=(cls==InputType.TYPE_CLASS_TEXT&&(variation==InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
            ||(cls==InputType.TYPE_CLASS_NUMBER&&variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD)
            ||(info.imeOptions&EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0;
        if(root!=null)draw();
    }
    @Override public void onFinishInput(){cancel();draft.setLength(0);result="";panel="";super.onFinishInput();}
    @Override public void onDestroy(){cancel();worker.shutdownNow();super.onDestroy();}
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(0);r.setPadding(dp(2),dp(1),dp(2),dp(1));root.addView(r);return r;}
    private void label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(fg);t.setTextSize(size);t.setPadding(dp(10),dp(5),dp(10),dp(5));root.addView(t);}
    private Button button(LinearLayout r,String s,float weight,Runnable task){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(fg);b.setTypeface(face);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(0,0,0,0);
        GradientDrawable d=new GradientDrawable();d.setColor(keyColor);d.setCornerRadius(dp(9));b.setBackground(d);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),weight);lp.setMargins(dp(2),dp(2),dp(2),dp(2));r.addView(b,lp);b.setOnClickListener(v->task.run());return b;}
    private void draw(){if(root==null)return;
        var prefs=getSharedPreferences("settings",0);int theme=prefs.getInt("theme",0);
        bg=Color.rgb(18,21,32);fg=Color.WHITE;keyColor=Color.rgb(39,44,63);accent=Color.rgb(176,147,255);
        if(theme==1){bg=Color.rgb(238,240,247);fg=Color.rgb(24,27,40);keyColor=Color.WHITE;accent=Color.rgb(97,70,191);}
        if(theme==2){bg=Color.rgb(7,30,40);keyColor=Color.rgb(17,56,70);accent=Color.rgb(96,222,215);}
        face=new Typeface[]{Typeface.DEFAULT,Typeface.SERIF,Typeface.MONOSPACE}[Math.min(2,Math.max(0,prefs.getInt("font",0)))];
        root.removeAllViews();root.setBackgroundColor(bg);root.setPadding(dp(3),dp(5),dp(3),dp(8));
        LinearLayout bar=row();button(bar,"✦ Shiro",1,()->{Intent i=new Intent(this,SettingsActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);});
        Button web=button(bar,"Web",1,()->openPanel("web"));Button ai=button(bar,"IA",1,()->openPanel("ai"));web.setEnabled(!sensitive);ai.setEnabled(!sensitive);
        button(bar,"🌐",0.7f,()->{if(android.os.Build.VERSION.SDK_INT < 28 || !switchToNextInputMethod(false))((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();});
        if(!panel.isEmpty()){
            label(panel.equals("web")?"RECHERCHE WEB · DuckDuckGo":"IA · envoi manuel à OpenAI",12);
            label(draft.length()==0?"Tape ton brouillon avec les touches ci-dessous.":draft.substring(Math.max(0,draft.length()-350)),16);
            LinearLayout controls=row();button(controls,"Sélection",1,()->{InputConnection ic=getCurrentInputConnection();CharSequence s=ic==null?null:ic.getSelectedText(0);if(s!=null){draft.setLength(0);draft.append(s.subSequence(0,Math.min(s.length(),4000)));result="";status="";draw();}else{status="Sélectionne d’abord du texte dans l’application.";draw();}});
            button(controls,"Vider",0.7f,()->{cancel();draft.setLength(0);result="";status="";draw();});button(controls,"Fermer",0.8f,()->{cancel();draft.setLength(0);result="";panel="";draw();});
            if(panel.equals("web")){Button go=button(controls,"Chercher",1,()->search());go.setEnabled(draft.length()>0);}
            else{
                LinearLayout actions=row();for(String title:new String[]{"Corriger","Reformuler","Anglais"}){Button b=button(actions,title,1,()->send(title));b.setEnabled(!busy&&draft.length()>0);}
                if(!result.isEmpty()){
                    ScrollView scroll=new ScrollView(this);TextView answer=new TextView(this);answer.setText(result);answer.setTextColor(fg);answer.setTextSize(15);answer.setPadding(dp(8),dp(4),dp(8),dp(4));scroll.addView(answer);root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(90)));
                    button(row(),"Insérer la réponse",1,()->{InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(result,1);result="";draft.setLength(0);panel="";draw();});
                }
            }
            if(!status.isEmpty())label(status,12);
        }else if(sensitive)label("Champ sensible · Web et IA désactivés",12);
        String[] rows=symbols?new String[]{"1234567890","@#€%&*+-=","()!?/:;,."}:new String[]{"azertyuiop","qsdfghjklm","wxcvbn"};
        for(int n=0;n<rows.length;n++){LinearLayout r=row();if(n==2)button(r,shift?"⇧ ●":"⇧",1.3f,()->{shift=!shift;draw();});
            for(char c:rows[n].toCharArray()){String raw=String.valueOf(c);String shown=shift&&!symbols?raw.toUpperCase(Locale.FRENCH):raw;button(r,shown,1,()->type(shown));}
            if(n==2){Button del=button(r,"⌫",1.3f,()->erase());del.setOnLongClickListener(v->{if(!panel.isEmpty()){draft.setLength(0);draw();}else{InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.deleteSurroundingText(50,0);}return true;});}
        }
        LinearLayout accents=row();for(String c:new String[]{"é","è","ê","à","ç","ù","’"})button(accents,c,1,()->type(shift?c.toUpperCase(Locale.FRENCH):c));
        LinearLayout bottom=row();button(bottom,symbols?"ABC":"?123",1,()->{symbols=!symbols;draw();});button(bottom,",",0.6f,()->type(","));button(bottom,"espace",3,()->type(" "));button(bottom,".",0.6f,()->type("."));button(bottom,"↵",1,()->enter());
    }
    private void openPanel(String which){if(sensitive)return;cancel();panel=which;draft.setLength(0);result="";status="";draw();}
    private void type(String s){if(!panel.isEmpty()){if(busy)return;if(draft.length()+s.length()<=4000){draft.append(s);result="";status="";}}else{InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(s,1);}if(shift)shift=false;draw();}
    private void erase(){if(!panel.isEmpty()){if(busy)return;if(draft.length()>0)draft.delete(draft.offsetByCodePoints(draft.length(),-1),draft.length());result="";draw();return;}
        InputConnection ic=getCurrentInputConnection();if(ic!=null){CharSequence selected=ic.getSelectedText(0);if(selected!=null&&selected.length()>0)ic.commitText("",1);else ic.deleteSurroundingTextInCodePoints(1,0);}}
    private void enter(){if(!panel.isEmpty()){if(panel.equals("web"))search();else type("\n");return;}InputConnection ic=getCurrentInputConnection();EditorInfo e=getCurrentInputEditorInfo();if(ic==null||e==null)return;int action=e.imeOptions&EditorInfo.IME_MASK_ACTION;
        if((e.imeOptions&EditorInfo.IME_FLAG_NO_ENTER_ACTION)==0&&action!=EditorInfo.IME_ACTION_NONE&&action!=EditorInfo.IME_ACTION_UNSPECIFIED)ic.performEditorAction(action);else ic.commitText("\n",1);}
    private void search(){if(sensitive||draft.toString().trim().isEmpty())return;Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://duckduckgo.com/?q="+Uri.encode(draft.toString())));i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{startActivity(i);draft.setLength(0);panel="";draw();}catch(Exception e){status="Aucun navigateur disponible.";draw();}}
    private void send(String operation){if(sensitive||busy||draft.length()==0)return;
        final String token;try{token=TokenVault.read(this);}catch(Exception e){status="Clé inaccessible : enregistre-la à nouveau dans les paramètres.";draw();return;}
        if(token.isEmpty()){status="Configure une clé API dans ✦ Shiro.";draw();return;}
        final String model=getSharedPreferences("settings",0).getString("model","gpt-4.1-mini");final String input=draft.toString();final int request=++generation;busy=true;result="";status="Envoi du brouillon à OpenAI…";draw();
        pending=worker.submit(()->{HttpURLConnection local=null;try{
            String instruction=operation.equals("Corriger")?"Corrige l’orthographe et la grammaire du texte. Renvoie uniquement le texte corrigé.":operation.equals("Reformuler")?"Reformule ce texte clairement dans sa langue originale. Renvoie uniquement la reformulation.":"Traduis le texte en anglais. Renvoie uniquement la traduction.";
            JSONObject body=new JSONObject().put("model",model).put("messages",new JSONArray().put(new JSONObject().put("role","system").put("content",instruction)).put(new JSONObject().put("role","user").put("content",input)));
            local=(HttpURLConnection)new URL("https://api.openai.com/v1/chat/completions").openConnection();connection=local;local.setInstanceFollowRedirects(false);local.setConnectTimeout(15000);local.setReadTimeout(45000);local.setRequestMethod("POST");local.setDoOutput(true);local.setRequestProperty("Content-Type","application/json");local.setRequestProperty("Authorization","Bearer "+token);
            try(OutputStream os=local.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=local.getResponseCode();if(code<200||code>=300)throw new IOException(code==401?"Clé API refusée.":code==429?"Quota ou limite de requêtes atteint.":"Service IA indisponible (HTTP "+code+").");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream stream=local.getInputStream()){byte[] buffer=new byte[4096];int n;while((n=stream.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException("Requête annulée.");if(bytes.size()+n>1048576)throw new IOException("Réponse trop volumineuse.");bytes.write(buffer,0,n);}}
            String answer=new JSONObject(bytes.toString("UTF-8")).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            main.post(()->{if(request!=generation)return;busy=false;result=answer;status="Vérifie la réponse avant de l’insérer.";draw();});
        }catch(Exception e){final String message=e instanceof IOException&&e.getMessage()!=null&&!e.getMessage().contains("http")?e.getMessage():"La requête IA a échoué. Vérifie le réseau et le modèle configuré.";main.post(()->{if(request!=generation)return;busy=false;status=message;draw();});}
        finally{if(local!=null)local.disconnect();if(connection==local)connection=null;}});
    }
}
