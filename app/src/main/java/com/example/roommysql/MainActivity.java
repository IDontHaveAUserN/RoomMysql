package com.example.roommysql;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.room.Room;

import com.example.roommysql.databinding.ActivityMainBinding;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private UserDao userDao;
    private ExecutorService executorService;
    private AppDatabase db;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.out.println("CRASH: " + throwable.getMessage());
            throwable.printStackTrace();
        });
        setupDatabase();
        setupOnClick();

        loadUsers();
    }

    private void setupDatabase(){
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "user-database")
                .fallbackToDestructiveMigration() 
                .allowMainThreadQueries()
                .build();

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        userRepository = new UserRepository(db.userDao(), apiService);
        //userDao = db.userDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    private void setupOnClick(){
        binding.btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createUser();
            }
        });

        binding.btnGetUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUsers();
            }
        });

        binding.btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncFromServer();
            }
        });
    }

    private void createUser() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

       // executorService.execute(new Runnable() {
         //   @Override
           // public void run() {
                User user = new User();
                user.nome = name;
                user.email = email;
                user.isSynced = false;

                userRepository.insertUser(user, () -> {
                    Toast.makeText(MainActivity.this, "User created locally. Syncing...", Toast.LENGTH_SHORT).show();
                    binding.etName.setText("");
                    binding.etEmail.setText("");

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loadUsers();
                        }
                    }, 1000);
                });


                /*userDao.insert(user);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "User created successfully", Toast.LENGTH_SHORT).show();
                        binding.etName.setText("");
                        binding.etEmail.setText("");
                    }
                });*/
           // }
        //});
    }

    private void loadUsers() {
        userRepository.getAllUsers(new UserRepository.UserListCallback() {
            @Override
            public void onUsersLoaded(List<User> users) {
                displayUsers(users);
            }
        });
    }

    private void syncAllUsers() {
        userRepository.syncAllUnsyncedUsers();
        Toast.makeText(this, "Syncing unsynced users...", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(this::loadUsers, 2000);
        /*executorService.execute(new Runnable() {
            @Override
            public void run() {
                List<User> users = userDao.getAll();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayUsers(users);
                    }
                });
            }
        });*/
    }

    private void syncFromServer() {
        Toast.makeText(this, "Syncing from server...", Toast.LENGTH_SHORT).show();
        userRepository.fetchUsersFromServer(() -> {
            Toast.makeText(this, "Server users synced!", Toast.LENGTH_SHORT).show();
            loadUsers();
        });
    }

    private void displayUsers(List<User> users) {
        if (users.isEmpty()) {
            binding.tvResult.setText("No user found");
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("User (Local + Remote):\n\n");

        for (User user : users) {
            stringBuilder.append("ID: ").append(user.id)
                    .append("\nName: ").append(user.nome)
                    .append("\nEmail: ").append(user.email)
                    .append("\nStatus: ").append(user.isSynced ? "✓ Synced" : "⏳ Pending Sync")
                    .append("\n\n");
        }
        binding.tvResult.setText(stringBuilder.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}