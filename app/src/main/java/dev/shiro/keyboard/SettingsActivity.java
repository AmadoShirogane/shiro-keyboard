package dev.shiro.keyboard;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.InputType;

public class SettingsActivity extends Activity {
    private LinearLayout page;
    private void text(String s) { TextView t=new TextView(this);t.setText(s);t.setTextSize(17);t.setTextColor(Color.WHITE);t.setPadding(0,18,0,10);page.addView(t); }
    private EditText field(String title,String value) {text(title);EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(Color.WHITE);e.setText(value);page.addView(e);return e;}
    private void action(String title,Runnable r){Button b=new Button(this);b.setText(title);page.addView(b);b.setOnClickListener(v->r.run());}
    @Override public void onCreate(Bundle b){super.onCreate(b);
        ScrollView sc=new ScrollView(this);page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(28,40,28,32);page.setBackgroundColor(Color.rgb(18,21,32));sc.addView(page);setContentView(sc);
        text("SHIRO KEYBOARD · 0.1\nAZERTY • Web • IA");
        text("1. Active Shiro, puis sélectionne-le comme clavier. Android avertit que tout clavier peut lire la saisie. Shiro ne journalise pas tes frappes.");
        action("Activer le clavier",()->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        action("Choisir le clavier",()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());
        var prefs=getSharedPreferences("settings",0);
        text("Apparence"); Spinner theme=new Spinner(this);theme.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Nuit violette","Clair","Océan"}));theme.setSelection(prefs.getInt("theme",0));page.addView(theme);
        Spinner font=new Spinner(this);font.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Sans serif","Serif","Monospace"}));font.setSelection(prefs.getInt("font",0));page.addView(font);
        text("IA facultative · API OpenAI\nSeul le brouillon affiché est envoyé, quand tu appuies sur Envoyer. Aucun envoi automatique de la saisie. Une clé API et une facturation API distincte peuvent être nécessaires ; l’abonnement ChatGPT ne fournit pas cette clé.");
        EditText model=field("Modèle disponible sur ton compte",prefs.getString("model","gpt-4.1-mini"));
        EditText token=field("Nouvelle clé API (vide = conserver)","");token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);token.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        action("Enregistrer",()->{
            if(model.getText().toString().trim().isEmpty()){Toast.makeText(this,"Indique un modèle",Toast.LENGTH_LONG).show();return;}
            try {if(token.length()>0)TokenVault.save(this,token.getText().toString().trim());prefs.edit().putInt("theme",theme.getSelectedItemPosition()).putInt("font",font.getSelectedItemPosition()).putString("model",model.getText().toString().trim()).apply();token.setText("");Toast.makeText(this,"Enregistré",Toast.LENGTH_SHORT).show();}
            catch(Exception ex){Toast.makeText(this,"Impossible de stocker la clé",Toast.LENGTH_LONG).show();}
        });
        action("Effacer la clé API",()->{try{TokenVault.save(this,"");Toast.makeText(this,"Clé effacée",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Échec",Toast.LENGTH_SHORT).show();}});
        text("La police change les touches seulement. Recherche : ton navigateur ouvre DuckDuckGo avec le brouillon. IA et recherche sont désactivées dans les champs sensibles. Pas de publicité, pas de statistiques intégrées. Première version : pas encore d’autocorrection, de swipe ou de dictée.");
    }
}
