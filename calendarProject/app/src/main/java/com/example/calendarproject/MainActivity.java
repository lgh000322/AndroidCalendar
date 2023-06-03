package com.example.calendarproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnItemListener {

    TextView monthYearText,dialogTextView1;

    int year = 0, month = 0, lastDate = 0;
    final Bundle bundle = new Bundle();
    String haksaSchedule = "";
    String url = "https://www.kongju.ac.kr/kongju/12476/subview.do#this";
    ImageButton prebtn, nextbtn;

    RecyclerView recyclerView;

    Button getScheduleBtn;
    EditText dialogEdt;
    View dialogView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("학사일정이 있는 캘린더 만들기");
        monthYearText = (TextView) findViewById(R.id.date);
        prebtn = (ImageButton) findViewById(R.id.prebtn);
        nextbtn = (ImageButton) findViewById(R.id.nextbtn);
        getScheduleBtn = (Button) findViewById(R.id.getScheduleBtn);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        lastDate = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        setMonthView(calendar, year, month, lastDate);

        //크롤링 구현하기
        Handler handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle bundle = msg.getData();
                ArrayList<String> contentList = bundle.getStringArrayList("message");
                String fileName = month + "달의 학사일정";
                StringBuilder sb = new StringBuilder();

                for (String content : contentList) {
                    sb.append(content).append("\n");
                }

                try {
                    FileOutputStream fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
                    fileOutputStream.write(sb.toString().getBytes());
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();

                // Request 객체 생성
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                // HTTP 요청 보내기
                try (Response response = client.newCall(request).execute()) {
                    // HTTP 응답 받기
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();

                        // Jsoup을 사용하여 HTML 파싱하기
                        Document doc = Jsoup.parse(responseBody);
                        Elements elements = doc.select(".sche-comt table tr");  // tr 요소 선택

                        ArrayList<String> contentList = new ArrayList<>();

                        for (Element element : elements) {
                            Elements thElements = element.select("th");  // th 요소 선택
                            Elements tdElements = element.select("td");  // td 요소 선택

                            StringBuilder contentBuilder = new StringBuilder();

                            for (Element thElement : thElements) {
                                String thText = thElement.text();
                                contentBuilder.append(thText).append(" ");  // th 텍스트 추가
                            }

                            for (Element tdElement : tdElements) {
                                String tdText = tdElement.text();
                                contentBuilder.append(tdText).append(" ");  // td 텍스트 추가
                            }

                            contentList.add(contentBuilder.toString().trim());
                        }

                        // 메시지 핸들러를 사용하여 UI 업데이트하기
                        bundle.putStringArrayList("message", contentList);
                        Message message = handler.obtainMessage();
                        message.setData(bundle);
                        handler.sendMessage(message);
                    } else {
                        Log.e("OkHttp", "HTTP Request Error : " + response.code());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();


        prebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                if (month == 1) {
                    month = 12;
                    year -= 1;
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month - 1);
                    lastDate = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                    setMonthView(calendar, year, month, lastDate);
                } else {
                    month -= 1;
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month - 1);
                    lastDate = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                    setMonthView(calendar, year, month, lastDate);
                }
            }
        });

        nextbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (month == 12) {
                    month = 1;
                    year += 1;
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month - 1);
                    lastDate = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                    setMonthView(calendar, year, month, lastDate);
                } else {
                    month += 1;
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month - 1);
                    lastDate = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                    setMonthView(calendar, year, month, lastDate);
                }
            }
        });
        getScheduleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogView = (View) View.inflate(MainActivity.this, R.layout.dialog2, null);
                AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                dlg.setView(dialogView);
                dialogTextView1 = (TextView) dialogView.findViewById(R.id.dialogTextView1);
                dialogTextView1.setText(readRecord(month + "달의 학사일정"));
                Log.d("학사일정버튼의 haskaschedule:", haksaSchedule);
                dlg.setTitle("이번달 학사일정입니다.");
                dlg.setNegativeButton("닫기", null);
                dlg.show();
            }
        });
    }

    private ArrayList<String> daysInMonthArray(Calendar calendar, int year, int month, int lastDate) {
        ArrayList<String> dayList = new ArrayList<>();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2;


        for (int i = 0; i < 42; i++) {
            if (i <= firstDayOfWeek || i > lastDate + firstDayOfWeek) {
                dayList.add("");
            } else {
                dayList.add(String.valueOf(i - firstDayOfWeek));
            }
        }
        return dayList;

    }

    private void setMonthView(Calendar calendar, int selectedYear, int selectedMonth, int selectedLastDate) {
        monthYearText.setText(selectedYear + "년 " + selectedMonth + "월");

        ArrayList<String> dayList = daysInMonthArray(calendar, selectedYear, selectedMonth, selectedLastDate);

        CalendarAdapter adapter = new CalendarAdapter(dayList, MainActivity.this);

        RecyclerView.LayoutManager manager = new GridLayoutManager(getApplicationContext(), 7);

        recyclerView.setLayoutManager(manager);

        recyclerView.setAdapter(adapter);
    }

    private String readRecord(String fileName) {
        String planStr = "";
        FileInputStream fileInputStream;
        try{
            fileInputStream = openFileInput(fileName);
            byte[] txt = new byte[500];
            fileInputStream.read(txt);
            fileInputStream.close();
            planStr = (new String(txt)).trim();
        }catch(IOException e){
            e.printStackTrace();
        }
        return planStr;
    }


    @Override
    public void OnItemClickEvent(String dayText) {
        dialogView = (View) View.inflate(MainActivity.this, R.layout.dialog1, null);
        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
        dlg.setTitle(year + "년 " + month + "월 " + dayText + "일의 " + "일정입력");
        dlg.setView(dialogView);
        dialogEdt = (EditText) dialogView.findViewById(R.id.dialogEdittext);
        String fileName = year + "년" + month + "월" + dayText + "일";
        String planStr = readRecord(fileName);
        dialogEdt.setText(planStr);

        dlg.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    String str = dialogEdt.getText().toString();
                    FileOutputStream fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
                    fileOutputStream.write(str.getBytes());
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        dlg.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "취소되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });



        dlg.show();

    }
}