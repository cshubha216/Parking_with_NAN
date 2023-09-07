package net.mobilewebprint.nan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.nan_home_layout);
        Button managerButton = findViewById(R.id.manager);
        final Button userButton = findViewById(R.id.user);
        managerButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          // Navigate to the parking lot connection screen
          Intent intent = new Intent(HomePageActivity.this, ParkAssistActivity.class);
          intent.putExtra("action", "manager");
          startActivity(intent);
        }
      });
        userButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomePageActivity.this, ParkAssistActivity.class);
                intent.putExtra("action", "user");
                startActivity(intent);
            }
        });

    }
}