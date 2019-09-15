package com.example.mbprogramming;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Button btTranslate;
    Button btReadText;
    EditText etInput;
    TextView tvResult;
    RadioButton radioButton_ko;
    RadioButton radioButton_en;
    String language1;
    String language2;

    //텍스트를 읽어주는 기능
    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        etInput =  findViewById(R.id.tv_input);
        tvResult =  findViewById(R.id.tv_result);
        btTranslate =  findViewById(R.id.btn_translate);
        btReadText = findViewById(R.id.btn_read);

        radioButton_ko = findViewById(R.id.radio_ko);
        radioButton_en = findViewById(R.id.radio_en);

        //번역 버튼 클릭
        btTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //번역할 내용이 있는지 체크
                if (etInput.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "번역할 내용을 입력하세요.", Toast.LENGTH_SHORT).show();
                    etInput.requestFocus();
                    return;
                }

                //사용자 번역 옵션
                if(radioButton_ko.isChecked()){
                    language1 = "ko";
                    language2 = "en";
                }
                if(radioButton_en.isChecked()){
                    language1 = "en";
                    language2 = "ko";
                }

                //번역할 내용 스트링으로 변환 후
                String params = etInput.getText().toString();

                //네이버 측으로 전송
                NaverTranslateTask asyncTask = new NaverTranslateTask();
                asyncTask.execute(params,language1,language2);

            }
        });

        //TTS를 생성하고 초기화
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                //에러가 없다면
                if(status != TextToSpeech.ERROR){
                    //언어는 한국어
                    tts.setLanguage(Locale.KOREA);
                }
            }
        });

        btReadText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //읽을 텍스트 박스가 비어있다면
                if (tvResult.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "읽을 내용을 입력하세요.", Toast.LENGTH_SHORT).show();
                    etInput.requestFocus();
                    return;
                }

                //읽을 텍스트박스를 읽는다
                String text = tvResult.getText().toString();
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //음성을 모두 읽고나서 tts 객체가 있다면 실행을 중지하고 메모리에서 제거
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }
    }


    //ASYNCTASK
    public class NaverTranslateTask extends AsyncTask<String, Void, String> {

        //네이버 인증 값
        String clientId = "";
        String clientSecret = "";

        //AsyncTask 메인처리
        @Override
        protected String doInBackground(String... params) {

            String sourceText = params[0];
            String sourceLang = params[1];
            String targetLang = params[2];

            try {
                //입력받은 값을 UTF-8 로 인코딩
                String text = URLEncoder.encode(sourceText, "UTF-8");
                //네이버 주소값
                String apiURL = "https://openapi.naver.com/v1/language/translate";
                //URL 담아주고
                URL url = new URL(apiURL);
                //커넥션 생성
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                //POST 방식으로 ID,SECRET 값 추가해서
                con.setRequestMethod("POST");
                con.setRequestProperty("X-Naver-Client-Id", clientId);
                con.setRequestProperty("X-Naver-Client-Secret", clientSecret);

                //번역 옵션
                String postParams = "source=" + sourceLang + "&target=" + targetLang + "&text=" + text;
                //OutputStream 허용
                con.setDoOutput(true);

                //보낼 값과 정보 담아서 전송
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postParams);
                wr.flush();
                wr.close();

                //응답 값 체크
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if (responseCode == 200) {
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }

                //반환 값 전부 담고 리턴
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();

                return response.toString();

            } catch (Exception e) {

                Log.d("error", e.getMessage());
                return null;
            }
        }

        //번역된 결과를 받아서 처리
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            //Json 으로 오기 때문에 파싱
            Gson gson = new GsonBuilder().create();
            JsonParser parser = new JsonParser();
            JsonElement rootObj = parser.parse(result)
                    //값 안에서 message 부분의 result 값을 꺼내 오겠다
                    .getAsJsonObject().get("message")
                    .getAsJsonObject().get("result");

            //꺼내온 값을 파싱
            TranslatedItem items = gson.fromJson(rootObj.toString(), TranslatedItem.class);

            //파싱한 값을 텍스트뷰에 세팅
            tvResult.setText(items.getTranslatedText());
        }

        //파싱
        private class TranslatedItem {
            String translatedText;

            public String getTranslatedText() {
                return translatedText;
            }
        }
    }
}