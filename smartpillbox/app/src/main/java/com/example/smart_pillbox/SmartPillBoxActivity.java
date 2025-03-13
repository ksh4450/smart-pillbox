package com.example.smart_pillbox;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.content.SharedPreferences;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SmartPillBoxActivity extends AppCompatActivity implements BluetoothManager.ConnectionCallback {

    private static final int REQUEST_BLUETOOTH_CONNECTION = 2;

    private BluetoothManager bluetoothManager;
    private TextView tvIrValue1, tvIrValue2, tvIrValue3, tvIrValue4;
    private boolean stopThread;
    private static final String TAG = "SmartPillBoxActivity";
    private static final String PREFS_NAME = "MedicineIntakePrefs";
    private static final String KEY_INTAKE_TIMES = "intakeTimes";
    private static final long ONE_HOUR_MILLIS = 3600000; // 1시간을 밀리초로 표현

    private static final String CHANNEL_ID = "medicine_intake_channel";
    private static final String CHANNEL_NAME = "Medicine Intake";
    private static final int NOTIFICATION_ID = 1;


    private ArrayList<Long> intakeTimes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_pill_box);

        bluetoothManager = BluetoothManager.getInstance(this);
        bluetoothManager.setConnectionCallback(this);

        Button btnConnectBluetooth = findViewById(R.id.btnConnectBluetooth);
        Button btnDisconnectBluetooth = findViewById(R.id.btnDisconnectBluetooth);
        Button btnHome = findViewById(R.id.btnHome);
        Button btnSmartPillBox = findViewById(R.id.btnSmartPillBox);

        btnConnectBluetooth.setOnClickListener(v -> connectBluetooth());
        btnDisconnectBluetooth.setOnClickListener(v -> disconnectBluetooth());

        tvIrValue1 = findViewById(R.id.tvIrValue1);
        tvIrValue2 = findViewById(R.id.tvIrValue2);
        tvIrValue3 = findViewById(R.id.tvIrValue3);
        tvIrValue4 = findViewById(R.id.tvIrValue4);

        btnHome.setOnClickListener(v -> finish());

        /*btnSmartPillBox.setOnClickListener(v ->
                Toast.makeText(this, "현재 스마트 약통 화면입니다", Toast.LENGTH_SHORT).show());*/


        // 인텐트에서 디바이스 주소 가져오기
        String connectedDeviceAddress = getIntent().getStringExtra("DEVICE_ADDRESS");
        Log.d(TAG, "Received device address from intent: " + connectedDeviceAddress);

        if (connectedDeviceAddress != null) {
            Log.d(TAG, "Attempting to connect to received device address");
            bluetoothManager.connect(connectedDeviceAddress);
            readFromArduino();
        } else if (bluetoothManager.isConnected()) {
            Log.d(TAG, "BluetoothManager is already connected. Starting to read from Arduino.");
            readFromArduino();
        } else {
            Log.d(TAG, "No device address received and not connected. Showing toast.");
            Toast.makeText(this, "Bluetooth is not connected", Toast.LENGTH_SHORT).show();
        }
        loadIntakeTimes(); // 저장된 복용 시간 불러오기

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("스마트 약통 복용 알림");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Bluetooth connected successfully", Toast.LENGTH_SHORT).show();
            readFromArduino();
        });
    }

    @Override
    public void onConnectionFailed(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Bluetooth connection failed: " + error, Toast.LENGTH_SHORT).show();
        });
    }
    private void connectBluetooth() {
        if (!bluetoothManager.isConnected()) {
            Intent intent = new Intent(SmartPillBoxActivity.this, BluetoothActivity.class);
            startActivityForResult(intent, REQUEST_BLUETOOTH_CONNECTION);
        } else {
            Toast.makeText(this, "이미 연결되어 있습니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLUETOOTH_CONNECTION && resultCode == RESULT_OK) {
            String connectedDeviceAddress = data.getStringExtra("DEVICE_ADDRESS");
            Log.d(TAG, "Bluetooth connection successful. Device address: " + connectedDeviceAddress);

            // 디바이스 주소 저장
            SharedPreferences.Editor editor = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE).edit();
            editor.putString(MainActivity.LAST_CONNECTED_DEVICE, connectedDeviceAddress);
            editor.apply();

            Log.d(TAG, "Saved device address: " + connectedDeviceAddress);

            Toast.makeText(this, "Bluetooth connection successful!", Toast.LENGTH_SHORT).show();
            bluetoothManager.connect(connectedDeviceAddress);
            readFromArduino();
        }
    }
    private void disconnectBluetooth() {
        if (bluetoothManager.isConnected()) {
            stopThread = true;  // 데이터 읽기 스레드 중지
            bluetoothManager.disconnect();
            Toast.makeText(this, "블루투스 연결이 해제되었습니다", Toast.LENGTH_SHORT).show();
            // UI 업데이트
            updateUIForDisconnection();
        } else {
            Toast.makeText(this, "연결된 블루투스가 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUIForDisconnection() {
        runOnUiThread(() -> {
            tvIrValue1.setText("연결 끊김");
            tvIrValue2.setText("연결 끊김");
            tvIrValue3.setText("연결 끊김");
            tvIrValue4.setText("연결 끊김");
            tvIrValue1.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            tvIrValue2.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            tvIrValue3.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            tvIrValue4.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        });
    }
    private void readFromArduino() {
        stopThread = false;
        new Thread(() -> {
            try {
                InputStream inputStream = bluetoothManager.getInputStream();
                if (inputStream == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Bluetooth input stream is null", Toast.LENGTH_LONG).show());
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(bluetoothManager.getInputStream()));
                while (!stopThread) {
                    String line = reader.readLine();
                    if (line != null) {
                        if (line.contains("TILT,EVENT")) {
                            // 기울기 이벤트 감지
                            handleMedicineIntake();
                        }
                        // 기존의 IR 센서 데이터 처리
                        String[] values = line.split(",");
                        if (values.length == 4) {
                            runOnUiThread(() -> {
                                updateSensorView(tvIrValue1, "", values[0]);
                                updateSensorView(tvIrValue2, "", values[1]);
                                updateSensorView(tvIrValue3, "", values[2]);
                                updateSensorView(tvIrValue4, "", values[3]);
                            });
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleMedicineIntake() {
        long currentTime = System.currentTimeMillis();

        // 최근 1시간 동안의 복용 횟수 계산
        int recentIntakes = 0;
        for (int i = intakeTimes.size() - 1; i >= 0; i--) {
            if (currentTime - intakeTimes.get(i) <= ONE_HOUR_MILLIS) {
                recentIntakes++;
            }
        }

        // 새로운 복용 시간 추가
        intakeTimes.add(currentTime);
        saveIntakeTimes();

        // UI에 알림 표시
        final int finalIntakes = recentIntakes + 1;
        runOnUiThread(() -> {
            String message = "최근 1시간 내 " + finalIntakes + "회 복용하셨습니다.";

            // 토스트 메시지 표시
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            showNotification(message);
        });
    }

    private void showNotification(String message) {
        Intent intent = new Intent(this, SmartPillBoxActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("약 복용 알림")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Android 13 이상에서는 알림 권한 확인 필요
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없는 경우 권한 요청
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1);
            }
            return;
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void saveIntakeTimes() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 24시간이 지난 복용 기록 제거
        long currentTime = System.currentTimeMillis();
        intakeTimes.removeIf(time -> (currentTime - time) > (24 * ONE_HOUR_MILLIS));

        // 복용 시간들을 문자열로 변환하여 저장
        StringBuilder sb = new StringBuilder();
        for (Long time : intakeTimes) {
            sb.append(time).append(",");
        }
        editor.putString(KEY_INTAKE_TIMES, sb.toString());
        editor.apply();
    }

    private void loadIntakeTimes() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String timesStr = prefs.getString(KEY_INTAKE_TIMES, "");

        intakeTimes.clear();
        if (!timesStr.isEmpty()) {
            String[] times = timesStr.split(",");
            for (String time : times) {
                try {
                    intakeTimes.add(Long.parseLong(time));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateSensorView(TextView textView, String prefix, String value) {
        try {
            int intValue = Integer.parseInt(value);
            if (intValue <= 250) {
                textView.setText("O");  // 250 이하일 때 'O' 표시
                textView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                textView.setText("X");  // 250 초과일 때 'X' 표시
                textView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            }
        } catch (NumberFormatException e) {
            textView.setText("Error");
            textView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        }
    }



    @Override
    protected void onDestroy() {

        super.onDestroy();
        saveIntakeTimes();
    }
}
