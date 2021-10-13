package com.example.adhocnetwork.adhocnetwork;

import com.example.adhocnetwork.adhocnetwork.ui.main.CreateList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MyData {
    private static final MyData ourInstance = new MyData();

    public static MyData getInstance()
    {
        return ourInstance;
    }

    private String name;
    private  String showName;
    private double latitude;
    private double longitude;
    private Double targetLatitude;
    private Double targetLongitude;
    private int batteryLevel;
    private boolean hasTemperatureSensor;
    private boolean hasLightSensor;
    private boolean hasPressureSensor;
    private float temperature;
    private float light;
    private float pressure;

    private int hrTimeSetting = 18;
    private int hrValueSetting = 10;
    private int hrStartingValue = 70;
    private int hrMaximum = 105;
    private int hrMinimum = 55;
    private int hrChange = 8;

    private int pTimeSetting = 18;
    private int pValueSetting = 20;
    private int upStartingValue = 120;
    private int lpStartingValue = 80;
    private int upMaximum = 140;
    private int upMinimum = 100;
    private int lpMaximum = 100;
    private int lpMinimum = 60;
    private int pChange = 8;

    private int olTimeSetting = 18;
    private int olValueSetting = 10;
    private int olStartingValue = 96;
    private int olMaximum = 103;
    private int olMinimum = 85;
    private int olChange = 2;

    private int resendTime = 6;
    public FakeSensor heartrate;
    public FakeSensor upperPressure;
    public FakeSensor lowerPressure;
    public FakeSensor bloodOxygenLevel;

    private UserData following;
    private boolean amFlagging;

    private ArrayList<UserData> users;
    private ArrayList<ChatMessage> messages;
    private ArrayList<CreateList> images;

    private MyData()
    {
        users = new ArrayList<UserData>();
        images = new ArrayList<CreateList>();
        messages = new ArrayList<ChatMessage>();
        heartrate = new FakeSensor(hrTimeSetting/resendTime,hrValueSetting,hrStartingValue,hrMaximum,hrMinimum,hrChange);
        upperPressure = new FakeSensor(pTimeSetting/resendTime,pValueSetting, upStartingValue,upMaximum,upMinimum,pChange);
        lowerPressure = new FakeSensor(pTimeSetting/resendTime, pValueSetting, lpStartingValue,lpMaximum,lpMinimum,pChange);
        bloodOxygenLevel = new FakeSensor(olTimeSetting/resendTime,olValueSetting,olStartingValue,olMaximum,olMinimum,olChange);
    }

    public ArrayList<UserData> getUsers() {
        return users;
    }

    public ArrayList<ChatMessage> getMessages() {
        return messages;
    }
    public void addMessage(ChatMessage cm) {
        messages.add(cm);
    }
    public void clearMessages() {
        messages.clear();
    }

    public void setMyInitialValues(String nam, double lat, double lon) {
        name = nam;
        showName = nam.split("=")[0];
        latitude = lat;
        longitude = lon;
        targetLatitude = null;
        targetLongitude = null;
        amFlagging = false;
        following = null;
        batteryLevel = 100;
        hasTemperatureSensor = false;
        hasLightSensor = false;
        hasPressureSensor = false;
        temperature = -1;
        light = -1;
        pressure = -1;
    }

    public UserDataTransfer getMyDataTransfer() {
        return new UserDataTransfer(name,latitude, longitude,batteryLevel,
                hasLightSensor,hasTemperatureSensor,hasPressureSensor,light,temperature,pressure,heartrate.sensorValue,upperPressure.sensorValue,lowerPressure.sensorValue,bloodOxygenLevel.sensorValue,heartrate.sensorAlarm);
    }

    public void setMyLocation(double lat, double lon)
    {
        latitude = lat;
        longitude = lon;
    }

    public UserData getFollowing() {
        return following;
    }

    public void setFollowing(UserData following) {
        this.following = following;
    }

    public void addUser(UserData ud)
    {
        users.add(ud);
    }

    public int removeUser(String userFullName)
    {
        for (int i=0; i<users.size(); i++) {
            if(users.get(i).Name.equals(userFullName))
            {
                users.remove(i);
                return i;
            }
        }
        return 0;
    }

    public int refreshUser(UserDataTransfer u)
    {
        for (int i=0; i<users.size(); i++) {
            if(users.get(i).Name.equals(u.Name))
            {
                users.get(i).SetUserData(u);
                users.get(i).CalculateDistance(this.latitude, this.longitude);

                return i;
            }
        }
        return 0;
    }
    public boolean containsUserByName(String name)
    {
        boolean containsUser = false;
        for (int i=0; i<users.size(); i++)
        {
            if (users.get(i).Name.equals(name))
            {
                containsUser = true;
            }
        }
        return containsUser;
    }
    public void clearUsers()
    {
        users.clear();
    }
    public static String getUserSensorsString(UserData user)
    {
        String str = "Battery:" + user.BatteryLevel + "% Ambient: ";
        if (user.HasLightSensor)
            str = str.concat(user.Light + "lux ");
        if (user.HasTemperatureSensor)
            str = str.concat(user.Temperature + "°C ");
        if (user.HasPressureSensor)
            str = str.concat(user.Pressure + "mbar");
        return str;
    }
    public static String getUserFakeHealthSensorsString(UserData user)
    {
        return "Health: " + user.upperPressure + "/" + user.lowerPressure + "  " + user.heartrate + " BPM " + user.bloodOxygenLevel + "%";
    }
    public String getMySensorsString()
    {
        String str = "Battery:" + batteryLevel + "% Ambient: ";
        if (hasLightSensor)
            str = str.concat(light + "lux ");
        if (hasTemperatureSensor)
            str = str.concat(temperature + "°C ");
        if (hasPressureSensor)
            str = str.concat(pressure + "mbar");
        return str;
    }
    public String getMyFakeHealthSensorsString()
    {
        String str = "Health: "+ upperPressure.sensorValue + "/"+ lowerPressure.sensorValue + "  " + heartrate.sensorValue + " BPM " + bloodOxygenLevel.sensorValue +"%";
        return str;
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public String getName() {
        return name;
    }
    public String getShowName() {
        return showName;
    }
    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getTargetLatitude() {
        return targetLatitude;
    }
    public void setTargetLatitude(Double targetLatitude) {
        this.targetLatitude = targetLatitude;
    }

    public Double getTargetLongitude() {
        return targetLongitude;
    }
    public void setTargetLongitude(Double targetLongitude) {
        this.targetLongitude = targetLongitude;
    }
    public void setMyTargetFlag(boolean iflag, Double lon, Double lat)
    {
        this.amFlagging = iflag;
        this.targetLongitude = lon;
        this.targetLatitude = lat;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }
    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public boolean getHasTemperatureSensor() {
        return hasTemperatureSensor;
    }
    public void setHasTemperatureSensor(boolean hasTemperatureSensor) {
        this.hasTemperatureSensor = hasTemperatureSensor;
    }

    public boolean getHasLightSensor() {
        return hasLightSensor;
    }
    public void setHasLightSensor(boolean hasLightSensor) {
        this.hasLightSensor = hasLightSensor;
    }

    public boolean getHasPressureSensor() {
        return hasPressureSensor;
    }
    public void setHasPressureSensor(boolean hasPressureSensor) {
        this.hasPressureSensor = hasPressureSensor;
    }

    public float getTemperature() {
        return temperature;
    }
    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }
    public float getLight() {
        return light;
    }
    public void setLight(float light) {
        this.light = light;
    }

    public boolean isAmFlagging() {
        return amFlagging;
    }

    public float getPressure() {
        return pressure;
    }
    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public void addImage(CreateList cl)
    {
        images.add(cl);
    }
    public ArrayList<CreateList> returnImages()
    {
        return images;
    }

    public int getHrTimeSetting() {
        return hrTimeSetting;
    }
    public int getHrValueSetting() {
        return hrValueSetting;
    }

    public int getPrTimeSetting() {
        return pTimeSetting;
    }
    public int getPrValueSetting() {
        return pValueSetting;
    }

    public void updateFakeSensorSettings(int hrTime,int hrValue, int pressureTime, int pressureValue)
    {
        hrTimeSetting = hrTime;
        hrValueSetting = hrValue;
        heartrate = new FakeSensor(hrTime/resendTime, hrValue,hrStartingValue,hrMaximum,hrMinimum,hrChange);

        pTimeSetting = pressureTime;
        pValueSetting = pressureValue;
        upperPressure = new FakeSensor(pressureTime/resendTime, pressureValue, upStartingValue,upMaximum,upMinimum,pChange);
        lowerPressure = new FakeSensor(pressureTime/resendTime, pressureValue, lpStartingValue,lpMaximum,lpMinimum,pChange);
    }

    public void updateFakeSensors()
    {
        heartrate.updateFakeSensor();
        upperPressure.updateFakeSensor();
        lowerPressure.updateFakeSensor();
        bloodOxygenLevel.updateFakeSensor();
        if(bloodOxygenLevel.sensorValue > 100) bloodOxygenLevel.sensorValue=100;
    }
    public boolean getHealthAlarm()
    {
        return heartrate.sensorAlarm && upperPressure.sensorAlarm && lowerPressure.sensorAlarm && bloodOxygenLevel.sensorAlarm;
    }
}
