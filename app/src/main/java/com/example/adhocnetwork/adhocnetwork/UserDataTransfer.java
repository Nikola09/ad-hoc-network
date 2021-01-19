package com.example.adhocnetwork.adhocnetwork;

import java.io.Serializable;

public class UserDataTransfer implements Serializable {
    public String Name;
    public double Lat;
    public double Lon;
    //public Double TargetLat;
    //public Double TargetLon;
    public int BatteryLevel;
    public boolean HasTemperatureSensor;
    public boolean HasLightSensor;
    public boolean HasPressureSensor;
    public float Temperature;
    public float Light;
    public float Pressure;
    public int heartrate;
    public int upperPressure;
    public int lowerPressure;
    public int bloodOxygenLevel;
    public boolean healthAlarm;

    public UserDataTransfer(String n, double la, double lo,/* Double tla, Double tlo,*/int blevel,boolean hl, boolean ht, boolean hp, float l, float t, float p,int heart,int up, int lp, int ol, boolean hAlarm)
    {
        Name=n;
        Lat=la;
        Lon=lo;
        //TargetLat=tla;
        //TargetLon=tlo;
        BatteryLevel = blevel;
        HasTemperatureSensor = ht;
        HasLightSensor = hl;
        HasPressureSensor = hp;
        Temperature = t;
        Light = l;
        Pressure = p;
        heartrate = heart;
        upperPressure = up;
        lowerPressure = lp;
        bloodOxygenLevel = ol;
        healthAlarm = hAlarm;
    }
    public UserDataTransfer(){}
}
