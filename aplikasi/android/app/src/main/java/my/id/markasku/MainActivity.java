package my.id.markasku;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AdzanPlugin.class);   // harus sebelum super.onCreate supaya tersedia di window.Capacitor
        super.onCreate(savedInstanceState);
    }
}
