package com.example.roommysql;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;


import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private UserDao userDao;
    private ApiService apiService;
    private ExecutorService executorService;

    public UserRepository(UserDao userDao, ApiService apiService) {
        this.userDao = userDao;
        this.apiService = apiService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insertUser(User user, Runnable onComplete) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Inserting user locally: " + user.nome);
                long id = userDao.insert(user);
                user.id = (int) id;
                Log.d(TAG, "User inserted locally, ID: " + user.id);

                syncUserToServer(user);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting user: " + e.getMessage());
                e.printStackTrace();
            }
            new Handler(Looper.getMainLooper()).post(onComplete);
        });
    }

    private void syncUserToServer(User user) {
        try {
            Log.d(TAG, "Syncing user to server: " + user.nome);
            UserRequest userRequest = new UserRequest(user.nome, user.email);
            Response<APIResponse> response = apiService.createUser(userRequest).execute();

            Log.d(TAG, "Server response code: " + response.code());
            Log.d(TAG, "Server response body: " + response.body());

            if (response.isSuccessful() && response.body() != null) {
                Log.d(TAG, "Response status: " + response.body().status);
                if ("success".equals(response.body().status)) {
                    user.isSynced = true;
                    userDao.updateUser(user);
                    Log.d(TAG, "User synced successfully: " + user.nome);
                } else {
                    Log.d(TAG, "Server returned error: " + response.body().message);
                }
            } else {
                Log.d(TAG, "Response not successful: " + response.message());
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error during sync: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during sync: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void getAllUsers(UserListCallback callback) {
        executorService.execute(() -> {
            try {
                List<User> users = userDao.getAll();
                Log.d(TAG, "Retrieved " + users.size() + " users from local DB");
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onUsersLoaded(users);
                    Log.d(TAG, "Callback executed with " + users.size() + " users");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error getting users: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void syncAllUnsyncedUsers() {
        executorService.execute(() -> {
            try {
                List<User> unsyncedUsers = userDao.getUnsyncedUsers();
                Log.d(TAG, "Found " + unsyncedUsers.size() + " unsynced users");
                for (User user : unsyncedUsers) {
                    syncUserToServer(user);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing all users: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public interface UserListCallback {
        void onUsersLoaded(List<User> users);
    }

    public void fetchUsersFromServer(Runnable onComplete) {
        executorService.execute(() -> {
            try {
                Response<APIResponse> response = apiService.getUsersFromServer().execute();
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().status)) {
                    List<APIResponse.UserData> data = List.of(response.body().data);
                    for (APIResponse.UserData item : data) {
                        if (userDao.getByServerId(String.valueOf(item.id)) == null) {
                            User user = new User();
                            user.nome = item.nome;
                            user.email = item.email;
                            user.isSynced = true;
                            user.serverId = String.valueOf(item.id);
                            userDao.insert(user);
                        }
                    }
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching users: " + e.getMessage());
            }
        });
    }

}
