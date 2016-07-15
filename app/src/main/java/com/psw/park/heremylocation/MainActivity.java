package com.psw.park.heremylocation;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button btnSendGPS = null;
    Button btnSendIntent = null;

    Location loc   = null;
    String sNumber = null;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // 롤리팝의 퍼미션용
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("AAAA", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setGPSPemission();
        setUpUI();
    }

    // 롤리팝을 위한 메소드
    private void setGPSPemission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);

            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // URL Scheme으로 왔다면 파라메터로 넘어온 전화번호를 읽어와서
        // SMS를 보낸다.
        Intent i = getIntent();
        if(i != null){
            Uri uri = i.getData();
            if (uri == null) return;

            // 전화번호를 저장하고 3초후에 sms 보낸다.
            sNumber = uri.getQueryParameter("number");
            doSendMessageDelay();
        }
    }

    private void setUpUI() {
        // GPS 모니터링!
        TrackMyPoint();

        btnSendGPS = (Button) findViewById(R.id.btnSendGPS);
        btnSendGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loc != null) {
                    sendLocationSMS("0103003000", loc);
                }

            }
        });

        btnSendIntent = (Button) findViewById(R.id.btnSendIntent);
        btnSendIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "여깁니다!");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, makeURLText("여기입니다", loc));
                startActivity(Intent.createChooser(sharingIntent, ""));

            }
        });
    }

    public void TrackMyPoint(){
        // system Location Manager
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            boolean gps_enabled = false;
            boolean network_enabled = false;

            // GPS 정보상태를 가져오는 메소드(isProviderEnabled) 리턴값으로 부울린값을 넘긴다.
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if(gps_enabled == false){
                Toast.makeText(getApplicationContext(), "gps 세팅이 안되었습니다", Toast.LENGTH_LONG).show();
                return;
            }

            if(network_enabled == false){
                Toast.makeText(getApplicationContext(), "gps 네트웍 설정이 안되었습니다", Toast.LENGTH_LONG).show();
                return;
            }

        } catch(Exception ex) {

        }

        // Listener 추가하기
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                loc = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, locationListener);
    }

    // 보낼 문자열을 만듭니다.
    private String makeURLText(String sTitle, Location currentLocation){
        StringBuffer smsBody = new StringBuffer();
        smsBody.append(sTitle + "\nhttp://maps.google.com/maps?q=");
        smsBody.append(currentLocation.getLatitude());
        smsBody.append(",");
        smsBody.append(currentLocation.getLongitude());

        return smsBody.toString();

    }
    // 위치 SMS 보내기!! Google 지도!
    public void sendLocationSMS(String phoneNumber, Location currentLocation) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, makeURLText("여기입니다", currentLocation), null, null);

        Toast.makeText(getApplicationContext(), "보냈습니다.", Toast.LENGTH_LONG).show();
    }

    // 5초 Dealy후 실행하는 메소드
    Handler handler;
    private void doSendMessageDelay(){
        handler = new Handler() {
            // sendEmptyMessage???() 메소드실행하면 호출되는 비동기 메소드
            public void handleMessage(Message msg) {
                if(sNumber == null || loc == null) return;
                sendLocationSMS(sNumber, loc);
                sNumber = null;
            }
        };
        // 5초 후 호출
        handler.sendEmptyMessageDelayed(0, 5000);
    }
}
