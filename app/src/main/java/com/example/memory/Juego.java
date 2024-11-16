package com.example.memory;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class Juego extends AppCompatActivity {

    //Sonido
    MiMediaPlayer miMediaPlayer;
    SoundManager snd;
    int error, acierto;

    //Players y puntuaciones
    int puntuacionPlayer1, puntuacionPlayer2, racha;
    TextView txt_nicknameP1, txt_puntuacionPlayer1, txt_nicknameP2, txt_puntuacionPlayer2, txt_racha;

    //Cartas
    ImageButton ib01, ib02, ib03, ib04, ib05, ib06, ib07, ib08, ib09, ib10, ib11, ib12, ib13, ib14, ib15, ib16;
    ImageButton[] tablero = new ImageButton[16];
    ArrayList<Integer> listaImagenes = new ArrayList<>();
    ArrayList<Integer> listaImagenesDesordenada = new ArrayList<>();
    ArrayList<Integer> listaImagenesDesordenada2 = new ArrayList<>();
    int backCard;

    //Partida
    int puntosParejaCorrecta;
    boolean parejaGirada;
    ImageButton primeraCarta, segundaCarta;
    int idPrimeraCarta, idSegundaCarta, cantidadParejasAcertadas;
    final Handler handler = new Handler();

    //Buttons
    Button btn_salir;

    //BB.DD.
    BBDD miBBDD;
    SQLiteDatabase db;

    //Firebase
    ProgressDialog progressDialog;
    FirebaseFirestore dbFirebase = FirebaseFirestore.getInstance();
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://memoryselene-default-rtdb.firebaseio.com/");
    private String playerUniqueId = "0", opponentUniqueId = "0";
    private boolean opponentFound = false;
    private String status = "matching";
    private String playerTurn = "";
    // Id de conexion en el que el player se ha unido para jugar
    private String connectionId = "";
    //Listeners
    ValueEventListener cardsEventListener, playedCardsEventListener, finalPlayedCardsEventListener, wonEventListener, turnsEventListener, cantidadAciertosEventListener, desconexionEventListener;
    ArrayList<Integer> listaCartasGiradas = new ArrayList<>();
    ArrayList<Integer> listaCartasTemporalmenteGiradas = new ArrayList<>();
    ArrayList<Integer> listaCartasTemporalmenteGiradasPorContrincante = new ArrayList<>();
    ArrayList<Integer> listaCartasGiradasPorContrincante = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego);

        miMediaPlayer = MiMediaPlayer.getInstance();
        // Creamos una instancia de nuestro sound manger y cargamos los sonidos
        snd = new SoundManager(getApplicationContext());
        error = snd.load(R.raw.error);
        acierto = snd.load(R.raw.cutemonster);

        try {
            miMediaPlayer.cambiarCancion("Juego");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Para mantener la pantalla del dispositivo encendida
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*-------------[[ BB.DD ]]-------------*/
        miBBDD = new BBDD(this, getString(R.string.txt_bbddName), null, 1);
        db = miBBDD.getWritableDatabase();

        initFirebase();
    }

    private void initStaticValues() {
        playerUniqueId = "0";
        opponentUniqueId = "0";
        opponentFound = false;
        status = "matching";
        playerTurn = "";
        connectionId = "";
        listaCartasGiradas.clear();
        listaCartasTemporalmenteGiradas.clear();
        listaCartasTemporalmenteGiradasPorContrincante.clear();
        listaCartasGiradasPorContrincante.clear();
    }

    private void initValues() {
        initTablero();
        initImages();
        initButtons();
    }

    private void initFirebase() {
        initStaticValues();
        initPlayers();
        mostrarProgressDialog(true, getString(R.string.txt_esperandoContrincante));

        //Generamos un id unico para el player
        playerUniqueId = String.valueOf(System.currentTimeMillis());

        // Creamos el hijo 'connections' en el databaseReference y escuchamos si cambia (si alguien entra)
        databaseReference.child("connections").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChildren()) {
                    Log.i("##FIREBASE", "Entra en opponent not found");
                    //Comprobamos si hay otros usuarios en la firebase realtime database
                    if (!opponentFound) {
                        Log.i("##FIREBASE", "Entra en Snapshot has children");
                        //Miramos en cada conexion a ver si hay algun usuario esperando para jugar
                        for (DataSnapshot connections : snapshot.getChildren()) {
                            //Guardamos la id unica de la conexion
                            String conId = connections.getKey();
                            Log.i("##FIREBASE", "La conId es: " + conId);
                            //Se requieren dos jugadores para jugar
                            int getPlayersCount = (int) connections.getChildrenCount();
                            Log.i("##FIREBASE", "getPlayersCount: " + getPlayersCount);

                            //Despues de crear una nueva conexion, esperando a que otro jugador se una
                            Log.i("##FIREBASE", "status: " + status);
                            if (status.equals("waiting")) {
                                Log.i("##FIREBASE", "Entra en status es WAITING");
                                //Si getPlayersCount es 2 significa que otro jugador se ha unido
                                if (getPlayersCount == 2) {
                                    Log.i("##FIREBASE", "Entra en HAY DOS PLAYERS");

                                    playerTurn = playerUniqueId;
                                    applyPlayerTurn(playerTurn);

                                    //True cuando encontramos un contrincante con quien jugar
                                    boolean playerFound = false;
                                    Log.i("##FIREBASE", "playerFound: " + playerFound);

                                    //Obteniendo los players de la conexion
                                    for (DataSnapshot players : connections.getChildren()) {
                                        String getPlayerUniqueId = players.getKey();
                                        Log.i("##FIREBASE", "getPlayerUniqueId: " + getPlayerUniqueId);

                                        //Comprobamos si el id del jugador encontrado es el mismo
                                        // id que el del usuario que creo la conexion
                                        if (getPlayerUniqueId.equals(playerUniqueId)) {
                                            playerFound = true;
                                            Log.i("##FIREBASE", "playerFound: " + playerFound);
                                        } else if (playerFound) {
                                            Log.i("##FIREBASE", "Entra en PLAYER FOUND");
                                            String getOpponentPlayerName = players.child("player_name").getValue(String.class);
                                            opponentUniqueId = players.getKey();
                                            Log.i("##FIREBASE", "getOpponentPlayerName: " + getOpponentPlayerName);
                                            Log.i("##FIREBASE", "playerFound: " + opponentUniqueId);

                                            //**Ponemos el nombre en el textview del P2**//
                                            txt_nicknameP2.setText(getOpponentPlayerName);
                                            connectionId = conId;
                                            Log.i("##FIREBASE", "connectionId: " + connectionId);

                                            opponentFound = true;
                                            Log.i("##FIREBASE", "opponentFound: " + opponentFound);

                                            //Añadiendo listeners
                                            databaseReference.child("cards").child(connectionId).addValueEventListener(cardsEventListener);
                                            databaseReference.child("playedCards").child(connectionId).addValueEventListener(playedCardsEventListener);
                                            databaseReference.child("finalPlayedCards").child(connectionId).addValueEventListener(finalPlayedCardsEventListener);
                                            databaseReference.child("won").child(connectionId).addValueEventListener(wonEventListener);
                                            databaseReference.child("turns").child(connectionId).addValueEventListener(turnsEventListener);
                                            databaseReference.child("cantidadParejasAcertadas").child(connectionId).addValueEventListener(cantidadAciertosEventListener);
                                            databaseReference.child("desconexion").child(connectionId).addValueEventListener(desconexionEventListener);
                                            databaseReference.child("desconexion").child(connectionId).child(playerUniqueId).setValue(playerUniqueId);
                                            databaseReference.child("desconexion").child(connectionId).child(playerUniqueId).setValue(opponentUniqueId);
                                            initValues();
                                            initJuego();

                                            //Ocultamos el progress dialog si se estaba mostrando
                                            if (progressDialog.isShowing()) {
                                                progressDialog.dismiss();
                                            }

                                            //Una vez se ha realizado la conexion, eliminamos los listeners de la conexion del Database Reference
                                            databaseReference.child("connections").removeEventListener(this);

                                            break;
                                        }
                                    }
                                }
                            }
                            //En el caso de que el usuario no haya creado la conexion porque ya hay otras conexiones disponibles
                            else {
                                //Miramos si la conexion tiene 1 jugador y necesita a otro para jugar
                                if (getPlayersCount == 1) {
                                    Log.i("##FIREBASE", "Entra en LA CONEXION TIENE 1 JUGADOR");
                                    //Añadimos al player a la conexion
                                    connections.child(playerUniqueId).child("player_name").getRef().setValue(txt_nicknameP1.getText());
                                    status = "matching";

                                    //Obtenemos los dos jugadores
                                    for (DataSnapshot players : connections.getChildren()) {
                                        String getOpponentName = players.child("player_name").getValue(String.class);
                                        opponentUniqueId = players.getKey();

                                        //El primer turno sera de quien creo la conexion
                                        playerTurn = opponentUniqueId;
                                        applyPlayerTurn(playerTurn);

                                        txt_nicknameP2.setText(getOpponentName);
                                        connectionId = conId;
                                        opponentFound = true;

                                        //Añadiendo listeners
                                        databaseReference.child("cards").child(connectionId).addValueEventListener(cardsEventListener);
                                        databaseReference.child("playedCards").child(connectionId).addValueEventListener(playedCardsEventListener);
                                        databaseReference.child("finalPlayedCards").child(connectionId).addValueEventListener(finalPlayedCardsEventListener);
                                        databaseReference.child("won").child(connectionId).addValueEventListener(wonEventListener);
                                        databaseReference.child("turns").child(connectionId).addValueEventListener(turnsEventListener);
                                        databaseReference.child("cantidadParejasAcertadas").child(connectionId).addValueEventListener(cantidadAciertosEventListener);
                                        databaseReference.child("desconexion").child(connectionId).addValueEventListener(desconexionEventListener);
                                        databaseReference.child("desconexion").child(connectionId).child(playerUniqueId).setValue(playerUniqueId);
                                        databaseReference.child("desconexion").child(connectionId).child(playerUniqueId).setValue(opponentUniqueId);
                                        initValues();
                                        initJuego();

                                        //Ocultamos el progress dialog si se estaba mostrando
                                        if (progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                        }

                                        //Una vez se ha realizado la conexion, eliminamos los listeners de la conexion del Database Reference
                                        databaseReference.child("connections").removeEventListener(this);

                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                //Si no hay conexion disponible en la firebase database, entonces creamos una nueva
                // Es como crear una sala y esperar que se unan otros jugadores
                else {
                    //Generamos una id unica para la conexion
                    String connectionUniqueId = String.valueOf(System.currentTimeMillis());
                    Log.i("##FIREBASE", "SE CREA UNA CONEXIÓN");
                    //"Snapshot" hace referencia a donde estamos ahora mismo que es 'connections'
                    // Creamos un hijo en 'connections' con la connectionUniqueId y como hijo pondremos al player con el valor "txt_nicknameP1"
                    //Añadimos el primer jugador a la conexion, el cual esperara a que otro se conecte
                    snapshot.child(connectionUniqueId).child(playerUniqueId).child("player_name").getRef().setValue(txt_nicknameP1.getText());
                    initValues();
                    initDesordenarImagenes(connectionUniqueId);
                    status = "waiting";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        cardsEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Obtenemos todos los turnos de las conexiones
                Log.i("##FIREBASE", "ENTRA EN CARDSEVENTLISTENER");
                Log.i("##FIREBASE", "snapsotgetkey: " + snapshot.getKey());

                if (snapshot.getKey().equals(connectionId)) {
                    if (snapshot.hasChild("images")) {
                        Log.i("##FIREBASE", "snapsot tiene images (valor): " + snapshot.child("images").getValue());
                        GenericTypeIndicator<ArrayList<Integer>> genericTypeIndicator = new GenericTypeIndicator<ArrayList<Integer>>() {
                        };
                        listaImagenesDesordenada2 = snapshot.child("images").getValue(genericTypeIndicator);
                        initImagenesEnTablero();
                        Log.i("##FIREBASE", "tenemos listadesordenada2: " + listaImagenesDesordenada2);
                    }
                }
                Log.i("##FIREBASE", "snapsotgetvalue: " + snapshot.getValue());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        playedCardsEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.getKey().equals(connectionId)) {
                        Log.i("##FIREBASE", "conex es igual: " + snapshot.getKey());
                        for (DataSnapshot connection : snapshot.getChildren()) {
                            Log.i("##FIREBASE", "snapshot tiene children: " + connection.getKey());
                            if (connection.getKey().equals(opponentUniqueId)) {
                                if (connection.hasChild("played_cards")) {
                                    Log.i("##FIREBASE", "entra en playedcards: " + connection.child("played_cards").getValue());
                                    if (connection.getChildrenCount() > 0) {
                                        for (DataSnapshot posicion : connection.getChildren()) {
                                            GenericTypeIndicator<ArrayList<Integer>> genericTypeIndicator = new GenericTypeIndicator<ArrayList<Integer>>() {
                                            };
                                            ArrayList<Integer> cartasTemporalmenteGiradas = posicion.getValue(genericTypeIndicator);
                                            Log.i("##FIREBASE", "LISTACARTASGIRDS (temporalmente): " + cartasTemporalmenteGiradas);
                                            listaCartasTemporalmenteGiradasPorContrincante = cartasTemporalmenteGiradas;
                                            Log.i("## FIREBASE", "seteamos listacartastemporalmentegiradasporcontrincante: " + listaCartasTemporalmenteGiradasPorContrincante);

                                            girarCartas(cartasTemporalmenteGiradas);
                                        }
                                    }
                                } else {
                                    Log.i("## FIREBASE", "connection no tiene el hijo 'played_cards': " + listaCartasTemporalmenteGiradasPorContrincante);
                                    if (listaCartasTemporalmenteGiradasPorContrincante.size() > 0) {
                                        for (Integer posicion : listaCartasTemporalmenteGiradasPorContrincante) {
                                            tablero[posicion].setScaleType(ImageView.ScaleType.CENTER_CROP);
                                            tablero[posicion].setImageResource(backCard);
                                        }
                                        listaCartasTemporalmenteGiradasPorContrincante.clear();
                                        Log.i("## FIREBASE", "LIMPIAMOS LISTA CONTRINCANTE: " + listaCartasTemporalmenteGiradasPorContrincante);
                                    }
                                }

                            } else {
                                if (listaCartasTemporalmenteGiradasPorContrincante.size() > 0) {
                                    for (Integer posicion : listaCartasTemporalmenteGiradasPorContrincante) {
                                        tablero[posicion].setScaleType(ImageView.ScaleType.CENTER_CROP);
                                        tablero[posicion].setImageResource(backCard);
                                    }
                                    listaCartasTemporalmenteGiradasPorContrincante.clear();
                                    Log.i("## FIREBASE", "LIMPIAMOS LISTA CONTRINCANTE: " + listaCartasTemporalmenteGiradasPorContrincante);
                                }
                            }
                        }
                    }
                } else {
                    Log.i("## FIREBASE", "no existe snapshot: " + listaCartasTemporalmenteGiradasPorContrincante);
                    if (listaCartasTemporalmenteGiradasPorContrincante.size() > 0) {
                        for (Integer posicion : listaCartasTemporalmenteGiradasPorContrincante) {
                            tablero[posicion].setScaleType(ImageView.ScaleType.CENTER_CROP);
                            tablero[posicion].setImageResource(backCard);
                        }
                        listaCartasTemporalmenteGiradasPorContrincante.clear();
                        Log.i("## FIREBASE", "LIMPIAMOS LISTA CONTRINCANTE: " + listaCartasTemporalmenteGiradasPorContrincante);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        finalPlayedCardsEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    if (snapshot.getKey().equals(connectionId)) {
                        Log.i("##FIREBASE", "conex es igual: " + snapshot.getKey());
                        for (DataSnapshot connection : snapshot.getChildren()) {
                            Log.i("##FIREBASE", "snapshot tiene children: " + connection.getKey());
                            if (connection.getKey().equals(opponentUniqueId)) {
                                if (connection.hasChild("final_played_cards")) {
                                    Log.i("##FIREBASE", "connection tiene el hijo 'final_played_cards': " + connection.child("final_played_cards").getValue());
                                    if (connection.getChildrenCount() > 0) {
                                        for (DataSnapshot posicion : connection.getChildren()) {
                                            GenericTypeIndicator<ArrayList<Integer>> genericTypeIndicator = new GenericTypeIndicator<ArrayList<Integer>>() {
                                            };
                                            ArrayList<Integer> cartasGiradas = posicion.getValue(genericTypeIndicator);
                                            Log.i("##FIREBASE", "LISTACARTASGIRDS: " + cartasGiradas);
                                            listaCartasGiradasPorContrincante = cartasGiradas;
                                            Log.i("## FIREBASE", "seteamos listacartasgiradasporcontrincante: " + listaCartasGiradasPorContrincante);

                                            girarCartas(cartasGiradas);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        turnsEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Si estamos en nuestra conexión (si es nuestra partida)
                if (snapshot.getKey().equals(connectionId)) {

                    if (snapshot.hasChild("player_points")) {
                        if (playerTurn.equals(playerUniqueId)) {
                            puntuacionPlayer1 = snapshot.child("player_points").getValue(Integer.class);
                            txt_puntuacionPlayer1.setText(getString(R.string.txt_puntuacion) + " " + puntuacionPlayer1);
                            databaseReference.child("turns").child(connectionId).removeValue();

                        } else if (playerTurn.equals(opponentUniqueId)) {
                            puntuacionPlayer2 = snapshot.child("player_points").getValue(Integer.class);
                            txt_puntuacionPlayer2.setText(getString(R.string.txt_puntuacion) + " " + puntuacionPlayer2);
                            databaseReference.child("turns").child(connectionId).removeValue();
                        }
                    }
                    if (snapshot.hasChild("player_id")) {
                        playerTurn = snapshot.child("player_id").getValue(String.class);
                        applyPlayerTurn(playerTurn);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        cantidadAciertosEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.i("##FIN PARTIDA", "entra en cantidadAciertosListener: " + snapshot.getKey());
                if (snapshot.getKey().equals(connectionId)) {
                    if (snapshot.hasChild("cantidad_parejas_acertadas")) {
                        Log.i("##FIN PARTIDA", "valor de la conexion (cantidadaciertoslistener): " + snapshot.child("cantidad_parejas_acertadas").getValue(Integer.class));
                        cantidadParejasAcertadas = snapshot.child("cantidad_parejas_acertadas").getValue(Integer.class);

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                comprobarFinPartida();
                            }
                        }, 100);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        wonEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.hasChild("player_id")) {
                    String idGanador = snapshot.child("player_id").getValue(String.class);
                    if (idGanador.equals(playerUniqueId)) { //Ganador player 1
                        mostrarAlertDialog("\n " + getString(R.string.txt_jugarMensaje), getString(R.string.txt_ganadorTitulo), R.drawable.trophy, false, getString(R.string.txt_confirmar), getString(R.string.btn_doPositive_jugar), getString(R.string.txt_negar), getString(R.string.btn_doNegative_noJugar));
                    } else if (idGanador.equals(opponentUniqueId)) { //Ganador player 2
                        mostrarAlertDialog("\n " + getString(R.string.txt_jugarMensaje), getString(R.string.txt_perdedorTitulo), R.drawable.skull, false, getString(R.string.txt_confirmar), getString(R.string.btn_doPositive_jugar), getString(R.string.txt_negar), getString(R.string.btn_doNegative_noJugar));
                    } else if (idGanador.equals(getString(R.string.txt_empate))) { //Empate
                        mostrarAlertDialog("\n " + getString(R.string.txt_jugarMensaje), getString(R.string.txt_empateTitulo), R.drawable.empate, false, getString(R.string.txt_confirmar), getString(R.string.btn_doPositive_jugar), getString(R.string.txt_negar), getString(R.string.btn_doNegative_noJugar));
                    }

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            databaseReference.child("cards").child(connectionId).removeEventListener(cardsEventListener);
                            databaseReference.child("playedCards").child(connectionId).removeEventListener(playedCardsEventListener);
                            databaseReference.child("finalPlayedCards").child(connectionId).removeEventListener(finalPlayedCardsEventListener);
                            databaseReference.child("won").child(connectionId).removeEventListener(wonEventListener);
                            databaseReference.child("turns").child(connectionId).removeEventListener(turnsEventListener);
                            databaseReference.child("cantidadParejasAcertadas").child(connectionId).removeEventListener(cantidadAciertosEventListener);
                        }
                    }, 100);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        desconexionEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getKey().equals(connectionId)) {
                    Log.i("##DESCONEXION ", "snapshot childrencount " + snapshot.getChildrenCount());
                    Log.i("##DESCONEXION ", "snapshot key " + snapshot.getKey());
                    Log.i("##DESCONEXION ", "snapshot val " + snapshot.getValue());

                    if (snapshot.getChildrenCount() == 2) {
                        for (DataSnapshot players : snapshot.getChildren()) {
                            Log.i("##DESCONEXION ", "player key " + players.getKey());
                            Log.i("##DESCONEXION ", "player value " + players.getValue());
                            if (players.getKey().equals(opponentUniqueId) && players.getValue().equals("desconexion") && !isFinishing()) { //Si se ha desconectado el oponente
                                Log.i("##DESCONEXION ", "EL OPONENTE SE HA DESCONECTADO");
                                mostrarProgressDialog(false, getString(R.string.txt_oponenteSalePartida));

                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                        databaseReference.child("desconexion").child(connectionId).removeEventListener(desconexionEventListener);
                                        databaseReference.child("desconexion").child(connectionId).removeValue();
                                        eliminarValuesFirebase();
                                        finish();
                                    }
                                }, 3000);
                            }
                        }
                    } else if (snapshot.getChildrenCount() == 0 && !isFinishing()) {
                        Log.i("##DESCONEXION ", "Cierre forzoso");
                        mostrarProgressDialog(false, getString(R.string.txt_cierreConexion));

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                databaseReference.child("desconexion").child(connectionId).removeEventListener(desconexionEventListener);
                                databaseReference.child("desconexion").child(connectionId).removeValue();
                                eliminarValuesFirebase();
                                finish();
                            }
                        }, 3000);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        databaseReference.child("desconexion").onDisconnect().setValue("desconexion");
    }

    private void girarCartas(ArrayList<Integer> cartasGiradas) {
        for (Integer posicion : cartasGiradas) {
            tablero[posicion].setScaleType(ImageView.ScaleType.CENTER_CROP);
            tablero[posicion].setImageResource(listaImagenes.get(listaImagenesDesordenada2.get(posicion)));
        }
    }

    private void applyPlayerTurn(String playerUniqueId2) {
        Log.i("##FIREBASE", "ENTRA EN APPLYPLAYERTURN");
        if (playerUniqueId2.equals(playerUniqueId)) {
            Log.i("##FIREBASE", "ENTRA EN PLAYERUNIQUEID Y ES EL TURNO DE" + playerTurn);
            txt_nicknameP1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            txt_puntuacionPlayer1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            txt_nicknameP1.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));
            txt_puntuacionPlayer1.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));

            txt_nicknameP2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            txt_puntuacionPlayer2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            txt_nicknameP2.setTextColor(ContextCompat.getColor(this, R.color.grey));
            txt_puntuacionPlayer2.setTextColor(ContextCompat.getColor(this, R.color.grey));

        } else if (playerUniqueId2.equals(opponentUniqueId)) {
            Log.i("##FIREBASE", "ENTRA EN OPPONENTPLAYERID Y ES EL TURNO DE" + playerTurn);
            txt_nicknameP2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            txt_puntuacionPlayer2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            txt_nicknameP2.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));
            txt_puntuacionPlayer2.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));

            txt_nicknameP1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            txt_puntuacionPlayer1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            txt_nicknameP1.setTextColor(ContextCompat.getColor(this, R.color.grey));
            txt_puntuacionPlayer1.setTextColor(ContextCompat.getColor(this, R.color.grey));
        }
    }

    private void mostrarProgressDialog(boolean cancelable, String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(cancelable);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void initPlayers() {
        //Inicializamos valores
        racha = 0;
        puntuacionPlayer1 = 0;
        puntuacionPlayer2 = 0;

        //Ponemos los textos
        txt_nicknameP1 = findViewById(R.id.txt_nicknamePlayer1);
        txt_puntuacionPlayer1 = (TextView) findViewById(R.id.txt_puntuacionPlayer1);

        txt_nicknameP2 = findViewById(R.id.txt_nicknamePlayer2);
        txt_puntuacionPlayer2 = (TextView) findViewById(R.id.txt_puntuacionPlayer2);

        txt_puntuacionPlayer1.setText(getString(R.string.txt_puntuacion) + " " + puntuacionPlayer1);
        txt_puntuacionPlayer2.setText(getString(R.string.txt_puntuacion) + " " + puntuacionPlayer2);

        txt_racha = findViewById(R.id.txt_racha);
        txt_racha.setText(getString(R.string.txt_racha) + " " + racha);

        //Recuperamos el nickname para ponerlo en la parte superior izquierda
        Bundle extrasDelMenu = this.getIntent().getExtras();
        txt_nicknameP1.setText(extrasDelMenu.getString(getString(R.string.key_alias)));
        txt_nicknameP2.setText("");
    }

    private void initTablero() {
        //Cogemos los image button del view
        ib01 = findViewById(R.id.imageButton01);
        ib02 = findViewById(R.id.imageButton02);
        ib03 = findViewById(R.id.imageButton03);
        ib04 = findViewById(R.id.imageButton04);
        ib05 = findViewById(R.id.imageButton05);
        ib06 = findViewById(R.id.imageButton06);
        ib07 = findViewById(R.id.imageButton07);
        ib08 = findViewById(R.id.imageButton08);
        ib09 = findViewById(R.id.imageButton09);
        ib10 = findViewById(R.id.imageButton10);
        ib11 = findViewById(R.id.imageButton11);
        ib12 = findViewById(R.id.imageButton12);
        ib13 = findViewById(R.id.imageButton13);
        ib14 = findViewById(R.id.imageButton14);
        ib15 = findViewById(R.id.imageButton15);
        ib16 = findViewById(R.id.imageButton16);

        //Ponemos cada image button en el tablero
        tablero[0] = ib01;
        tablero[1] = ib02;
        tablero[2] = ib03;
        tablero[3] = ib04;
        tablero[4] = ib05;
        tablero[5] = ib06;
        tablero[6] = ib07;
        tablero[7] = ib08;
        tablero[8] = ib09;
        tablero[9] = ib10;
        tablero[10] = ib11;
        tablero[11] = ib12;
        tablero[12] = ib13;
        tablero[13] = ib14;
        tablero[14] = ib15;
        tablero[15] = ib16;
    }

    private void initImages() {
        //Añadimos las imagenes a la lista
        if (listaImagenes.size() != 8) {
            Log.i("##FIN PARTIDA", "ENTRA EN INITIMAGES");
            listaImagenes.add(R.drawable.monster01);
            listaImagenes.add(R.drawable.monster02);
            listaImagenes.add(R.drawable.monster03);
            listaImagenes.add(R.drawable.monster04);
            listaImagenes.add(R.drawable.monster05);
            listaImagenes.add(R.drawable.monster06);
            listaImagenes.add(R.drawable.monster07);
            listaImagenes.add(R.drawable.monster08);
        }

        backCard = R.drawable.backcard;

        //Inicializamos valores
        primeraCarta = null;
        segundaCarta = null;
        idPrimeraCarta = -1;
        idSegundaCarta = -1;
        cantidadParejasAcertadas = 0;
        puntosParejaCorrecta = 2;
        parejaGirada = false;
    }

    private void initDesordenarImagenes(String connectionUniqueId) {
        for (int i = 0; i < listaImagenes.size() * 2; i++) {
            listaImagenesDesordenada.add(i % listaImagenes.size());
        }

        Collections.shuffle(listaImagenesDesordenada);
        Log.i("##FIREBASE LISTAIMAGENES: ", String.valueOf(listaImagenes));
        Log.i("##FIREBASE LISTADESORDENADA: ", String.valueOf(listaImagenesDesordenada));

        databaseReference.child("cards").child(connectionUniqueId).child("images").setValue(listaImagenesDesordenada);
    }

    private void initImagenesEnTablero() {
        //Añadimos imagenes aleatoriamente al tablero
        Log.i("##FIREBASE: ", "ENTRA EN initImagenesEnTablero");
        for (int i = 0; i < tablero.length; i++) {
            tablero[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
            tablero[i].setImageResource(listaImagenes.get(listaImagenesDesordenada2.get(i)));
        }
    }

    private void initButtons() {
        btn_salir = findViewById(R.id.btn_salir);
        btn_salir.setVisibility(View.VISIBLE);

        btn_salir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mostrarAlertDialog(getString(R.string.txt_salirMensaje), getString(R.string.txt_salirTitulo), R.drawable.warning, false, getString(R.string.txt_confirmar), getString(R.string.btn_doPositive_finalizar), getString(R.string.txt_negar), getString(R.string.btn_doNegative_cancelar));
            }
        });
    }

    private void initJuego() {
        //Vemos las cartas durante unos segundos
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < tablero.length; i++) {
                    tablero[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
                    tablero[i].setImageResource(backCard); //Le ponemos el fondo
                }
            }
        }, 3000);

        //Activamos todos los botones del tablero y le añadimos el listener
        for (int i = 0; i < tablero.length; i++) {
            final int posicion = i;
            tablero[i].setEnabled(true);
            tablero[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i("##FIREBASE : ", "CLICO EN UNA IMAGEN Y EL TURNO ES DE: " + playerTurn);
                    Log.i("##FIREBASE : ", "PAREJA GIRADA: " + parejaGirada);
                    if (!parejaGirada && playerTurn.equals(playerUniqueId)) {
                        Log.i("##FIREBASE : ", "ENTRO EN comprobarpareja");
                        comprobarPareja(posicion, tablero[posicion]);
                    }
                }
            });
        }
    }

    private void comprobarPareja(int posicion, ImageButton ib) {
        //Si no tenemos primera carta seleccionada
        if (primeraCarta == null) {
            primeraCarta = ib;
            primeraCarta.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Log.i("##FIREBASE : ", "listaImagenes-----" + listaImagenes.toString());
            Log.i("##FIREBASE : ", "listaImagenesDesordenada2-----" + listaImagenesDesordenada2.toString());
            primeraCarta.setImageResource(listaImagenes.get(listaImagenesDesordenada2.get(posicion)));
            primeraCarta.setEnabled(false);
            idPrimeraCarta = listaImagenesDesordenada2.get(posicion);

            listaCartasTemporalmenteGiradas.add(posicion);
            Log.i("@@@cards : ", "añado a listaCartasTemporalmenteGiradas la posición: " + posicion + " y la listatemporal es: " + listaCartasTemporalmenteGiradas);
            databaseReference.child("playedCards").child(connectionId).child(playerTurn).child("played_cards").setValue(listaCartasTemporalmenteGiradas);


            //Si tenemos primera carta seleccionada
        } else {
            parejaGirada = true;
            segundaCarta = ib;
            segundaCarta.setScaleType(ImageView.ScaleType.CENTER_CROP);
            segundaCarta.setImageResource(listaImagenes.get(listaImagenesDesordenada2.get(posicion)));
            segundaCarta.setEnabled(false);
            idSegundaCarta = listaImagenesDesordenada2.get(posicion);
            listaCartasTemporalmenteGiradas.add(posicion);
            Log.i("@@@cards : ", "añado a listaCartasTemporalmenteGiradas la posición: " + posicion + " y la listatemporal es: " + listaCartasTemporalmenteGiradas);
            databaseReference.child("playedCards").child(connectionId).child(playerTurn).child("played_cards").setValue(listaCartasTemporalmenteGiradas);


            //Si hemos conseguido pareja
            if (idPrimeraCarta == idSegundaCarta) {
                snd.play(acierto);
                primeraCarta = null;
                segundaCarta = null;
                idPrimeraCarta = -1;
                idSegundaCarta = -1;
                racha++;
                parejaGirada = false;

                Log.i("@@@cards : ", "antes de añadir a listafinal: " + listaCartasTemporalmenteGiradas);
                listaCartasGiradas.add(listaCartasTemporalmenteGiradas.get(0));
                listaCartasGiradas.add(listaCartasTemporalmenteGiradas.get(1));
                databaseReference.child("playedCards").child(connectionId).child(playerTurn).child("played_cards").removeValue();
                databaseReference.child("finalPlayedCards").child(connectionId).child(playerTurn).child("final_played_cards").setValue(listaCartasGiradas);
                databaseReference.child("cantidadParejasAcertadas").child(connectionId).child("cantidad_parejas_acertadas").setValue(cantidadParejasAcertadas + 1);
                Log.i("@@@cards : ", "añado a listafinal: " + listaCartasGiradas);
                listaCartasTemporalmenteGiradas.clear();
                Log.i("@@@cards : ", "limpio la listatemporal (hemos conseguido pareja): " + listaCartasTemporalmenteGiradas);

                if (playerTurn.equals(playerUniqueId)) { //Si es el turno del jugador 1
                    puntuacionPlayer1 += puntosParejaCorrecta + racha * 2;
                    txt_puntuacionPlayer1.setText(getString(R.string.txt_puntuacion) + " " + puntuacionPlayer1);
                    txt_racha.setText(getString(R.string.txt_racha) + " " + racha);
                    databaseReference.child("turns").child(connectionId).child("player_points").setValue(puntuacionPlayer1);

                } else { //Si es el turno del jugador 2
                    puntuacionPlayer2 += puntosParejaCorrecta + racha * 2;
                    txt_puntuacionPlayer2.setText(getString(R.string.txt_puntuacion) + " " + puntuacionPlayer2);
                    txt_racha.setText(getString(R.string.txt_racha) + " " + racha);
                    databaseReference.child("turns").child(connectionId).child("player_points").setValue(puntuacionPlayer2);
                }

                Log.i("@@@cards : ", "hemos conseguido pareja y envío a 'played_cards': " + listaCartasTemporalmenteGiradas);
                Log.i("@@@cards : ", "hemos conseguido pareja y envío a 'final_played_cards': " + listaCartasGiradas);

                //Si no hemos conseguido pareja
            } else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        primeraCarta.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        primeraCarta.setImageResource(backCard);
                        primeraCarta.setEnabled(true);
                        segundaCarta.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        segundaCarta.setImageResource(backCard);
                        segundaCarta.setEnabled(true);

                        //Inicializamos variables
                        snd.play(error);
                        primeraCarta = null;
                        segundaCarta = null;
                        idPrimeraCarta = -1;
                        idSegundaCarta = -1;
                        parejaGirada = false;
                        racha = 0;
                        txt_racha.setText(getString(R.string.txt_racha) + " " + racha);

                        databaseReference.child("playedCards").child(connectionId).child(playerTurn).child("played_cards").removeValue();
                        listaCartasTemporalmenteGiradas.clear();
                        Log.i("@@@cards : ", "limpio la listatemporal (no hemos conseguido pareja): " + listaCartasTemporalmenteGiradas);

                        //Cambiamos el turno
                        if (playerTurn.equals(playerUniqueId)) {
                            Log.i("##FIREBASE : ", "EL TURNO ES MIOOOOOOOOOOOOO (antes): " + playerTurn);
                            playerTurn = opponentUniqueId;
                            Log.i("##FIREBASE : ", "EL TURNO ES MIOOOOOOOOOOOOO (después): " + playerTurn);

                        } else {
                            Log.i("##FIREBASE : ", "EL TURNO ES DEL OTROOOO (antes): " + playerTurn);
                            playerTurn = playerUniqueId;
                            Log.i("##FIREBASE : ", "EL TURNO ES DEL OTROOOO (después): " + playerTurn);

                        }
                        databaseReference.child("turns").child(connectionId).child("player_id").setValue(playerTurn);
                    }
                }, 1000);
            }
        }
    }

    /*--------------------------[[ FUNCIONES ]]--------------------------*/

    private void comprobarFinPartida() {
        String ganador = "";
        Log.i("##FIN PARTIDA", "Cantidad parejas acertadas: " + cantidadParejasAcertadas + " y listaimagenes size: " + listaImagenes.size());
        Log.i("##FIN PARTIDA", "Puntuación player 1: " + puntuacionPlayer1);
        Log.i("##FIN PARTIDA", "Puntuación player 2: " + puntuacionPlayer2);
        if (cantidadParejasAcertadas == listaImagenes.size()) {
            if (puntuacionPlayer1 - puntuacionPlayer2 > 0) { //Gana el player 1
                Log.i("##FIN PARTIDA", "ENTRA EN EL PRIMER IF");
                Log.i("##FIN PARTIDA", "Puntuación player 1 (IF): " + puntuacionPlayer1);
                Log.i("##FIN PARTIDA", "Puntuación player 2 (IF): " + puntuacionPlayer2);
                ganador = txt_nicknameP1.getText().toString();
                databaseReference.child("won").child(connectionId).child("player_id").setValue(playerUniqueId);
            } else if (puntuacionPlayer1 - puntuacionPlayer2 < 0) { //Gana el player 2
                Log.i("##FIN PARTIDA", "ENTRA EN EL ELSE IF");
                Log.i("##FIN PARTIDA", "Puntuación player 1 (ELSE IF): " + puntuacionPlayer1);
                Log.i("##FIN PARTIDA", "Puntuación player 2 (ELSE IF): " + puntuacionPlayer2);
                ganador = txt_nicknameP2.getText().toString();
                databaseReference.child("won").child(connectionId).child("player_id").setValue(opponentUniqueId);
            } else { //Empate
                Log.i("##FIN PARTIDA", "ENTRA EN EL ELSE");
                Log.i("##FIN PARTIDA", "Puntuación player 1 (ELSE): " + puntuacionPlayer1);
                Log.i("##FIN PARTIDA", "Puntuación player 2 (ELSE): " + puntuacionPlayer2);
                ganador = getString(R.string.txt_empate);

                databaseReference.child("won").child(connectionId).child("player_id").setValue(ganador);
            }
            listaImagenes.clear();
            listaImagenesDesordenada.clear();
            insertPartidaBBDD(ganador);
            insertPuntosBBDD(txt_nicknameP1.getText().toString(), puntuacionPlayer1);
            insertPuntosBBDD(txt_nicknameP2.getText().toString(), puntuacionPlayer2);
            btn_salir.setVisibility(View.INVISIBLE);
        }
    }

    private void eliminarValuesFirebase() {
        databaseReference.child("cards").child(connectionId).removeValue();
        databaseReference.child("playedCards").child(connectionId).removeValue();
        databaseReference.child("finalPlayedCards").child(connectionId).removeValue();
        databaseReference.child("won").child(connectionId).removeValue();
        databaseReference.child("turns").child(connectionId).removeValue();
        databaseReference.child("cantidadParejasAcertadas").child(connectionId).removeValue();
        databaseReference.child("connections").child(connectionId).removeValue();
    }

    private void mostrarAlertDialog(String message, String title, int icon, Boolean cancelable, String positiveBtn, String doPositive, String negativeBtn, String doNegative) {
        AlertDialog.Builder alert = new AlertDialog.Builder(Juego.this);

        alert.setMessage(message)
                .setTitle(title)
                .setIcon(icon)
                .setCancelable(cancelable) //Ponemos false para que el usuario no pueda salir del dialogo pulsando fuera del area del dialogo ni con el boton retroceso
                .setPositiveButton(positiveBtn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (doPositive.equals(getString(R.string.btn_doPositive_finalizar))) { //El usuario quiere volver al menú principal
                            databaseReference.child("desconexion").child(connectionId).child(playerUniqueId).setValue("desconexion");
                            eliminarValuesFirebase();
                            finish();
                        } else if (doPositive.equals(getString(R.string.btn_doPositive_jugar))) { //El usuario quiere volver a jugar
                            eliminarValuesFirebase();
                            initFirebase();
                        }
                    }
                })
                .setNegativeButton(negativeBtn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (doNegative.equals(getString(R.string.btn_doNegative_cancelar))) // El usuario no quiere volver al menú principal
                            dialogInterface.cancel();
                        else if (doNegative.equals(getString(R.string.btn_doNegative_noJugar))) { //El usuario no quiere volver a jugar
                            eliminarValuesFirebase();
                            finish();
                        }
                    }
                })
                .create();
        alert.show();
    }

    /*--------------------------[[ BASE DE DATOS ]]--------------------------*/
    private void insertPartidaBBDD(String ganador) {
        if (ganador.equals(getString(R.string.txt_empate))) ganador = "empate";
        db.execSQL("INSERT INTO Partidas (NombreJugador1, PuntuaciónJugador1, NombreJugador2, PuntuaciónJugador2, Ganador, Fecha) VALUES ('" + txt_nicknameP1.getText().toString() + "', '" + puntuacionPlayer1 + "','" + txt_nicknameP2.getText().toString() + "', '" + puntuacionPlayer2 + "', '" + ganador + "', '" + obtenerFechaActual() + "')");
    }

    private void insertPuntosBBDD(String alias, int puntos) {
        Cursor c1 = db.rawQuery("SELECT Puntuación FROM Puntuaciones WHERE NombreJugador = '" + alias + "'", null);
        int puntuacion = 0;

        //Si no existe un jugador con el alias, se inserta nuevo
        if (c1 != null && !c1.moveToFirst()) {
            do {
                db.execSQL("INSERT INTO Puntuaciones (NombreJugador, Puntuación, Fecha) VALUES ('" + alias + "', '" + puntos + "', '" + obtenerFechaActual() + "')");
            } while (c1.moveToNext());

            //Si ya existe el jugador
        } else {
            puntuacion = c1.getInt(0) + puntos;
            db.execSQL("UPDATE Puntuaciones SET Puntuación = '" + puntuacion + "', Fecha = '" + obtenerFechaActual() + "' WHERE NombreJugador = '" + alias + "'");

        }

        //Añadimos el jugador a Firebase para el ranking global
        Map<String, Object> jugador = new HashMap<>();
        jugador.put("Alias", alias);
        jugador.put("Puntuación", puntos);
        jugador.put("Fecha", obtenerFechaActual());

        int puntuacionFinal = puntuacion;

        insertJugadorFirebase(jugador, alias, puntuacionFinal);

        //Como los dispositivos haran la insercion (unicamente la primera vez), eliminamos uno duplicado
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                eliminarDuplicadosFirebase();
            }
        }, 5000);

        c1.close();
    }

    private void insertJugadorFirebase(Map<String, Object> jugador, String alias, int puntuacionFinal) {
        dbFirebase.collection("jugadores")
                .whereEqualTo("Alias", alias)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) { // Se ha podido realizar la consulta correctamente
                            if (task.getResult().isEmpty()) { // Si no hay jugadores con el mismo alias, lo añadimos
                                dbFirebase.collection("jugadores")
                                        .add(jugador)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                Log.i("@@RANKING", "Jugador introducido correctamente con el id: " + documentReference.getId());
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i("@@RANKING", "Ha habido un error al introducir al jugador a la base de datos Firebase: ", e);
                                            }
                                        });
                            } else { //Si ya existe el jugador, actualizamos su puntuacion
                                Log.i("@@RANKING", "Ya existe un jugador con el mismo alias.");
                                DocumentSnapshot jugadorSnapshot = task.getResult().getDocuments().get(0);
                                String jugadorId = jugadorSnapshot.getId();
                                if (puntuacionFinal != 0) {
                                    dbFirebase.collection("jugadores")
                                            .document(jugadorId)
                                            .update("Puntuación", puntuacionFinal)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.i("@@RANKING", "Puntuación actualizada correctamente para el jugador con alias: " + alias);
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.i("@@RANKING", "Error al actualizar la puntuación: ", e);
                                                }
                                            });
                                }
                            }
                        } else { // La consulta no se ha podido realizar
                            Log.i("@@RANKING", "Error al realizar la consulta: ", task.getException());
                        }
                    }
                });
    }

    private void eliminarDuplicadosFirebase() {
        dbFirebase.collection("jugadores")
                .orderBy("Alias")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Set<String> uniqueAliases = new HashSet<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String alias = document.getString("Alias");

                                // Verifica si el alias ya existe
                                if (!uniqueAliases.add(alias)) {
                                    // Es un duplicado, elimina el documento
                                    String documentId = document.getId();
                                    dbFirebase.collection("jugadores").document(documentId).delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.i("@@RANKING", "Documento eliminado correctamente.");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.i("@@RANKING", "Error al eliminar el documento: ", e);
                                                }
                                            });
                                }
                            }
                        } else {
                            Log.i("@@RANKING", "Error al realizar la consulta: ", task.getException());
                        }
                    }
                });
    }

    private String obtenerFechaActual() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
        Date fecha = new Date();
        return dateFormat.format(fecha);
    }

    /*--------------------------[[ FUNCIONES ANDROID STUDIO ]]--------------------------*/

    //Cuando le damos a volver atras con la flecha del telefono
    @Override
    public void onBackPressed() {
        mostrarAlertDialog(getString(R.string.txt_salirMensaje), getString(R.string.txt_salirTitulo), R.drawable.warning, false, getString(R.string.txt_confirmar), "Finalizar", getString(R.string.txt_negar), "Cancelar");
    }
}
