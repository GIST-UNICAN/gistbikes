package unican.gist.gpsbike;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;
import rx.Subscriber;
import unican.gist.gpsbike.model.GetVersionUseCase;
import unican.gist.gpsbike.model.Utils;
import unican.gist.gpsbike.model.VersionData;


public class MainActivity extends Activity implements LocationListener, OnMapReadyCallback {
    LocationManager managerLocation; //Gestor del servicio de localización
    private boolean servicioActivo;
    private LocationListener locListener; //parta actualizar constantemente la posición en este caso esta implementado
    private Button botonActivar;
    private String provider;
    private double[] t1 = {0, 0};
    private int tiempo;
    //vamos a contar también el número de veces que se ha registrado la posición desde que se activa el botón
    private int numeroDeRegistro = 0;

    //gps
    private AlertDialog alert = null;
    private String gpsActivo = "no";

    //google maps
    GoogleMap googleMap;
    private GoogleApiClient client;

    //permisos localizacion
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 2;
    private boolean localizacion = false;


    //Definimos la traza y los poligonos de la carretera
    private double[] polilineaTraza;
    private ArrayList poligonos;


    //boleanos de las Asynctask
    private boolean traza = true;
    private boolean poligononoDefinido = true;

    //Utilidades
    Utils utils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        utils = new Utils(getApplicationContext());
        botonActivar = (Button) findViewById(R.id.BotonActivar);

