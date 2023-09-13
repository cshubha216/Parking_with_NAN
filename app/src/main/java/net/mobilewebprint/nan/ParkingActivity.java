package net.mobilewebprint.nan;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.mobilewebprint.nan.databinding.ParkingUserViewBinding;

public class ParkingActivity extends AppCompatActivity {
    private ParkingUserViewBinding parkingUserViewBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parkingUserViewBinding = ParkingUserViewBinding.inflate(getLayoutInflater());
        setContentView(parkingUserViewBinding.getRoot());

        addParkingSpot("Parking Spot 1", 100, 200);
        addParkingSpot("Parking Spot 2", 150, 200);
        addParkingSpot("Parking Spot 3", 200, 400);
    }

    private void addParkingSpot(final String spotName, int marginLeft, int marginTop) {
        ImageView parkingSpot = new ImageView(this);
        parkingUserViewBinding.parkingLayout.setLayoutParams(new RelativeLayout.LayoutParams(40, 40));

        parkingSpot.setBackgroundResource(R.drawable.parking_indicator_foreground);
        parkingSpot.setX(marginLeft);
        parkingSpot.setY(marginTop);
        parkingSpot.setClickable(true);
        parkingSpot.setFocusable(true);

        parkingSpot.setContentDescription(spotName);

        parkingSpot.setOnClickListener(
                view -> Toast.makeText(getApplicationContext(), "Car Parked", Toast.LENGTH_LONG).show()
        );

        parkingUserViewBinding.parkingLayout.addView(parkingSpot);
    }
}
