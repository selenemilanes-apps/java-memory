package com.example.memory;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundManager { // Controla los sfx de la aplicacion

    private Context pContext;
    private SoundPool sndPool;

    private float rate = 1.0f, masterVolume = 1.0f, leftVolume = 2.0f, rightVolume = 2.0f, balance = 0.5f;

    public SoundManager(Context appContext)
    {
        sndPool = new SoundPool(16, AudioManager.STREAM_MUSIC, 100);
        pContext = appContext;
    }

    // Recibe el id de la pista para que la elija de los archivos alojados físicamente en el proyecto
    // carga un sonido (load) y devuelve (return) la id
    public int load(int sound_id)
    {
        return sndPool.load(pContext, sound_id, 1);
    }

    // Nos permite reproducir la pista pasando como parámetro
    // el resto de los ajustes como sonido, balance y velocidad.
    public void play(int sound_id)
    {
        sndPool.play(sound_id, leftVolume, rightVolume, 1, 0, rate);
    }

    // Set volume values based on existing balance value
    public void setVolume(float vol)
    {
        masterVolume = vol;

        if(balance < 1.0f)
        {
            leftVolume = masterVolume;
            rightVolume = masterVolume * balance;
        }
        else
        {
            rightVolume = masterVolume;
            leftVolume = masterVolume * ( 2.0f - balance );
        }

    }

    /* SI QUISIERAMOS MODIFICAR LA VELOCIDAD
    public void setSpeed(float speed)
    {
        rate = speed;

        // Speed of zero is invalid
        if(rate < 0.01f)
            rate = 0.01f;

        // Speed has a maximum of 2.0
        if(rate > 2.0f)
            rate = 2.0f;
    }*/

    /* SI QUISIERAMOS MODIFICAR EL BALANCE
    public void setBalance(float balVal)
    {
        balance = balVal;

        // Recalculate volume levels
        setVolume(masterVolume);
    }*/

    // Nos permite liberar la memoria de todos los objetos SoundPool que ya no
    // son requeridos y evitar asi, que la aplicacion ralentice el telefono
    public void unloadAll()
    {
        sndPool.release();
    }
}
