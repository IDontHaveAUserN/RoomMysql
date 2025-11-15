package com.example.roommysql;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);

    @Update
    void updateUser(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM user")
    List<User> getAll();

    @Query("SELECT * FROM user WHERE isSynced = 0")
    List<User> getUnsyncedUsers();

    @Query("UPDATE user SET isSynced = 1, serverId = :serverId WHERE id = :localId")
    void markAsSynced(int localId, String serverId);

    @Query("SELECT * FROM user WHERE serverId = :serverId")
    User getByServerId(String serverId);
}
