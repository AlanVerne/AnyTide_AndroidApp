package com.avaa.balitidewidget;

import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.avaa.balitidewidget.data.Port;
import com.avaa.balitidewidget.widget.TideWidget;

import java.util.Map;

public class NewWidgetConfigActivity extends AppCompatActivity implements ChoosePortFragment.ChoosePortFragmentListener {
    int    appWidgetId;
    String selectedPortID = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_widget_config);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        ChoosePortFragment cpf = (ChoosePortFragment)getSupportFragmentManager().findFragmentById(R.id.cpf);
        onPortSelected(cpf.getBestPort());
    }


    public void btnBackClick(View view) {
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);
        finish();
    }
    public void btnOkClick(View view) {
        if (selectedPortID == null) return;

        SharedPreferences sp = getSharedPreferences(Common.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();

        edit.putString (TideWidget.SPKEY_PORT_ID + appWidgetId, selectedPortID);
        edit.putBoolean(TideWidget.SPKEY_CROP + appWidgetId, ((RadioButton)findViewById(R.id.rbCrop)).isChecked());
        edit.putBoolean(TideWidget.SPKEY_SHOW_NAME + appWidgetId, ((RadioButton)findViewById(R.id.rbShow)).isChecked());

        edit.commit();

        new TideWidget.WidgetsUpdater(getApplicationContext()).run();

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
    public void onRadioButtonClicked(View view) {
        // update preview
    }
    public void choosePort(View view) {
        findViewById(R.id.flChoosePort).setVisibility(View.VISIBLE);

        ChoosePortFragment cpf = (ChoosePortFragment)getSupportFragmentManager().findFragmentById(R.id.cpf);
        cpf.activate();
    }


    @Override
    public void onPortSelected(Map.Entry<String, Port> port) {
        findViewById(R.id.flChoosePort).setVisibility(View.GONE);

        if (port == null) {
            findViewById(R.id.btnOk).setVisibility(View.INVISIBLE);
        }
        else {
            findViewById(R.id.btnOk).setVisibility(View.VISIBLE);
            ((EditText)findViewById(R.id.tvPort)).setText(port.getValue().getName());
            selectedPortID = port.getKey();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ChoosePortFragment cpf = (ChoosePortFragment)getSupportFragmentManager().findFragmentById(R.id.cpf);
        cpf.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
