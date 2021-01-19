package com.example.adhocnetwork.adhocnetwork;

import com.google.android.gms.maps.model.LatLng;

public class UserData {
    public String Name;
    public String ShowName;
    public String EndpointId;
    public double Lat;
    public double Lon;
    public double Distance;
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

    public UserData(String n,String ei, double la, double lo)
    {
        Name=n;
        ShowName = n.split("=")[0];
        EndpointId=ei;
        Lat=la;
        Lon=lo;
        Distance = 0;

        BatteryLevel = 100;
        HasTemperatureSensor = false;
        HasLightSensor = false;
        HasPressureSensor = false;
        Temperature = -1;
        Light = -1;
        Pressure = -1;
        heartrate = 70;
        upperPressure = 120;
        lowerPressure = 80;
        bloodOxygenLevel = 95;
        healthAlarm = false;
    }

    public UserData(){}

    public void  SetUserData(UserDataTransfer udt)
    {
        //Name=udt.Name;
        //ShowName = udt.Name.split("=")[0];
        Lat=udt.Lat;
        Lon=udt.Lon;

        BatteryLevel = udt.BatteryLevel;
        HasTemperatureSensor = udt.HasTemperatureSensor;
        HasLightSensor = udt.HasLightSensor;
        HasPressureSensor = udt.HasPressureSensor;
        Temperature = udt.Temperature;
        Light = udt.Light;
        Pressure = udt.Pressure;
        heartrate = udt.heartrate;
        upperPressure = udt.upperPressure;
        lowerPressure = udt.lowerPressure;
        bloodOxygenLevel = udt.bloodOxygenLevel;
        healthAlarm = udt.healthAlarm;
    }
    public void CalculateDistance(double my_lat, double my_lon)
    {
        Distance = meterDistanceBetweenPoints(Lat, Lon, my_lat, my_lon);
    }

    public double meterDistanceBetweenPoints( double lat_a,  double lng_a,  double lat_b,  double lng_b) {
        double pk = (180.f/Math.PI);
        double a1 = lat_a / pk;
        double a2 = lng_a / pk;
        double b1 = lat_b / pk;
        double b2 = lng_b / pk;

        double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
        double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
        double t3 = Math.sin(a1) * Math.sin(b1);
        double tt = Math.acos(t1 + t2 + t3);

        return 6366000 * tt;
    }

    public LatLng getLatLng()
    {
        return new LatLng(Lat, Lon);
    }
}
