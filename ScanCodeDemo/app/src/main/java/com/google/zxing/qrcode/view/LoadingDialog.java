package com.google.zxing.qrcode.view;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.milanac007.scancode.R;

public class LoadingDialog {
	private LayoutInflater factory;
	Dialog loadingDialog;
	int dialogWidth;
	String showText;
	public boolean cancelable = false;
	Context context;
	
	
	public LoadingDialog(Context context,int dialogWidth, String showText) {
		super();
		this.context = context;
		this.dialogWidth = dialogWidth;
		this.showText = showText;
		show();
	}
	
	
	public void show(){
		try {
			factory = LayoutInflater.from(context);
			View loadingView = factory.inflate(R.layout.dialog_loding, null);
			TextView dialog_info = (TextView) loadingView
					.findViewById(R.id.dialog_info);
			dialog_info.setText(showText);
			loadingDialog = new Dialog(context, R.style.loading_dialog);
			loadingDialog.setCancelable(cancelable);
			loadingDialog.setContentView(loadingView, new LinearLayout.LayoutParams(
					(int)(dialogWidth*0.7),
					LinearLayout.LayoutParams.FILL_PARENT));
			loadingDialog.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public Dialog getLoadingDialog() {
		return loadingDialog;
	}


	public void setLoadingDialog(Dialog loadingDialog) {
		this.loadingDialog = loadingDialog;
	}


	public int getDialogWidth() {
		return dialogWidth;
	}


	public void setDialogWidth(int dialogWidth) {
		this.dialogWidth = dialogWidth;
	}
	
	
	
}
