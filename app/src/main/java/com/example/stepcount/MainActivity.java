package com.example.stepcount;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.stepcount.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    GoogleSignInAccount account;
    FitnessOptions fitnessOptions;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .build();

        account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED){
            requestLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        else{
            if(!GoogleSignIn.hasPermissions(account, fitnessOptions)){
                GoogleSignIn.requestPermissions(MainActivity.this, 1, account, fitnessOptions);
            }
            else {
                readData();
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.refresh){

                binding.progressBar.setProgress(0);
                readData();
        }
        return super.onOptionsItemSelected(item);
    }

    private void readData() {

        /*Fitness.getSensorsClient(this, account)
                        .add(
                                new SensorRequest.Builder()
                                        .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                                        .setSamplingRate(10, TimeUnit.SECONDS)
                                        .setAccuracyMode(3)
                                        .build()
                                , new OnDataPointListener() {
                                    @Override
                                    public void onDataPoint(@NonNull DataPoint dataPoint) {

                                        for(Field field : dataPoint.getDataType().getFields()){
                                            int stepCount = dataPoint.getValue(field).asInt();
                                             binding.stepCount.setText(String.valueOf(stepCount));
                                        }
                                        binding.stepCount.setText(String.valueOf(dataPoint.getValue(Field.FIELD_STEPS).asInt()));
                                    }
                                }
                        );
        

            */

        Fitness.getHistoryClient(MainActivity.this, account)
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(new OnSuccessListener<DataSet>() {
                    @Override
                    public void onSuccess(DataSet dataSet) {
                        binding.stepCount.setText(dataSet.isEmpty() ?  String.valueOf(0) : String.valueOf(dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt()));
                        binding.progressBar.setProgress(Integer.parseInt(binding.stepCount.getText().toString()), true);
                        if(binding.progressBar.getMax() == Integer.parseInt(binding.stepCount.getText().toString())){
                            Toast.makeText(MainActivity.this, "Maximum daily limit reached", Toast.LENGTH_SHORT).show();
                        }
                        else if(binding.progressBar.getMax() < Integer.parseInt(binding.stepCount.getText().toString())){
                            Toast.makeText(MainActivity.this, "Daily limit Exceed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK && requestCode == 1){

            Fitness.getRecordingClient(this, account)
                    .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                           // Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                            readData();
                        }
                    });
        }
        else Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
    }

    ActivityResultLauncher<String> requestLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if(result){
                if(!GoogleSignIn.hasPermissions(account, fitnessOptions)){
                    GoogleSignIn.requestPermissions(MainActivity.this, 1, account,fitnessOptions);
                }
                else {
                    readData();
                }
            }
        }
    });

    @Override
    protected void onResume() {
        binding.progressBar.setProgress(0);
        readData();
        super.onResume();
    }
}