package unican.gist.gpsbike.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;

import unican.gist.gpsbike.Polygon;

/**
 * Created by andres on 25/04/2017.
 */

public class Utils {
    Context context;

    public Utils(Context context) {
        this.context = context;
    }

    public int getVersionFromPreferences() {
        SharedPreferences prefs = context.getSharedPreferences("BIKEPREFERENCES", context.MODE_PRIVATE);
        int preferencesVersion = prefs.getInt("version", 0);
        return preferencesVersion;
    }
    public void setVersionToPreferences(int version) {
        SharedPreferences prefs = context.getSharedPreferences("BIKEPREFERENCES", context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putInt("version",version);
        prefsEditor.commit();

    }

    public void setPoligonsToPreferences(ArrayList<Polygon> poligonos) {
        SharedPreferences mPrefs = context.getSharedPreferences("BIKEPREFERENCES", context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(poligonos);
        prefsEditor.putString("POLIGONOS", json);
        prefsEditor.commit();

    }

    public ArrayList<Polygon> getPoligonsFromPreferences() {
        SharedPreferences mPrefs = context.getSharedPreferences("BIKEPREFERENCES", context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = mPrefs.getString("POLIGONOS", "");
        ArrayList<Polygon> obj = gson.fromJson(json, ArrayList.class);
        return obj;
    }

    public void setTrazaToPreferences(double[] puntosTraza) {
        SharedPreferences mPrefs = context.getSharedPreferences("BIKEPREFERENCES", context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(puntosTraza);
        prefsEditor.putString("TRAZA", json);
        prefsEditor.commit();

    }
    public double[] getPointsFromPreferences() {
        SharedPreferences mPrefs = context.getSharedPreferences("BIKEPREFERENCES", context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = mPrefs.getString("TRAZA", "");
        double obj[] = gson.fromJson(json, double[].class);
        return obj;
    }
}
