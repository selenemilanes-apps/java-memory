package com.example.memory;

public class Ranking {

    private int puesto;
    private String nombreJugador;
    private int puntuacion;

    public Ranking(int puesto, String nombreJugador, int puntuacion) {
        this.puesto = puesto;
        this.nombreJugador = nombreJugador;
        this.puntuacion = puntuacion;
    }

    public String getNombreJugador() { return nombreJugador; }
    public int getPuntuacion() { return puntuacion; }

}
