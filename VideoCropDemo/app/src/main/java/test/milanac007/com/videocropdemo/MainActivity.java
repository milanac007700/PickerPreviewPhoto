package test.milanac007.com.videocropdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.tdr.tdrsipim.activity.CustomVideoCaptureActivity;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View cropVideo = findViewById(R.id.cropVideo);
        cropVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CustomVideoCaptureActivity.class));
            }
        });
    }
}
