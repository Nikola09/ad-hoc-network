package com.example.adhocnetwork.adhocnetwork;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.nearby.connection.Payload;

import java.nio.charset.StandardCharsets;

public class SettingsActivity extends AppCompatActivity {

    private int minTime = 12;
    private int maxTime = 60;
    private int minVal = 6;
    private int maxVal = 50;
    private EditText settingsHrTime;
    private EditText settingsHrValue;
    private EditText settingsPrTime;
    private EditText settingsPrValue;
    private Button saveSettingsButton;
    private Button exitSettingsButton;
    private MyData mMyData;
    private ConnectionService mConnectionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        mMyData = MyData.getInstance();
        mConnectionService = ConnectionService.getInstance();

        settingsHrTime = findViewById(R.id.txtAlarmTime);
        settingsHrValue = findViewById(R.id.txtAlarmValue);
        settingsHrTime.setText(String.valueOf(mMyData.getHrTimeSetting()));
        settingsHrValue.setText(String.valueOf(mMyData.getHrValueSetting()));
        settingsPrTime = findViewById(R.id.txtPressureTime);
        settingsPrValue = findViewById(R.id.txtPressureValue);
        settingsPrTime.setText(String.valueOf(mMyData.getPrTimeSetting()));
        settingsPrValue.setText(String.valueOf(mMyData.getPrValueSetting()));

        saveSettingsButton = findViewById(R.id.btnSaveSettings);
        exitSettingsButton = findViewById(R.id.btnExitSettings);

        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int hrTime = CorrectedValue(minTime,maxTime ,mMyData.getHrTimeSetting(), settingsHrTime.getText().toString());
                int hrValue = CorrectedValue(minVal,maxVal ,mMyData.getHrValueSetting(), settingsHrValue.getText().toString());
                int prTime = CorrectedValue(minTime,maxTime ,mMyData.getPrTimeSetting(), settingsPrTime.getText().toString());
                int prValue = CorrectedValue(minVal,maxVal ,mMyData.getPrValueSetting(), settingsPrValue.getText().toString());
               mMyData.updateFakeSensorSettings(hrTime,hrValue,prTime,prValue);
                String settingsMessage = "cmd_settings=" + hrTime +"="+ hrValue + "=" + prTime +"="+ prValue;
                Payload settingsPayload = Payload.fromBytes(settingsMessage.getBytes(StandardCharsets.UTF_8));
                mConnectionService.send(settingsPayload);
               finish();
            }
        });
        exitSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }
    private int CorrectedValue(int min,int max,int defaultValue, String value)
    {
        int result;
        try {
            result = Integer.parseInt(value);
        }
        catch (Exception e)
        {
            result = defaultValue;
        }
        if (result < min)
        {
            result = min;
        }
        else if (result > max)
        {
            result = max;
        }
        return result;
    }


}