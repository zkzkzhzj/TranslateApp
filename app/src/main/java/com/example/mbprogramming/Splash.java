package com.example.mbprogramming;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Splash extends AppCompatActivity {
// drawable 에 있는 splash.xml 을 사용하지 않고 values 의  styles.xml 파일을 사용
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Thread.sleep(1000);  //  1초후
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startActivity(new Intent(this,MainActivity. class)); //메인 액티비티 실행
        finish();
    }
}
