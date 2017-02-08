package com.avaa.balitidewidget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;

import com.avaa.balitidewidget.data.Port;
import com.avaa.balitidewidget.widget.TideWidget;

public class NewWidgetConfigActivity extends AppCompatActivity implements ChoosePortFragment.ChoosePortFragmentListener {
    private int appWidgetID;
    private Port    selectedPort = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_widget_config);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        ChoosePortFragment cpf = (ChoosePortFragment)getSupportFragmentManager().findFragmentById(R.id.cpf);
        onPortSelected(cpf.getBestPort());
    }


    @Override
    public void onBackPressed() {
        if (findViewById(R.id.flChoosePort).getVisibility() == View.VISIBLE) {
            findViewById(R.id.flChoosePort).setVisibility(View.INVISIBLE);
        }
        else super.onBackPressed();
    }


    public void btnBackClick(View view) {
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetID);
        setResult(RESULT_CANCELED, resultValue);
        finish();
    }
    public void btnOkClick(View view) {
        if (selectedPort == null) return;

        SharedPreferences sp = getSharedPreferences(Common.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();

        edit.putString (TideWidget.SPKEY_PORT_ID + appWidgetID, selectedPort.id);
        edit.putString (TideWidget.SPKEY_PORT_NAME + appWidgetID, selectedPort.getName());
        edit.putString (TideWidget.SPKEY_PORT_POSITION + appWidgetID, selectedPort.position.latitude + " " + selectedPort.position.longitude);
        edit.putString (TideWidget.SPKEY_PORT_TIMEZONE + appWidgetID, selectedPort.timeZone.getID());
        edit.putBoolean(TideWidget.SPKEY_CROP + appWidgetID, ((RadioButton)findViewById(R.id.rbCrop)).isChecked());
        edit.putBoolean(TideWidget.SPKEY_SHOW_NAME + appWidgetID, ((RadioButton)findViewById(R.id.rbShow)).isChecked());

        edit.commit();

        new TideWidget.WidgetsUpdater(getApplicationContext()).run();

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetID);
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
    public void onPortSelected(Port port) {
        findViewById(R.id.flChoosePort).setVisibility(View.GONE);
        setSelectedPort(port);
    }


    public void setSelectedPort(Port port) {
        if (port == null) return;
        findViewById(R.id.btnOk).setVisibility(View.VISIBLE);
        ((EditText)findViewById(R.id.tvPort)).setText(port.getName());
        selectedPort = port;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ChoosePortFragment cpf = (ChoosePortFragment)getSupportFragmentManager().findFragmentById(R.id.cpf);
        cpf.onRequestPermissionsResult(requestCode, permissions, grantResults);
        setSelectedPort(cpf.getBestPort());
    }
}
