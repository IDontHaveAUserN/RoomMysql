package com.example.roommysql;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @POST("user.php")
    Call<APIResponse> createUser(@Body UserRequest user);

    @GET("getUser.php")
    Call<APIResponse> getUsersFromServer();
}
