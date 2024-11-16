package com.example.memory;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;

public class MiMediaPlayer { //Controla la musica general de la aplicacion

    static MediaPlayer mediaPlayer;

    private static MiMediaPlayer miMediaPlayer = new MiMediaPlayer();

    private Context appContext;

    private String songPlaying = "";
    private boolean muteado = false;

    public MiMediaPlayer() {
    }

    public Context get() {
        return getInstance().getContext();
    }

    public static synchronized MiMediaPlayer getInstance() {
        return miMediaPlayer;
    }

    public void init(Context context) {
        if (appContext == null) {
            this.appContext = context;
        }
    }

    private Context getContext() {
        return appContext;
    }

    public MediaPlayer getSingletonMedia() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(get(), R.raw.menusong);
        }
        mediaPlayer.setLooping(true);
        return mediaPlayer;
    }

    public void startMusic() {
        mediaPlayer.start();
    }

    public void pauseMusic() {
        mediaPlayer.pause();
    }

    public boolean isMuteado() {
        return muteado;
    }

    public void setMuteado(boolean muteado) {
        this.muteado = muteado;
    }

    public void cambiarCancion(String nombreCancion) throws IOException {
        if (isMuteado()) return;

        switch (nombreCancion) {
            case "MenuPrincipal":
                ponemosCancion(nombreCancion, R.raw.menusong);
                break;
            case "Juego":
                ponemosCancion(nombreCancion, R.raw.pikmin);
                break;
        }
    }

    public void ponemosCancion(String nombreCancion, int song) throws IOException {
        if (songPlaying.equals(nombreCancion)) return;
        mediaPlayer.reset();
        songPlaying = nombreCancion;
        mediaPlayer.setDataSource(get(), getURLForResource(song));
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    public Uri getURLForResource(int resourceId) {
        return Uri.parse("android.resource://" + R.class.getPackage().getName() + "/" + resourceId);
    }
}
