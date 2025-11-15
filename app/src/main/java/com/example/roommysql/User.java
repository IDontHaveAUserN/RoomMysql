package com.example.roommysql;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String nome;
    public String email;

    public boolean isSynced = false;
    public String serverId;
}
