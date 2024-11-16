package com.example.memory;

import android.app.UiModeManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.text.InputFilter;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Locale;

public class Settings extends PreferenceActivity {

    //Preferencias
    SharedPreferences.OnSharedPreferenceChangeListener listener;

    //BB.DD.
    BBDD miBBDD;
    SQLiteDatabase db;

    final Handler handler = new Handler();

    //Firebase
    FirebaseFirestore dbFirebase = FirebaseFirestore.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferencias);

        //Para mantener la pantalla del dispositivo encendida
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Recuperamos las preferencias
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Iniciamos el editor para modificar preferencias
        SharedPreferences.Editor editorPreferencias = prefs.edit();
        establecerTema(prefs);

        /*-------------[[ BB.DD ]]-------------*/
        miBBDD = new BBDD(this, getString(R.string.txt_bbddName), null, 1);
        db = miBBDD.getWritableDatabase();

        //Hacemos que el EditTextPreference de nickname_cambiadoe escriba siempre en mayúsculas
        EditText nickname_cambiado = ((EditTextPreference) findPreference(getString(R.string.nickname_cambiado))).getEditText();
        nickname_cambiado.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(10)});

        listener = (prefs1, key) -> {
            //Si se ha producido un cambio de nickname
            if (key.equals(getString(R.string.nickname_cambiado))) {
                if (!prefs1.getString(getString(R.string.nickname_actual), "").equals("") && !prefs1.getString(getString(R.string.nickname_cambiado), "").equals("")) {
                    Log.i("@@@SELENE", "Hay alias del menú, realizamos cambio en BB.DD. " + prefs1.getString(getString(R.string.nickname_cambiado), ""));
                    editorPreferencias.putString(getString(R.string.nickname_cambiado), prefs1.getString(getString(R.string.nickname_cambiado), ""));
                    editorPreferencias.commit();

                    //HACEMOS CAMBIO DE ALIAS EN BB.DD.
                    cambiarAliasBBDD(prefs, editorPreferencias);

                    //Vaciamos nickname cambiado
                    editorPreferencias.putString(getString(R.string.nickname_cambiado), "");
                    editorPreferencias.commit();
                    //setResult(RESULT_OK, menuPrincipal);
                } else if (prefs1.getString(getString(R.string.nickname_actual), "").equals("")) {
                    editorPreferencias.putString(getString(R.string.nickname_cambiado), "");
                    editorPreferencias.commit();
                    Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.necesitas_alias_cambio), Toast.LENGTH_LONG);
                    toast.show();
                }
                //Si se ha producido un cambio de idioma
            } else if (key.equals(getString(R.string.idioma_seleccionado))) {
                //El idioma del telefono tiene que estar en Español para que por defecto se cargue el español
                String idioma = prefs.getString(getString(R.string.idioma_seleccionado), "es");
                establecerIdioma(idioma);
                recreate();
            } else if (key.equals(getString(R.string.tema))) {
                establecerTema(prefs);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String idioma = prefs.getString(getString(R.string.idioma_seleccionado), "es");
                        establecerIdioma(idioma);
                        //Con el recreate() se ejecutan los métodos del ciclo de vida incluido el "onActivityResult()"
                        recreate();
                    }
                }, 5);
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    /*--------------------------[[ FUNCIONES ]]--------------------------*/
    private void establecerIdioma(String idioma) {
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void establecerTema(SharedPreferences prefs) {
        boolean temaOscuro = prefs.getBoolean(getString(R.string.tema), false);
        AppCompatDelegate.setDefaultNightMode(temaOscuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        SwitchPreference tema = (SwitchPreference) findPreference(getString(R.string.tema));
        tema.setTitle(temaOscuro ? getString(R.string.txt_temaDark) : getString(R.string.txt_temaLight));
        establecerTemaPreferencias(temaOscuro ? UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO);
    }

    //La 'PreferenceActivity' funciona de forma diferente y el cambio de tema se ha de hacer de esta forma
    private void establecerTemaPreferencias(int mode) {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (uiModeManager != null) {
            uiModeManager.setNightMode(mode);
        }
    }

    /*--------------------------[[ BASE DE DATOS ]]--------------------------*/
    private void cambiarAliasBBDD(SharedPreferences prefs, SharedPreferences.Editor editorPreferencias) {
        Cursor c1 = db.rawQuery("SELECT Nombre FROM Jugadores WHERE Nombre = '" + prefs.getString(getString(R.string.nickname_cambiado), "") + "'", null);

        //Si no existe ningun jugador con el alias al que se quiere cambiar
        if (c1 != null && !c1.moveToFirst()) {
            do {
                db.execSQL("UPDATE Jugadores SET Nombre = '" + prefs.getString(getString(R.string.nickname_cambiado), "") + "' WHERE Nombre = '" + prefs.getString(getString(R.string.nickname_actual), "") + "'");
                db.execSQL("UPDATE Puntuaciones SET NombreJugador = '" + prefs.getString(getString(R.string.nickname_cambiado), "") + "' WHERE NombreJugador = '" + prefs.getString(getString(R.string.nickname_actual), "") + "'");
                db.execSQL("UPDATE Partidas SET NombreJugador1 = '" + prefs.getString(getString(R.string.nickname_cambiado), "") + "' WHERE NombreJugador1 = '" + prefs.getString(getString(R.string.nickname_actual), "") + "'");
                db.execSQL("UPDATE Partidas SET NombreJugador2 = '" + prefs.getString(getString(R.string.nickname_cambiado), "") + "' WHERE NombreJugador2 = '" + prefs.getString(getString(R.string.nickname_actual), "") + "'");
                //HACEMOS CAMBIO DE ALIAS EN FIREBASE
                cambiarAliasEnFirebase(prefs.getString(getString(R.string.nickname_actual), ""), prefs.getString(getString(R.string.nickname_cambiado), ""), prefs, editorPreferencias);
            } while (c1.moveToNext());

        } else {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.alias_ya_existe), Toast.LENGTH_LONG);
            toast.show();
        }
        //Cerramos el cursor
        c1.close();
    }

    private void cambiarAliasEnFirebase(String aliasActual, String nuevoAlias, SharedPreferences prefs, SharedPreferences.Editor editorPreferencias) {
        // Realizar una consulta para verificar si ya existe un jugador con el nuevo alias
        dbFirebase.collection("jugadores")
                .whereEqualTo("Alias", nuevoAlias)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                // No hay jugadores con el nuevo alias
                                realizarActualizacionAlias(aliasActual, nuevoAlias, prefs, editorPreferencias);
                            } else {
                                // Ya existe un jugador con el nuevo alias
                                // Puedes mostrar un mensaje al usuario o realizar alguna acción adicional
                                Log.i("Firebase", "Ya existe un jugador con el nuevo alias: " + nuevoAlias);
                                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.alias_ya_existe), Toast.LENGTH_LONG);
                                toast.show();
                            }
                        } else {
                            // La consulta no se completó correctamente
                            Log.i("Firebase", "Error al realizar la consulta: ", task.getException());
                        }
                    }
                });
    }

    private void realizarActualizacionAlias(String aliasActual, String nuevoAlias, SharedPreferences prefs, SharedPreferences.Editor editorPreferencias) {
        // Realizar la actualización en Firebase
        dbFirebase.collection("jugadores")
                .whereEqualTo("Alias", aliasActual)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                // Actualizar el alias del jugador encontrado
                                String jugadorId = document.getId();
                                dbFirebase.collection("jugadores")
                                        .document(jugadorId)
                                        .update("Alias", nuevoAlias)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.i("Firebase", "Alias actualizado correctamente de '" + aliasActual + "' a '" + nuevoAlias + "'");
                                                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.cambio_alias_correcto), Toast.LENGTH_LONG);
                                                toast.show();
                                                //Nos guardamos el nickname cambiado como nickname actual
                                                editorPreferencias.putString(getString(R.string.nickname_actual), nuevoAlias);
                                                editorPreferencias.commit();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i("Firebase", "Error al actualizar el alias: ", e);
                                            }
                                        });
                            }
                        } else {
                            // La consulta no se completó correctamente
                            Log.i("Firebase", "Error al realizar la consulta: ", task.getException());
                        }
                    }
                });
    }

    /*--------------------------[[ FUNCIONES DE ANDROID STUDIO ]]--------------------------*/
    @Override
    protected void onPause() {
        super.onPause();
        //Desuscribimos el listener porque sino se acumula
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
