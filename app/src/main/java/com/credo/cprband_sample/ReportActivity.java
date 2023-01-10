package com.credo.cprband_sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ReportActivity extends Activity {

    private TextView totaltime_tv, correct_report_tv, total_report_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        totaltime_tv = findViewById(R.id.totaltime_tv);
        correct_report_tv = findViewById(R.id.correct_report_tv);
        total_report_tv = findViewById(R.id.total_report_tv);

        int totaltime = getIntent().getIntExtra("time", 0);
        int correct = getIntent().getIntExtra("correct_count", 0);
        int total = getIntent().getIntExtra("total_count", 0);

        totaltime_tv.setText("Total time : " + totaltime + "초");
        correct_report_tv.setText("Correct count : "+correct + "회");
        total_report_tv.setText("Total count :" +total + "회");


    }

}
