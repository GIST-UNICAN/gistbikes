package unican.gist.gpsbike;

import android.Manifest;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;


/**
 * Created by andres on 14/4/16.
 */
public class serviceBoot extends Service implements LocationListener {
    //ES ALL DE MOMENTO COPIA DEL MAIN ACTIVITY QUE HE IDO PROBANDO, seria interesante crear aparte la clase de localización, es una buena tarea
    //cuando quiera optimizar el código, lo cual simplificaría mucho las cosas


    LocationManager managerLocation; //Gestor del servicio de localización
    public boolean servicioActivo;
    private LocationListener locListener; //parta actualizar constantemente la posición en este caso esta implementado
    private String provider;

    private double[] t1 = {0, 0};
    public Double lat;
    public Double lng;
    double velo1 = 0;

    //vamos a contar también el número de veces que se ha registrado la posición desde que se activa el botón
    private int numeroDeRegistro = 0;
    private int tiempo;

    //definimos poligonos, vertices las zonas de control
    private ArrayList poligonos;
    ;
    private double[] vertices;
    Polygon poly1;
    private ArrayList poligonoPunto;//lista de poligonos que contienen el punto

    //Definimos la traza de la carretera
    private double[] polilineaTraza;
    private ArrayList polilineaTrazaPuntoActual;

    //variable para zaber en que zona estamos
    private int zona = 10000;

    //guardamos las 5 últimas posiciones para poder después dibujar la traza y hacer las predicciones pertinenetes
    private double[] almacenPosicion = new double[10];
    private int contadorAlmacen = 0;

    //Cojemos el tiempo para actualizar la notificacion cada ciertos segundos
    long time;

    //para la intersección de las 2 rectas una recta que ajuste por minimos cuadrados la posicion
    private double[] rectaPosicion;
    private double minimaDistancia = 1000000000;//para la distancia al punto de corte

    //Para la asignacion de puntos de la traza a poligonos
    int zonaTraza = 0;
    int idPunto = 0;
    ArrayList puntosTraza;
    ArrayList puntosTrazaUTM;

    //valores para enviar la posicion a la db
    private double[] enviarPosicion;
    double puntoEnviar;
    String android_id = "";


