package com.milanac007.scancode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener{
    public static final String TAG = "MainActivity";
    private ImageView mQRCodeImage;
    private final String usercode = "test000 Hello!";
    private TextView myQRcodeView;
    private TextView scanResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        myQRcodeView = (TextView)findViewById(R.id.myQRcode);
        View scanQRcodeView = findViewById(R.id.scanQRcode);
        scanResult = (TextView)findViewById(R.id.scanResult);
        mQRCodeImage = (ImageView)findViewById(R.id.qrcode_image);
        myQRcodeView.setOnClickListener(this);
        scanQRcodeView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.myQRcode){
            if(mQRCodeImage.getVisibility() == View.GONE) {
                myQRcodeView.setText("隐藏我的二维码");
                mQRCodeImage.setVisibility(View.VISIBLE);
                Bitmap bitmap = QRCodeUtil.createImage(usercode);
                mQRCodeImage.setImageBitmap(bitmap);
            }else if(mQRCodeImage.getVisibility() == View.VISIBLE){
                mQRCodeImage.setVisibility(View.GONE);
                myQRcodeView.setText("显示我的二维码");
            }

        }else if(v.getId() == R.id.scanQRcode){
            Intent intent = new Intent(MainActivity.this, QRCodeScanActivity.class);
            startActivityForResult(intent, QRCodeScanActivity.QRCODE_MASK);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case QRCodeScanActivity.QRCODE_MASK:{
                if(resultCode == 200) { //success
                    final String code = data.getStringExtra("code");
                    Log.e(TAG, "QRCodeScanActivity result: " + code);
					new HandlerPost(200){ //保证QRCodeScanActivity已经销毁
						@Override
						public void doAction() {
                            scanResult.setText(code);
                            Toast.makeText(MainActivity.this, code, Toast.LENGTH_LONG).show();
						}
					};

                }
            }break;
            default:
                break;
        }
    }
}
