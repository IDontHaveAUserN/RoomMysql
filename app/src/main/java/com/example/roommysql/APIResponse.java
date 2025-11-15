package com.example.roommysql;

public class APIResponse {
    public String status;
    public String message;
    public String id;

    public UserData[] data;

    public static class UserData {
        public int id;
        public String nome;
        public String email;
    }
}
