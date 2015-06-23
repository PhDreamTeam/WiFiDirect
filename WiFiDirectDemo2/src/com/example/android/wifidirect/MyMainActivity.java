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
    Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_main_activity);
        context = getApplicationContext();
        myThis = this;

        findViewById(R.id.btnWFDGroupOwner).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Group Owner!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, WiFiDirectActivity.class);
                        intent.putExtra("role", "GO");
                        startActivity(intent);
                    }
                });

        findViewById(R.id.btnWFDClient).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Wi-Fi Direct Client!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, WiFiDirectActivity.class);
                        intent.putExtra("role", "Client");
                        startActivity(intent);
                    }
                });

        findViewById(R.id.btnWFDAutoClient).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Wi-Fi Direct Auto Client!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, AutoClientActivity.class);
                        intent.putExtra("role", "Client");
                        startActivity(intent);
                    }
                });

        findViewById(R.id.btnRelay).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Relay!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, RelayActivity.class);
                        //intent.putExtra("role", "Relay");
                        // teste DR
//                        String CRPort = ((EditText) findViewById(R.id.editTextCRPortNumber)).getText().toString();
//                        Toast toast2 = Toast.makeText(context, CRPort, Toast.LENGTH_SHORT);
//                        toast2.show();
                        //   intent.putExtra("CrPortNumber", ((EditText) findViewById(R.id.editTextCRPortNumber)).getText().toString());

                        startActivity(intent);
                    }
                });

        findViewById(R.id.btnClient).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "Simple Client!!!!!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(myThis, ClientActivity.class);
//                        intent.putExtra("role", "Client");
//                        intent.putExtra("CrPortNumber", ((EditText) findViewById(R.id.editTextCRPortNumber2)).getText().toString());
//                        intent.putExtra("CrIpAddress", ((EditText) findViewById(R.id.editTextCrIpAddress)).getText().toString());
                        //TODO...
                        startActivity(intent);
                    }
                });
    }
}