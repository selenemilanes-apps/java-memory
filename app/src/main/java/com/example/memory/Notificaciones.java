package com.example.memory;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

public class Notificaciones extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);
        createNotificationChannel();

        Button btn_guardar = findViewById(R.id.btn_guardar);
        DatePicker datePicker = findViewById(R.id.datePicker);
        TimePicker timePicker = findViewById(R.id.timePicker);

        // Ponemos el formato 24 horas en el TimePicker porque no se puede hacer desde el layout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setIs24HourView(true);
        } else {
            // Antes de la versión M, setIs24HourView no estaba disponible (se tendria que buscar como se hace para esas versiones
        }

        btn_guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), getString(R.string.recordatorio_guardado), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(Notificaciones.this, BroadcastNotificaciones.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(Notificaciones.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                // Obtener la fecha seleccionada en el DatePicker
                int year = datePicker.getYear();
                int month = datePicker.getMonth();
                int day = datePicker.getDayOfMonth();

                // Obtener la hora seleccionada en el TimePicker
                int hour, minute;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    hour = timePicker.getHour();
                    minute = timePicker.getMinute();
                } else {
                    // Antes de la versión M
                    hour = timePicker.getCurrentHour();
                    minute = timePicker.getCurrentMinute();
                }

                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day, hour, minute);

                long fechaHoraEnMillis = calendar.getTimeInMillis();

                alarmManager.set(AlarmManager.RTC_WAKEUP, fechaHoraEnMillis, pendingIntent);
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String nombre = getString(R.string.txt_nombreCanal);
            int importancia = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel canal = new NotificationChannel("notificacionMemory", nombre, importancia);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(canal);
        }
    }
}