        //inicializador del fragment
        final AlertDialog alert = null;
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);///crear las clases que faltan https://developers.google.com/maps/documentation/android-api/?hl=es*/

        //El botón activar permitirá activar y desactivar el servicio.

        servicioActivo = false;

        //obtenemos la traza y los poligonos
        if (conectadoRedMovil()) {
            new GetVersionUseCase().execute(new MainActivity.GetVersionSubscriber());

        } else {
            Toast.makeText(getApplicationContext(), "esta aplicación requiere de una conexion a internet", Toast.LENGTH_LONG).show();
        }

        botonActivar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                accesoLocalizacion();
                accesoInternet();
                if (localizacion) {//si tiene permiso de acceso a localización
                    if (servicioActivo) {
                        pararServicio();
                    } else {
                        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            new AlertNoGps().execute();
                        }
                        while (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            Intent i = new Intent(MainActivity.this, serviceBoot.class);
                            MainActivity.this.startService(i);
                            iniciarServicio();
                            break;
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Debe permitir el acceso a la localización", Toast.LENGTH_LONG).show();
                }
            }
        });


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build( );


    }

    private void getData(int dataVersion) {
        int preferencesVersion = utils.getVersionFromPreferences();
        int dataBaseVersion = dataVersion;
        if (preferencesVersion != 0 && preferencesVersion == dataBaseVersion) {
            Log.d("VERSION", String.valueOf(dataVersion));

            //obtenemos los poligonos y la traza de preferencias
            iniciarServicio();
        } else {
            Log.d("VERSION descargando poligonos", String.valueOf(dataVersion));
            //descargamos los poligonos y la traza, y los guardamos en preferrencias
            new constructorPoligonos().execute();
            new traza().execute();
            utils.setVersionToPreferences(dataVersion);
            iniciarServicio();
        }
    }

    private final class GetVersionSubscriber extends Subscriber<VersionData> {
        //3 callbacks
        //Show the listView
        @Override
        public void onCompleted() {

        }

        //Show the error
        @Override
        public void onError(Throwable e) {
            Log.e("ERROR REMINDERS ", e.toString());
            e.printStackTrace();
        }

        //Update listview datas
        @Override
        public void onNext(VersionData versionData) {
            getData(versionData.getVersion());
        }
    }

    //NUEVO METODO GPS crea una asynctask para iniciar un intent a ajustes
    class AlertNoGps extends AsyncTask<String, String, String> {


        @Override
        protected String doInBackground(String... params) {
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("El sistema GPS esta desactivado, ¿Desea activarlo ahora?")
                    .setCancelable(false)
                    .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            gpsActivo = "ok";
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            gpsActivo = "no";
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

    }

    public void pararServicio() {
        //Se para el servicio de localización
        servicioActivo = false;
        //Se desactivan las notificaciones de posición
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
        managerLocation.removeUpdates(this);
        //ponemos a 0 el contador del registro de datos
        numeroDeRegistro = 0;
    }

    public void iniciarServicio() {
        //Se activa el servicio de localización
        servicioActivo = true;
        tiempo = 1000;
        botonActivar.setText("localizacion activa");
        //Crea el objeto que gestiona las localizaciones
        managerLocation = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final Criteria precision = new Criteria();
        precision.setAccuracy(Criteria.ACCURACY_FINE);
        //obtiene el mejor proveedor en función del criterio asignado
        //(la mejor precisión posible)
        //provider = managerLocation.getBestProvider(precision, true);
        provider = managerLocation.GPS_PROVIDER;

        //Se activan las notificaciones de localización con los parámetros: proveedor, tiempo mínimo de actualización 1000=1segundo, distancia mínima, Locationlistener

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        managerLocation.requestLocationUpdates(managerLocation.GPS_PROVIDER, 1000, 0, this);


    }

    public void muestraPosicionActual(Location loc) throws IOException {
        if (loc == null) {//Si no se encuentra localización, se mostrará "Desconocida"
            Toast.makeText(this, "Error al obtener la localización", Toast.LENGTH_LONG).show();
        } else {//Si se encuentra, se mostrará la latitud y longitud
            //Ponemos en marcha el conversor
            Double lat = Double.valueOf(loc.getLatitude());
            Double lng = Double.valueOf(loc.getLongitude());
            //GOOGLE MAPS
            LatLng posicion = new LatLng(lat, lng);
            CameraPosition cameraPosition = new CameraPosition(posicion, 17, 0, 0);
            googleMap.setTrafficEnabled(true);
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
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
            googleMap.setMyLocationEnabled(true);
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            //tratamiento de coordenadas
            double[] t0 = {lat, lng};
            cambiarCoordenadas cc = new cambiarCoordenadas();//método para pasar a latitud longitud
            if (t1[0] != 0) {
                //para el calculo de distancias usamos otro método de la librería que nos calcula la distancia, convertimos a metros y listo
                double dist = cc.distancia(t0, t1);
                // ponemos los decimales como nos gusta
                DecimalFormat decimales = new DecimalFormat("0.00");

            }
            //se guarda el registro para calcular luego la distancia -> velocidad
            t1[0] = t0[0];
            t1[1] = t0[1];
            //contamos que se ha registrado la posición
            numeroDeRegistro++;
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        LatLng mapCenter = new LatLng(43.4711134, -3.7988131);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(mapCenter, 7));
        // and change perspective when the map is tilted.
        map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_media_play))
                .position(mapCenter)
                .flat(true)
                .rotation(245));

        CameraPosition cameraPosition = CameraPosition.builder()
                .target(mapCenter)
                .zoom(10)
                .bearing(0)
                .build();

        // Animate the change in camera view over 1 seconds
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),
                1000, null);
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
        map.setMyLocationEnabled(true);
    }


    @Override
    public void onLocationChanged(Location location) {
        // Se ha encontrado una nueva localización
        try {
            muestraPosicionActual(location);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        //probamos con otro provider para poder tener datos
        new AlertNoGps().execute();


    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }


    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://unican.gist.gpsbike/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://unican.gist.gpsbike/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                //Si la petición es cancelada, el resultado estará vacío.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    localizacion = true;

                } else {
                    localizacion = false;
                    //Permiso denegado. Desactivar la funcionalidad que dependía de dicho permiso.
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_INTERNET: {
                //Si la petición es cancelada, el resultado estará vacío.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    localizacion = true;

                } else {
                    localizacion = false;
                    //Permiso denegado. Desactivar la funcionalidad que dependía de dicho permiso.
                }
                return;
            }

        }
    }

    public boolean accesoLocalizacion() {
        boolean permisoConcedido = false;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                localizacion = true;
        /* Aquí se mostrará la explicación al usuario de porqué es
        necesario el uso de un determinado permiso, pudiéndose mostrar de manera asíncrona, o lo que es lo mismo, desde un
        hilo secundario, sin bloquear el hilo principal, y a la espera de
        que el usuario concede el permiso necesario tras visualizar la explicación.*/
            } else {

        /* Se realiza la petición del permiso. En este caso permisos
        para leer los contactos.*/
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
        } else {
            localizacion = true;
        }

        return permisoConcedido;
    }

    public boolean accesoInternet() {
        boolean permisoConcedido = false;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            Log.d("entra", "0");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.INTERNET)) {
                Log.d("entra", "1");
                localizacion = true;
        /* Aquí se mostrará la explicación al usuario de porqué es
        necesario el uso de un determinado permiso, pudiéndose mostrar de manera asíncrona, o lo que es lo mismo, desde un
        hilo secundario, sin bloquear el hilo principal, y a la espera de
        que el usuario concede el permiso necesario tras visualizar la explicación.*/
            } else {
                Log.d("entra", "2");

        /* Se realiza la petición del permiso. En este caso permisos
        para leer los contactos.*/
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
            }
        }

        return permisoConcedido;
    }


    class constructorPoligonos extends AsyncTask<String, String, String>

    {
        //Comprobamos red móvil para saber si hay cobertura
        //consultadb dab=new consultadb();
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected String doInBackground(String... params) {
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (poligononoDefinido) {
                //conectamos a la db y guardamos la respuesta en un string
                consultadb db = new consultadb(getApplicationContext());
                String[] poli;
                //Vamos separando los valores que vienen unidos por comas y los almacenamos
                int zonaInicio = 1;
                int zonaFin = 628;
                db.polygon(zonaInicio, zonaFin);
            }
        }
    }


    //clase para obtener la traza de la carretera
    class traza extends AsyncTask<String, String, String>

    {
        @Override
        protected String doInBackground(String... params) {
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //contexto

            if (traza) {
                //conectamos a la db y guardamos la respuesta en un string
                Context context = getApplicationContext();
                consultadb db = new consultadb(context);
                String[] trazado;
                //Vamos separando los valores que vienen unidos por comas y los almacenamos
                db.trazaCarretera();

            }
        }
    }

    //para mirar si hay conexion de datos
    protected Boolean conectadoRedMovil() {
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo info2 = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (info != null) {
                if (info.isConnected()) {
                    return true;
                }
            }
            if (info2 != null) {
                if (info2.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }
}