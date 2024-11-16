package com.example.memory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;

public class MenuPrincipal extends AppCompatActivity {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editorPreferencias;
    EditText txt_nickname;
    MediaPlayer mediaPlayer;
    MiMediaPlayer miMediaPlayer;

    @Override
    protected void onResume() {
        super.onResume();
        try {
            miMediaPlayer.cambiarCancion("MenuPrincipal");
        } catch (IOException e) {
            Log.i("#####selene", "Ha entrado en el catch");
            throw new RuntimeException(e);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editorPreferencias = prefs.edit();
        inicializarIdiomaYTema(prefs);
        setContentView(R.layout.activity_menu);

        //Creamos el mediaplayer
        miMediaPlayer = MiMediaPlayer.getInstance();
        miMediaPlayer.init(getApplicationContext());
        mediaPlayer = miMediaPlayer.getSingletonMedia();
        miMediaPlayer.startMusic();

        //Para mantener la pantalla del dispositivo encendida
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*-------------[[ BB.DD ]]-------------*/
        BBDD miBBDD = new BBDD(this, getString(R.string.txt_bbddName), null, 1);
        SQLiteDatabase db = miBBDD.getWritableDatabase();

        /*-------------[[ VIEWS ]]-------------*/
        txt_nickname = findViewById(R.id.txt_nickname);
        TextView txt_quantityCharacters = findViewById(R.id.txt_quantityCharacters);
        //Con esto se escribira el texto directamente en mayusculas y tiene como limite max 10 caracteres
        txt_nickname.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(10)});

        txt_nickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                int letrasIntroducidas = charSequence.length();
                int totalLetras = 10; // Cambia esto al número total de letras permitidas

                String cantidadLetras = letrasIntroducidas + "/" + totalLetras;
                txt_quantityCharacters.setText(cantidadLetras);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        registerForContextMenu(txt_nickname);

