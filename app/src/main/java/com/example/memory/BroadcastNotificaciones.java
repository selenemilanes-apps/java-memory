package com.example.memory;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;

public class BroadcastNotificaciones extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String idioma = prefs.getString(context.getString(R.string.idioma_seleccionado), "es");

        // Establecer el idioma en el contexto de la notificación
        Context nuevoContexto = establecerIdioma(context, idioma);

        Intent abrirAppIntent = new Intent(nuevoContexto, MenuPrincipal.class);
        abrirAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(nuevoContexto, 0, abrirAppIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notificacionMemory")
                .setSmallIcon(R.drawable.notifications)
                .setContentTitle(nuevoContexto.getString(R.string.txt_notificacionTitulo))
                .setContentText(nuevoContexto.getString(R.string.txt_notificacionContenido))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); //La notificacion se cerrara automaticamente cuando el usuario toque la notificacion

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(nuevoContexto);

        //Esto verifica si nuestra aplicacion tiene el permiso 'POST_NOTIFICATIONS', si no tiene el permiso concedido, el codigo comentado solicitara ese permiso
        if (ActivityCompat.checkSelfPermission(nuevoContexto, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private Context establecerIdioma(Context context, String idioma) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
}
