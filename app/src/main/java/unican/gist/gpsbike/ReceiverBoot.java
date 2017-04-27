package unican.gist.gpsbike;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReceiverBoot extends BroadcastReceiver {



        @Override
        public void onReceive(Context context, Intent intent) {
            //se recibe la acción de arranque y se llama al otro método

                Intent serviceIntent = new Intent(context, serviceBoot.class);
                serviceIntent.setAction("gist.unican.MyService");
                context.startService(serviceIntent);


        }
}
