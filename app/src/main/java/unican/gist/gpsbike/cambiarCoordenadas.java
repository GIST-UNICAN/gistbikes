package unican.gist.gpsbike;

import android.util.Log;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;

import static uk.me.jstott.jcoord.UTMRef.getUTMLatitudeZoneLetter;

/**
 * Created by andres on 30/4/16.
 */
public class cambiarCoordenadas {
    private LatLng t0;
    private UTMRef t2;
    private LatLng t1;
    private double[] resultado={0,0};
    private double[] posicion={0,0};
    public double[] latilongi(double[] posicion){
        t0=new LatLng(posicion[0], posicion[1]);
        resultado[0]=t0.toUTMRef().getEasting();
        resultado[1]=t0.toUTMRef().getNorthing();
        return resultado;
    }
    public double[] utm(double[] posicion){
        String t="T";
        char ts =t.charAt(0);
        t2=new UTMRef(30,'T',posicion[0],posicion[1]);
        resultado[0]=t2.toLatLng().getLatitude();
        resultado[1]=t2.toLatLng().getLongitude();
        return resultado;
    }
    public double distancia (double[] posicion1, double[] posicion2 ){
        t0=new LatLng(posicion1[0], posicion1[1]);
        t1=new LatLng(posicion2[0], posicion2[1]);
        return t0.distance(t1)*1000;

    }

}
