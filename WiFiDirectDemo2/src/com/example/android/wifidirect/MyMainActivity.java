package com.example.android.wifidirect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

/**
 * Created by AT DR on 08-05-2015.
 * .
 */

public class MyMainActivity extends Activity {
    MyMainActivity myThis;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_main_activity);

        myThis = this;

        findViewById(R.id.btnGroupOwner).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = getApplicationContext();
                        CharSequence text = "Group Owner!!!!!";
                        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                        toast.show();

                        Intent intent = new Intent(myThis, WiFiDirectActivity.class);
                        intent.putExtra("role", "GO");
                        startActivity(intent);

                    }
                });

        findViewById(R.id.btnRelay).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = getApplicationContext();
                        CharSequence text = "Relay!!!!!";
                        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                        toast.show();

                        Intent intent = new Intent(myThis, WiFiDirectActivity.class);
                        intent.putExtra("role", "Relay");
                        startActivity(intent);
                    }
                });

        findViewById(R.id.btnClient).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = getApplicationContext();
                        CharSequence text = "Simple Client!!!!!";
                        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                        toast.show();

                        Intent intent = new Intent(myThis, WiFiDirectActivity.class);
                        intent.putExtra("role", "Client");
                        startActivity(intent);
                    }
                });
    }
}