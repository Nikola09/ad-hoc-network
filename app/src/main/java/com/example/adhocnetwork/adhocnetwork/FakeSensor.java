package com.example.adhocnetwork.adhocnetwork;

import java.util.Random;

public class FakeSensor {  //public class FakeSensor<T> to be generic if needed & changed value's int to T
    //fake sensor
    public int setSensorTimeCount;
    public int setSensorTriggerValue;
    public int[] previousValues;
    public int sensorValue;
    public int sensorMaximumValue;
    public int sensorMinimumValue;
    public int sensorValueChange;
    public boolean sensorAlarm;

    public FakeSensor(int tc, int tv, int val,int maximum, int minimum, int change)
    {
        setSensorTimeCount = tc;
        setSensorTriggerValue = tv;
        sensorMaximumValue = maximum;
        sensorMinimumValue = minimum;
        sensorValueChange = change;
        previousValues = new int[setSensorTimeCount];
        sensorValue = val;
        sensorAlarm = false;
        for (int i = 0; i < setSensorTimeCount; i++)
        {
            previousValues[i] = sensorValue;
        }
    }

    public int updateFakeSensor()
    {
        sensorAlarm = false;
        Random rand = new Random();
        int val = rand.nextInt(sensorValueChange*3) - Math.round( sensorValueChange*1.5f);
        val = val % sensorValueChange;
        sensorValue = sensorValue + val;

        for (int i = 0; i < (previousValues.length-1); i++)
        {
            previousValues[i] = previousValues[i+1];

            if(Math.abs(previousValues[i] - sensorValue) > setSensorTriggerValue)
            {
                sensorAlarm = true;
            }
        }
        if (sensorValue < sensorMinimumValue || sensorValue > sensorMaximumValue)
        {
            sensorAlarm = true;
        }

        previousValues[previousValues.length-1] = sensorValue;

        return sensorValue;
    }
}
