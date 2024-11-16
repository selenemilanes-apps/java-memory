package com.example.memory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;

public class ScoreActivity extends AppCompatActivity {

    final ArrayList<Ranking> ranking = new ArrayList<Ranking>();
    AdaptadorRanking adaptador;
    FirebaseFirestore dbFirebase = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scores);

        //Para mantener la pantalla del dispositivo encendida
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        BBDD miBBDD = new BBDD(this, getString(R.string.txt_bbddName), null, 1);

        //Para obtener acceso de escritura a la BBDD
        SQLiteDatabase db = miBBDD.getWritableDatabase();


        //-------------------------- [ CONSULTA A LA BB.DD. ]  --------------------------//
       /* Cursor c1 = db.rawQuery("SELECT NombreJugador, Puntuación FROM Puntuaciones ORDER BY Puntuación DESC, Fecha ASC LIMIT 5", null);

        //Si c1 no es null y me puedo mover al primer registro (moveToFirst())
        if (c1 != null && c1.moveToFirst()) {
            do {
                String nombreJugador = c1.getString(0);
                int puntuacion = c1.getInt(1);

                ranking.add(new Ranking(0, nombreJugador, puntuacion));

            } while (c1.moveToNext());

        }
        //Cerramos el cursor
        c1.close();*/

        dbFirebase.collection("jugadores")
                .orderBy("Puntuación", Query.Direction.DESCENDING) // Ordena por puntuación de forma descendente
                .orderBy("Fecha", Query.Direction.ASCENDING) // Luego, ordena por fecha de forma ascendente
                .limit(5) // Limita los resultados a 5 registros
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // La consulta se completó exitosamente
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Maneja cada documento recuperado (jugador)
                                String nombreJugador = document.getString("Alias");
                                int puntuacion = document.getLong("Puntuación").intValue();

                                ranking.add(new Ranking(0, nombreJugador, puntuacion));

                                Log.i("@@RANKING", "Alias: " + nombreJugador + ", Puntuación: " + puntuacion);
                            }
                            adaptador.notifyDataSetChanged(); //Hay que notificar al adaptador que los datos han cambiado
                        } else {
                            // La consulta no se completó correctamente
                            Log.i("@@RANKING", "Error al realizar la consulta: ", task.getException());
                        }
                    }
                });

        ListView listView = (ListView) findViewById(R.id.listView);
        adaptador = new AdaptadorRanking(this);
        listView.setAdapter(adaptador);

        //-------------------------- [ CERRAMOS LA BB.DD. ]  --------------------------//
        //db.close();
    }

    class AdaptadorRanking extends ArrayAdapter {
        Activity context;

        public AdaptadorRanking(Activity context) {
            super(context, R.layout.activity_scores, ranking);
            this.context = (Activity) context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();
            View item = inflater.inflate(R.layout.activity_listview, null);

            // OJOOOO!!!!! Hacemos el findViewById del item que tenemos inflado (item.findViewById).
            TextView txt_Puesto = (TextView) item.findViewById(R.id.txt_rank);
            TextView txt_NombreJugador = (TextView) item.findViewById(R.id.txt_name);
            TextView txt_Puntuacion = (TextView) item.findViewById(R.id.txt_score);

            txt_Puesto.setText(String.valueOf(position + 1));
            txt_NombreJugador.setText(ranking.get(position).getNombreJugador());
            txt_Puntuacion.setText(String.valueOf(ranking.get(position).getPuntuacion()));

            //Poner los top3 de colores diferentes (1st naranja, 2nd i 3rd azules)
            switch (position) {
                case 0:
                    txt_Puesto.setTextColor(ContextCompat.getColor(context, R.color.orange));
                    txt_NombreJugador.setTextColor(ContextCompat.getColor(context, R.color.orange));
                    txt_Puntuacion.setTextColor(ContextCompat.getColor(context, R.color.orange));
                    break;
                case 1:
                    txt_Puesto.setTextColor(ContextCompat.getColor(context, R.color.lightBlue));
                    txt_NombreJugador.setTextColor(ContextCompat.getColor(context, R.color.lightBlue));
                    txt_Puntuacion.setTextColor(ContextCompat.getColor(context, R.color.lightBlue));
                    break;
                case 2:
                    txt_Puesto.setTextColor(ContextCompat.getColor(context, R.color.lightBlue));
                    txt_NombreJugador.setTextColor(ContextCompat.getColor(context, R.color.lightBlue));
                    txt_Puntuacion.setTextColor(ContextCompat.getColor(context, R.color.lightBlue));
                    break;
            }

            return (item);
        }
    }
}