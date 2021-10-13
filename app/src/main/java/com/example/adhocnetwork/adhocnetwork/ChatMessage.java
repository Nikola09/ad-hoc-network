package com.example.adhocnetwork.adhocnetwork;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    public String UserName;
    public String CreatedTime;
    public String Message;
    public boolean CreatedByCurrentUser;

    public ChatMessage(String name, String time, String msg, boolean createdByCurrent)
    {
        UserName = name;
        CreatedTime = time;
        Message = msg;
        CreatedByCurrentUser = createdByCurrent;
    }

    public ChatMessage sendThisMessage()
    {
        return new ChatMessage(UserName, CreatedTime,Message,false);
    }
}
