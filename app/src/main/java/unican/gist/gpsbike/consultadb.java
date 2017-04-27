package unican.gist.gpsbike;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("InlinedApi")
/**
 * Created by andres on 21/4/16.
 */
public class consultadb {


    private ArrayList envio;
    private String txt = "nada";
    private Boolean continuar = false;
    boolean funcionando = true;
    int contador = 0;
    private int id = 0;
    private int idFin = 1;
    private String esTraza = "noestraza";//sirve para comprobar en el script php si tiene que hacer consulta de traza o de otra cosa
    private String array = "";//para la zona que mandamos
    private int idPunto = 0;
    Context context;

    consultadb(Context context) {
        this.context = context;
    }


    //nos creamos un boleano para comprobar desde otras pantallas que existe o no un usuario con su contraseña
    public void polygon(int zonaInicio, int zonaFin) {
        id = zonaInicio;
        idFin = zonaFin;
        funcionando = true;
        consultaBaseDeDatos("noestraza");
    }

    public void trazaCarretera() {
        funcionando = true;
        consultaBaseDeDatos("siestraza");
    }

    public void enviarIdPoligono(String arraymontado) {//solo se usa en el caso de descomponer los puntos para asignarles un poligono
        array = arraymontado;
        consultaBaseDeDatos("enviarZona");

    }

    public void enviarUTMPoligono(String arraymontado) {//solo se usa en el caso de descomponer los puntos para asignarles un poligono
        array = arraymontado;
        consultaBaseDeDatos("sendUtmCorregida");

    }

    public void enviarPunto(String arraymontado) {//solo se usa en el caso de descomponer los puntos para asignarles un poligono
        array = arraymontado;
        consultaBaseDeDatos("posicion");

    }

    private synchronized String consultaBaseDeDatos(final String esTraza) { //solo una instancia simultanea de la accion
        this.esTraza = esTraza;
        final String[] respuesta = {"nohayusuario"};
        //añadimos lo que queremos comprobar en este caso usuario y contraseña
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://193.144.208.142:443/android/polygon.php";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        txt = response;
                        Log.d("volley", "volley ok");
                        Log.d("volley", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("volley", "Error params");
                        txt = "nohayusuario";
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("zona1", String.valueOf(id));
                params.put("zona2", String.valueOf(idFin));
                params.put("traza", esTraza);
                params.put("array", String.valueOf(array));

                return params;
            }

            //se ejecuta si hay respuesta
            @Override
            protected void deliverResponse(String response) {
                if (esTraza.equalsIgnoreCase("siestraza")) {
                    guardarPuntos(response);
                } else {
                    if (esTraza.equalsIgnoreCase("noestraza")) {
                        guardarPoligonos(response);
                    }
                }
                Log.d("respuesta recibida", response);
            }
        };
        queue.add(postRequest);

        //recibimos un string de texto con el usuario o en caso contrario un string con la palabra nohayusuario
        Log.d("respuesta", respuesta[0]);
        return respuesta[0];


    }

    private void guardarPuntos(String response) {
        FileOutputStream outputStream;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            File file = new File(context.getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS), "gpsbike");
            if (!file.mkdirs()) {
                Log.e("CREATE DIR", "Directory not created");
            }
            try {
                outputStream = context.openFileOutput("puntos.txt", Context.MODE_PRIVATE);
                Log.e("CREATE DIR","/puntos.txt");
                outputStream.write(response.getBytes());
                outputStream.close();
                FileChannel inChannel = new FileInputStream(context.getFilesDir()+"/puntos.txt").getChannel();
                FileChannel outChannel = new FileOutputStream(file.getAbsolutePath()+"/puntos.txt").getChannel();
                try
                {
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                }
                finally
                {
                    if (inChannel != null)
                        inChannel.close();
                    if (outChannel != null)
                        outChannel.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    private void guardarPoligonos(String response) {
        FileOutputStream outputStream;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            File file = new File(context.getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS), "gpsbike");
            if (!file.mkdirs()) {
                Log.e("CREATE DIR", "Directory not created");
            }
            try {
                outputStream = context.openFileOutput("poligonos.txt", Context.MODE_PRIVATE);
                Log.e("CREATE DIR","/poligonos.txt");
                outputStream.write(response.getBytes());
                outputStream.close();
                FileChannel inChannel = new FileInputStream(context.getFilesDir()+"/poligonos.txt").getChannel();
                FileChannel outChannel = new FileOutputStream(file.getAbsolutePath()+"/poligonos.txt").getChannel();
                try
                {
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                }
                finally
                {
                    if (inChannel != null)
                        inChannel.close();
                    if (outChannel != null)
                        outChannel.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }
}