    // constructor
    public serviceBoot() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "servicio creado", Toast.LENGTH_LONG).show();
        android_id = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
        //new Apoligono().execute(); //para asignar puntos a poligonos
        //new constructorPoligonos().execute();
        // new Apoligono3().execute(); //para asignar puntos a poligonos
        //RECUPERAR POLIGONOS;
        //RECUPERAR TRAZA;

        Toast.makeText(this, "servicio creado 2", Toast.LENGTH_LONG).show();
        poligonos = recuperarPoligonos();
        Log.d("POLIGONOS", String.valueOf(poligonos));

        polilineaTraza = recuperarPolilineaTraza();
        //iniciarServicio();
    }

    public Boolean checkMobileData() {
        ConnectivityManager cm = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            //si data
            return false;
        }
        return true;
    }


    public void iniciarServicio() {
        Toast.makeText(this, "servicio iniciado", Toast.LENGTH_LONG).show();
        //Se activa el servicio de localización
        servicioActivo = true;
        //fijamos el tiempo en milisecs
        tiempo = 1000;
        //reseteamos el contador
        numeroDeRegistro = 0;
        //Crea el objeto que gestiona las localizaciones
        managerLocation = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //fijamos el criterio de posicion
        final Criteria precision = new Criteria();
        precision.setAccuracy(Criteria.ACCURACY_FINE);
        //(la mejor precisión posible)
        provider = managerLocation.getBestProvider(precision, true);
        provider = managerLocation.GPS_PROVIDER;
        //quitamos la posiciona almacenada pues da error
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        managerLocation.requestLocationUpdates(provider, tiempo, 0, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(serviceBoot.this, "Servicio destruido", Toast.LENGTH_LONG).show();
        //para que nunca se cierre vamos a hacer que el servicio se reabra al ser cerrado
        Intent i = new Intent(this, serviceBoot.class);
        this.startService(i);
    }


    //metodos del location listener
    public void onLocationChanged(Location location) {
        // Se ha encontrado una nueva localización
        //new Apoligono6().execute(); //para asignar puntos a poligonos

        if (location != null) {
            lat = Double.valueOf(location.getLatitude());//obtenemos latutud y longitud
            lng = Double.valueOf(location.getLongitude());
            //tratamiento de coordenadas
            double[] t0 = {lat, lng};
            cambiarCoordenadas cc = new cambiarCoordenadas();//método para pasar a latitud longitud
            double[] xy = cc.latilongi(t0);
            //almacenamos las 5 últimas posiciones para las predicciones
            if (contadorAlmacen >= almacenPosicion.length) {
                contadorAlmacen = 0;
            }
            almacenPosicion[contadorAlmacen] = xy[0];
            contadorAlmacen++;
            almacenPosicion[contadorAlmacen] = xy[1];
            contadorAlmacen++;
            //si ya esta lleno el almacen de posición ajusta la recta de las posiciones y
            //se guarda en una variable para luego hacer la intersección
            if (almacenPosicion[9] != 0) {
                rectaPosicion = (double[]) minimosCuadrados(almacenPosicion);
                Log.d("recta m ", String.valueOf(rectaPosicion[0]));
                Log.d("recta n ", String.valueOf(rectaPosicion[1]));
            }
            if (t1[0] != 0) {
                //para el calculo de distancias usamos otro método de la librería que nos calcula la distancia, convertimos a metros y listo
                double dist = cc.distancia(t0, t1);
                // ponemos los decimales como nos gusta
                DecimalFormat decimales = new DecimalFormat("0.00");
                Double velo = Double.valueOf(location.getSpeed());
                velo1 = 3.6 * velo;
                //ponemos la condicion de que no nos muestre el primer registro para no tener datos distorsionados
                time = System.currentTimeMillis();
                if (numeroDeRegistro > 1) {
                    int notifyID = 1;
                    //notificamos la velocidad para decir que estamos funcionando de fondo y que se vea que la app esta funcionando
                    NotificationCompat.Builder mBuilder;
                    mBuilder = new NotificationCompat.Builder(serviceBoot.this)
                            .setContentTitle("GpsBike esta funcioando!!!")
                            .setContentText("velocidad: " + decimales.format(Double.valueOf(velo1)) + " km/h")
                            .setSmallIcon(R.mipmap.ic_launcher);

                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                    notificationManager.notify(notifyID, mBuilder.build());
                }

            }
            //se guarda el registro para calcular luego la distancia -> velocidad
            t1[0] = t0[0];
            t1[1] = t0[1];
            //contamos que se ha registrado la posición
            numeroDeRegistro++;
            //iniciamos la alerta de proximidad
            int alerta = compruebaPosición(Double.valueOf(location.getLatitude()), Double.valueOf(location.getLongitude()), 6000);
            switch (alerta) {
                case 0:
                    Log.d("posicion", "fuera de zona de control grande");
                    break;
                case 1:
                    Log.d("posición", "dentro de zona de control primaria");
                    //Entramos a un asynctask para obtener los poligonos, para ello vamos a comprobar que este conectado
                    //hay que comprobar en que poligono estamos, lo hacemos de una forma muy bruta
                    float plat = (float) xy[0];
                    float plng = (float) xy[1];
                    Point puntoActual = new Point(plat, plng);

                    //vamos ahora a identificar en que poligono estamos, identificamos que no sea un objeto falso y procedemos
                    poligonoPunto = new ArrayList();
                    if (poligonos != null) {
                        poligonoPunto.clear();
                        for (int i = 0; i < poligonos.size(); i++) {
                            //añadimos un poligono
                            vertices = (double[]) poligonos.get(i);
                            Double[] v2 = new Double[vertices.length];
                            for (int j = 0; j < vertices.length; j++) {
                                v2[j] = Double.valueOf(vertices[j]);
                            }

                            poly1 = new Polygon.Builder()
                                    .addVertex(new Point(v2[0].floatValue(), v2[1].floatValue()))
                                    .addVertex(new Point(v2[2].floatValue(), v2[3].floatValue()))
                                    .addVertex(new Point(v2[4].floatValue(), v2[5].floatValue()))
                                    .addVertex(new Point(v2[6].floatValue(), v2[7].floatValue()))
                                    .build();

                            if (poly1 != null) { //si existe el poligono comprueba el punto
                                if (poly1.contains(puntoActual)) {
                                    poligonoPunto.add(i + 1);
                                    //enviamos el punto a la db primero buscamos el punto que más se acerque al nuestro en la db de puntos que tenemos
                                    cambiarCoordenadas distancia = new cambiarCoordenadas();
                                    //nos quedamos con los puntos que corresponden a nuestro poligono y los dos anteriores
                                    int poligonoActual = i + 1;
                                    int contador = 0;
                                    double idPuntoEnviar = 1;
                                    polilineaTrazaPuntoActual = new ArrayList();
                                    for (int p = 0; p < polilineaTraza.length; p = p + 3) {
                                        //vamos cogiendo los puntos de nuestro poligogono , el siguiente y el anterior
                                        if (polilineaTraza[p] == poligonoActual || polilineaTraza[p] == poligonoActual + 1 || polilineaTraza[p] == poligonoActual - 1) {
                                            polilineaTrazaPuntoActual.add(polilineaTraza[p + 1]);//x punto
                                            polilineaTrazaPuntoActual.add(polilineaTraza[p + 2]);//y punto
                                            polilineaTrazaPuntoActual.add(idPuntoEnviar);//identificador punto
                                            //Log.d("punto", String.valueOf(idPuntoEnviar));

                                            contador = contador + 2;
                                        }
                                        idPuntoEnviar++;
                                    }
                                    double minimaDistancia = 1000000;

                                    for (int l = 0; l < polilineaTrazaPuntoActual.size(); l = l + 3) {
                                        //vamos a buscar el punto que pilla mas cercanoa a nuestra posicion
                                        double[] puntoComprobar = {(double) polilineaTrazaPuntoActual.get(l), (double) polilineaTrazaPuntoActual.get(l + 1)};
                                        double distanciaEnviar = Math.sqrt(Math.pow((puntoComprobar[0] - xy[0]), 2) + Math.pow(puntoComprobar[1] - xy[1], 2));//hemos cambiado las coordenadas porque el método requiere lat lng
                                        //si la distancia a enviar es menor que la guardad envia la distancia nueva
                                        if (distanciaEnviar < minimaDistancia) {
                                            minimaDistancia = distanciaEnviar;
                                            Log.d("distancia", String.valueOf(distanciaEnviar) + " punto " + polilineaTrazaPuntoActual.get(l + 2));

                                            puntoEnviar = (double) polilineaTrazaPuntoActual.get(l + 2);
                                        }
                                    }


                                }
                            }

                        }
                    }
                    Log.d("puntoEnv", String.valueOf(puntoEnviar));
                    new enviarPunto().execute();//para enviar el punto
                    if (poligonoPunto.isEmpty()) {
                        zona = 0;
                    } else {
                        zona = 1000;
                        Log.d("posición", "dentro de zona de control: " + poligonoPunto.get(0));
                    }

                    // Si estoy dentro de los poligonos zona toma un valor diferente de 0 ,
                    // Si estamos fuera, zona es  0  y vamos a pasar a comprobar la distancia a los poligonos
                    // Así calculamos lo que queda para entrar
                    int zona2 = 0;
                    if (zona == 0 & poligonos != null) {//llamamos al método de intersección y le pasamos la m y la n de las 2 rectas
                        // la que va con las 5 últimas posiciones y la recta que va con los 2 lados de un poligono
                        // empezamos con la recta que va con los 2 puntos de el poligono, es importante coger siempre los vertices 1 y 4 y 2-3 para tirar las 2 rectas
                        minimaDistancia = 100000;//ponemos un valor muy alto a la minima distancia para poder enviar valores
                        for (int i = 0; i < poligonos.size(); i++) {
                            double[] poligon = (double[]) poligonos.get(i); //guardamos en el double poligono los 4 vertices del actual
                            double[] recta1 = (double[]) recta(poligon[0], poligon[6], poligon[1], poligon[7]);//recta de los poligonos
                            double[] recta3 = (double[]) recta(poligon[2], poligon[4], poligon[3], poligon[5]);//recta de los poligonos
                            double[] recta2 = (double[]) minimosCuadrados(almacenPosicion);//recta por minimos cuadrados de la dirección actual
                            double[] interseccion = (double[]) interseccion(recta1[0], recta1[1], recta2[0], recta2[1]);
                            double[] interseccion2 = (double[]) interseccion(recta3[0], recta3[1], recta2[0], recta2[1]);
                            //comprobamos si el punto de intersección esta dentro de los limites del polígono
                            if (estaDentro(poligon[0], poligon[1], poligon[6], poligon[7], interseccion[0], interseccion[1]) && mismoSentido(recta2[0], xy[0], interseccion[0], xy[1], interseccion[1])) {
                                if (minimaDistancia > distanciaPtoInterseccion(xy, interseccion)) {
                                    minimaDistancia = distanciaPtoInterseccion(xy, interseccion);//xy es el punto actual, intersección representa el punmto de corte
                                    double tiempoEstimado = (minimaDistancia / Double.valueOf(location.getSpeed()));
                                    zona2 = i + 1;
                                }
                            }
                            if (estaDentro(poligon[2], poligon[3], poligon[4], poligon[5], interseccion2[0], interseccion2[1]) && mismoSentido(recta2[0], xy[0], interseccion2[0], xy[1], interseccion2[1])) {
                                if (minimaDistancia > distanciaPtoInterseccion(xy, interseccion2)) {
                                    minimaDistancia = distanciaPtoInterseccion(xy, interseccion2);//xy es el punto actual, intersección representa el punmto de corte
                                    double tiempoEstimado = (minimaDistancia / Double.valueOf(location.getSpeed()));
                                    zona2 = i + 1;
                                }
                            }


                        }
                        Log.d("intersección", String.valueOf(minimaDistancia) + "m al poligono: " + zona2 + " en un tiempo de : ");

                        break;
                    }
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider ) {

    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public int compruebaPosición(double x, double y, double radio) {
        //Primero recuperamos los datos de zona-> método a implementar de momento está puesta la universidad
        //mientras haya un solo tramo no hará falta poner nada a implementar, después ya si
        // UNICAN double xCentro = 43.47182;
        // UNICAN double yCentro = -3.798381;
        double xCentro = 43.273123;
        double yCentro = -3.833620;
        double[] centro = {xCentro, yCentro};
        double[] punto = {x, y};
        cambiarCoordenadas cc = new cambiarCoordenadas();
        //comprobamos la distancia
        if (cc.distancia(centro, punto) <= radio) {
            return 1;
        } else {
            return 0;
        }

    }


    class enviarPunto extends AsyncTask<String, String, String> //envia los puntos con el tiempo estimado de salida a la db

    {
        @Override
        protected String doInBackground(String... params) {
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //conectamos a la db y guardamos la respuesta en un string
            consultadb db = new consultadb(getApplicationContext());
            DecimalFormat decimales = new DecimalFormat("0.00");
            db.enviarPunto(android_id + ":" + puntoEnviar + ":" + decimales.format(velo1));
            Log.d("Punto Enviado", android_id + ":" + puntoEnviar);
            // db.enviarIdPoligono(desmontarArray);
        }
    }


    //métodos para definir las rectas
    Object minimosCuadrados(double[] almacenPosi) {//ajustamos una recta a las 5 últimas posiciones
        double x = (almacenPosi[0] + almacenPosi[2] + almacenPosi[4] + almacenPosi[6] + almacenPosi[8]);
        double y = (almacenPosi[1] + almacenPosi[3] + almacenPosi[5] + almacenPosi[7] + almacenPosi[9]);
        double x2 = (almacenPosi[0] * almacenPosi[0] + almacenPosi[2] * almacenPosi[2] + almacenPosi[4] * almacenPosi[4] + almacenPosi[6] * almacenPosi[6] + almacenPosi[8] * almacenPosi[8]);
        double xy = (almacenPosi[0] * almacenPosi[1] + almacenPosi[2] * almacenPosi[3] + almacenPosi[4] * almacenPosi[5] + almacenPosi[6] * almacenPosi[7] + almacenPosi[8] * almacenPosi[9]);
        double mediax = (almacenPosi[0] + almacenPosi[2] + almacenPosi[4] + almacenPosi[6] + almacenPosi[8]) / 5;
        double mediay = (almacenPosi[1] + almacenPosi[3] + almacenPosi[5] + almacenPosi[7] + almacenPosi[9]) / 5;
        double pendiente = ((xy - (x * y) * 0.2) / (x2 - (x * x * 0.2)));
        double n = mediay - (pendiente * mediax);
        double[] mn = {pendiente, n};
        return mn;
    }

    //objeto poligono, queda definida por sus cuatro vértives
    Object poligono(double v1, double v2, double v3, double v4) {
        double[] poligono = {v1, v2, v3, v4};
        return poligono;
    }

    //objeto recta a partir de dos puntos para la forma y = m·x + n
    Object recta(double x1, double x2, double y1, double y2) {
        double m = (y2 - y1) / (x2 - x1);
        double n = (((-x1 * (y2 - y1)) + (y1 * (x2 - x1))) / (x2 - x1));
        double[] mn = {m, n};
        return mn;
    }

    //intersección rectas
    Object interseccion(double m1, double n1, double m2, double n2) {
        double x = ((n2 - n1) / (m1 - m2));
        double y = m1 * x + n1;
        double[] ptoIntersecion = {x, y};
        return ptoIntersecion;
    }

    //Diatancia posición intersección, y tiempo
    protected double distanciaPtoInterseccion(double[] punto, double[] interseccion) {
        double pos = Math.sqrt(Math.pow((punto[0] - interseccion[0]), 2) + Math.pow(punto[1] - interseccion[1], 2));
        return pos;
    }

    //para la distanci a poligonos nos hace falta saber si el punto de intersección está en los límites del segmento que definen, los lados del polígono
    protected boolean estaDentro(double start_x, double start_y, double end_x, double end_y, double point_x, double point_y) {
        double dx = end_x - start_x;
        double dy = end_y - start_y;
        double innerProduct = (point_x - start_x) * dx + (point_y - start_y) * dy;
        return 0 <= innerProduct && innerProduct <= dx * dx + dy * dy;
    }

    //comprobar sentido de trayectoria y de la intersección, mediante vector directores: https://es.wikipedia.org/wiki/Vector_director
    protected boolean mismoSentido(double m, double x1, double x2, double y1, double y2) {
        double pEscalar = (x2 - x1) + (m * (y2 - y1));
        Log.d("prueba", String.valueOf(pEscalar));
        return 0 >= pEscalar;
    }

    private ArrayList<double[]> recuperarPoligonos() {
        Log.d("1", "1");

        ArrayList<double[]> poligonosRecuperados = new ArrayList<>();
        String line = "";
        String line1 = "";
        try {
            File file = new File(getApplicationContext().getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS), "gpsbike");
            FileChannel inChannel = new FileInputStream(file.getAbsolutePath() + "/poligonos.txt").getChannel();
            FileChannel outChannel = new FileOutputStream(getApplicationContext().getFilesDir() + "/poligonos.txt").getChannel();
            Log.d("2", "1");

            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
                Log.d("3", "1");

            } finally {
                if (inChannel != null)
                    inChannel.close();
                if (outChannel != null)
                    outChannel.close();
            }
            InputStream instream = openFileInput("poligonos.txt");
            Log.d("4", "1");

            if (instream != null) {
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);

                try {
                    while ((line = buffreader.readLine()) != null)
                        line1 = line1 + line;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            String error = "";
            error = e.getMessage();
            e.printStackTrace();
        }
        String[] verticesPoligono = line1.split(":");
        for (int i = 0; i < verticesPoligono.length - 8; i = i + 8) {
            Log.d("NUMERO POLIGONO ", String.valueOf(i));
            double[] polig = {Double.valueOf(verticesPoligono[i]), Double.valueOf(verticesPoligono[i + 1]), Double.valueOf(verticesPoligono[i + 2]), Double.valueOf(verticesPoligono[i + 3]), Double.valueOf(verticesPoligono[i + 4]), Double.valueOf(verticesPoligono[i + 5]), Double.valueOf(verticesPoligono[i + 6]), Double.valueOf(verticesPoligono[i + 7])};
            poligonosRecuperados.add(polig);
            //Log.d("poligono", String.valueOf(polig[6]));
            //Log.d("poligono", String.valueOf(polig[7]));
        }
        return poligonosRecuperados;
    }

    private double[] recuperarPolilineaTraza() {
        double[] polilineaTrazaRecuperada = null;
        String line = "";
        String line1 = "";
        try {
            File file = new File(getApplicationContext().getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS), "gpsbike");
            FileChannel inChannel = new FileInputStream(file.getAbsolutePath() + "/puntos.txt").getChannel();
            FileChannel outChannel = new FileOutputStream(getApplicationContext().getFilesDir() + "/puntos.txt").getChannel();
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } finally {
                if (inChannel != null)
                    inChannel.close();
                if (outChannel != null)
                    outChannel.close();
            }
            InputStream instream = openFileInput("puntos.txt");
            if (instream != null) {
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);

                try {
                    while ((line = buffreader.readLine()) != null)
                        line1 = line1 + line;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            String error = "";
            error = e.getMessage();
        }
        String[] verticesLinea = line1.split(":");
        polilineaTrazaRecuperada = new double[verticesLinea.length];
        for (int i = 0; i < verticesLinea.length; i++) {
            polilineaTrazaRecuperada[i] = Double.valueOf(verticesLinea[i]);
        }
        return polilineaTrazaRecuperada;
    }


    //clase para asignar poligono a los puntos en la db solo se usa en la primera ejecucion interna del programa, por ello esta comentada
   /* class Apoligono extends AsyncTask<String, String, String>

    {
        @Override
        protected String doInBackground(String... params) {
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
                //conectamos a la db y guardamos la respuesta en un string, sacamo todos los puntos que hay
                consultadb db = new consultadb();
                String[] trazado;
                //Vamos separando los valores que vienen unidos por comas y los almacenamos
                trazado = db.trazaCarretera().split(":");
                //guardamos la traza en una cadena de doubles
                polilineaTraza = new double[trazado.length];
                for (int i = 0; i < trazado.length; i++) {
                    polilineaTraza[i] = Double.parseDouble(trazado[i]);
                    Log.d("politraza", String.valueOf(polilineaTraza[i]));


                puntosTraza=new ArrayList();
            }
        }
    }



class Apoligono3 extends AsyncTask<String, String, String> //comprueba punto a punto y asigna poligonos a los puntos, es asincrona pero no haría falta

{
    @Override
    protected String doInBackground(String... params) {
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        puntosTrazaUTM=new ArrayList();
        for (int i=0;i<polilineaTraza.length;i=i+2){ //para cambiar de coordenadas todos los puntos de la traza
            cambiarCoordenadas cc=new cambiarCoordenadas();
           double[] utm= cc.utm(new double[]{polilineaTraza[i], polilineaTraza[i + 1]});
            Log.d("coor",utm[0]+" "+utm[1]);
            puntosTrazaUTM.add(utm[0]);
            puntosTrazaUTM.add(utm[1]);
        }
        /*for (int i = 0; i < poligonos.size(); i++) { //para poder asignar a cada punto su poligono
            //añadimos un poligono
            vertices = (double[]) poligonos.get(i);
            Double[] v2 = new Double[vertices.length];
            for (int j = 0; j < vertices.length; j++) {
                v2[j] = Double.valueOf(vertices[j]);
            }
            poly1 = new Polygon.Builder()
                    .addVertex(new Point(v2[0].floatValue(), v2[1].floatValue()))
                    .addVertex(new Point(v2[2].floatValue(), v2[3].floatValue()))
                    .addVertex(new Point(v2[4].floatValue(), v2[5].floatValue()))
                    .addVertex(new Point(v2[6].floatValue(), v2[7].floatValue()))
                    .build();
            int contadorPuntos=1;
            for(int m=0;m<polilineaTraza.length;m=m+2) {
                float plat2 = (float) polilineaTraza[m];
                float plng2 = (float) polilineaTraza[m+1];
                Point puntoActual2 = new Point(plat2, plng2);
                if (poly1 != null) { //si existe el poligono comprueba el punto
                    if (poly1.contains(puntoActual2)) {
                        zonaTraza=i+1;
                        idPunto = contadorPuntos;
                        puntosTraza.add(idPunto);
                        puntosTraza.add(zonaTraza);
                        //hemos añadido al array list cada punto con el poligono al cual pertenece

                    }
                }
                contadorPuntos++;
            }


        }



    }
}

    class Apoligono6 extends AsyncTask<String, String, String> //sirve para reenviar a la db los puntos con su poligono, para no hacerse un
                                                               //lío

    {
        @Override
        protected String doInBackground(String... params) {
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //conectamos a la db y guardamos la respuesta en un string
            consultadb db = new consultadb();
            String desmontarArray="";
            /*for (int i=0;i<puntosTraza.size();i=i+2){
                desmontarArray=desmontarArray+String.valueOf(puntosTraza.get(i))+":"+String.valueOf(puntosTraza.get(i+1))+":";
            }
            db.enviarIdPoligono(desmontarArray);
            for (int i=0;i<puntosTrazaUTM.size();i=i+2){
                desmontarArray=desmontarArray+String.valueOf(puntosTrazaUTM.get(i))+":"+String.valueOf(puntosTrazaUTM.get(i+1))+":";
            }
            db.enviarUTMPoligono(desmontarArray);
           // db.enviarIdPoligono(desmontarArray);



        }
    }*/
}









