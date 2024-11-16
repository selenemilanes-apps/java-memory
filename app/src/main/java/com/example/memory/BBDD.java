package com.example.memory;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BBDD extends SQLiteOpenHelper {

    // -------------------------- [ CREAMOS LAS SENTENCIAS QUE VAMOS A EJECUTAR ]  --------------------------//
    //Ponemos entre corchetes para poder poner espacio en el nombre de las columnas
    String sqlCreacionTablaJugadores = "CREATE TABLE Jugadores (IdJugador integer primary key autoincrement, " + " Nombre text not null);";
    String sqlCreacionTablaPuntuaciones = "CREATE TABLE Puntuaciones (NombreJugador text not null, " + " Puntuación int not null, " + " Fecha text not null);";
    String sqlCreacionTablaPartidas = "CREATE TABLE Partidas (NombreJugador1 text not null, " + " PuntuaciónJugador1 int not null, " + " NombreJugador2 text not null, " + " PuntuaciónJugador2 int not null, " + " Ganador text not null," + " Fecha text not null);";

    public BBDD(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sqlCreacionTablaJugadores);
        db.execSQL(sqlCreacionTablaPuntuaciones);
        db.execSQL(sqlCreacionTablaPartidas);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        //Solo se ejecuta cuando la versión de la BBDD con la que llamamos es superior a
        //la actual. Se tendria que hacer una migracion de datos.
        //Nosotros simplemente borraremos los datos antiguos y crearemos la nueva estructura.
        /*db.execSQL("DROP TABLE IF EXISTS Jugadores");
        db.execSQL("DROP TABLE IF EXISTS Puntuaciones");

        db.execSQL(sqlCreacionTablaJugadores);
        db.execSQL(sqlCreacionTablaPuntuaciones);*/
        // Tambien se podria hacer: onCreate(db);
    }
}