        //Con esto, cuando cliquemos, no se nos abrirá el teclado de Google para escribir
        txt_nickname.setShowSoftInputOnFocus(false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (!prefs.getString(getString(R.string.nickname_actual), "").equals("")) {
            Log.i("@@SELENE", "Ponemos texto en alias: " + prefs.getString(getString(R.string.nickname_actual), ""));
            txt_nickname.setText(prefs.getString(getString(R.string.nickname_actual), ""));
            Log.i("@@SELENE", "Recuperamos texto nickname: " + txt_nickname.getText());
        }

        //BOTONES
        Button btn_startGame = findViewById(R.id.btn_start);
        Button btn_score = findViewById(R.id.btn_score);
        Button btn_settings = findViewById(R.id.btn_settings);
        Button btn_notifications = findViewById(R.id.btn_notifications);
        ImageButton btn_sound = findViewById(R.id.btn_sound);

        /*-----------------------[[ ASIGNAMOS LISTENERS A LOS BUTTONS ]]-----------------------*/
        btn_startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!txt_nickname.getText().toString().equals("")) {
                    insertAliasBBDD(db, txt_nickname.getText().toString());
                    Intent activityJuego = new Intent(getApplicationContext(), Juego.class);
                    activityJuego.putExtra(getString(R.string.key_alias), txt_nickname.getText().toString());
                    editorPreferencias.putString(getString(R.string.nickname_actual), txt_nickname.getText().toString());
                    editorPreferencias.commit();
                    startActivityForResult(activityJuego, 0);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.necesitas_alias_juego), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        btn_score.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent activityScore = new Intent(getApplicationContext(), ScoreActivity.class);
                startActivityForResult(activityScore, 1);
            }
        });

        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent activitySettings = new Intent(getApplicationContext(), Settings.class);
                guardarAliasActual(txt_nickname);
                startActivityForResult(activitySettings, 2);
            }
        });

        btn_notifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent activityNotifications = new Intent(getApplicationContext(), Notificaciones.class);
                startActivityForResult(activityNotifications, 3);
            }
        });

        btn_sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer.isPlaying()){
                    miMediaPlayer.pauseMusic();
                    miMediaPlayer.setMuteado(true);
                    btn_sound.setImageResource(R.drawable.soundoff);
                } else {
                    miMediaPlayer.startMusic();
                    miMediaPlayer.setMuteado(false);
                    btn_sound.setImageResource(R.drawable.soundon);
                }
            }
        });
    }

    /*--------------------------[[ FUNCIONES ]]--------------------------*/
    private void inicializarIdiomaYTema(SharedPreferences prefs) {
        //Se carga el idioma antes que el ContentView
        establecerIdioma(prefs.getString(getString(R.string.idioma_seleccionado), "es"));
        establecerTema();
    }

    public void establecerIdioma(String idioma) {
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void establecerTema() {
        boolean temaOscuro = prefs.getBoolean(getString(R.string.tema), false);
        AppCompatDelegate.setDefaultNightMode(temaOscuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void guardarAliasActual(TextView txt_nickname) {
        if (!txt_nickname.getText().equals("")) {
            editorPreferencias.putString(getString(R.string.nickname_actual), txt_nickname.getText().toString());
            editorPreferencias.commit();
        } else {
            editorPreferencias.putString(getString(R.string.nickname_actual), "");
            editorPreferencias.commit();
        }
    }

    /*--------------------------[[ BASE DE DATOS ]]--------------------------*/
    private void insertAliasBBDD(SQLiteDatabase db, String alias) {
        Cursor c1 = db.rawQuery("SELECT Nombre FROM Jugadores WHERE Nombre = '" + alias + "'", null);

        //Si no existe un jugador con el alias, se crea
        if (c1 != null && !c1.moveToFirst()) {
            do {
                db.execSQL("INSERT INTO Jugadores (Nombre) VALUES ('" + alias + "')");
            } while (c1.moveToNext());

        }
        //Cerramos el cursor
        c1.close();
    }

    /*--------------------------[[ FUNCIONES DE ANDROID STUDIO ]]--------------------------*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Si volvemos de la activity de Settings
        if (requestCode == 2) {
            EditText txt_nickname = findViewById(R.id.txt_nickname);
            txt_nickname.setText(prefs.getString(getString(R.string.nickname_actual), ""));
            //Reiniciamos el MenuPrincipal para aplicar cambios en idioma o tema
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean mantenerSesion = prefs.getBoolean(getString(R.string.mantener_sesion), true);

        if (!mantenerSesion) {
            editorPreferencias.putString(getString(R.string.nickname_actual), "");
            editorPreferencias.commit();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(getString(R.string.txt_generadorAlias));
        getMenuInflater().inflate(R.menu.menucontextual_generadoralias, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int idItem = item.getItemId();
        String[] aliasMasculinos = {"PigPaddle", "Palpebral", "ParaEagle", "MiGrain", "HoleyMole", "Slaughter", "KookSpook", "Oxonomy", "KaboomView", "ZetanChamp", "TommyGun", "Belizard", "CatInHat", "MarchHare", "Marling", "Warlockk", "MorningDaw", "NumbLeg", "Dinotrex", "Capitulation"};
        String[] aliasFemeninos = {"SableCat", "Opally", "Incubus", "BatonRelay", "FlowerPwr", "Deadlight", "Quibble", "Emberglaze", "Minkx", "WrittenWod", "Obtusk", "MusicMiss", "Quern", "HarpyWitch", "Pheasant", "Mildewed", "Midgeabean", "Rhenus", "RobingHood", "NemeanLion"};
        String[] aliasAleatorios = {"Aexetan", "OculusVis", "Electricel", "Treasure", "SmokePumes", "RustSilver", "Pandora", "Ouster", "Scupperly", "Pharos", "RingRaid", "Sceptre", "Promenader", "Quern", "WillHuntin", "Outriggr", "Aexetan", "BellBoy", "Mortician", "Bullethart"};

        Random numRandom = new Random();
        int posicionAlias;

        if (idItem == R.id.generador_masculino) {
            posicionAlias = numRandom.nextInt(aliasMasculinos.length);
            txt_nickname.setText(aliasMasculinos[posicionAlias]);
            return true;
        } else if (idItem == R.id.generador_femenino) {
            posicionAlias = numRandom.nextInt(aliasFemeninos.length);
            txt_nickname.setText(aliasFemeninos[posicionAlias]);
            return true;
        } else if (idItem == R.id.generador_aleatorio) {
            posicionAlias = numRandom.nextInt(aliasAleatorios.length);
            txt_nickname.setText(aliasAleatorios[posicionAlias]);
            return true;
        }

        return super.onContextItemSelected(item);
    }
}
