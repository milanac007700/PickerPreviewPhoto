package com.example.milanac007.pickerandpreviewphoto;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by zqguo on 2016/12/7.
 */
public class PhotoAlbumAdapter extends BaseAdapter {

    private PickerAlbumActivity mContext;
    private List<PickerAlbumActivity.AlbumImgData> mImgDatas;
    private final LayoutInflater mInflater;
    private int mItemWidth;
    private FrameLayout.LayoutParams mItemParams;

    private int mMode = PickerAlbumActivity.MULTIMODE;
    private final Bitmap mPlaceholderBitmap;
    private final Bitmap cameraBitmap;


    public void setChooseMode(int mode){
        mMode = mode;
    }

    public PhotoAlbumAdapter(PickerAlbumActivity context, List<PickerAlbumActivity.AlbumImgData> imgDatas){
        super();
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mImgDatas = imgDatas;

        PickerAlbumActivity albumActivity = (PickerAlbumActivity)mContext;
        int srceenWidth = albumActivity.getSrceenWidth();
        mItemWidth = (srceenWidth - 4*4)/3; //item减4个像素
        mItemParams = new FrameLayout.LayoutParams(mItemWidth, mItemWidth);

        cameraBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.msg_camera);
        mPlaceholderBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.msg_pic_fail);
    }

    @Override
    public int getCount() {
        return mImgDatas.size();
    }

    @Override
    public PickerAlbumActivity.AlbumImgData getItem(int position) {
        if(position < 0 || position > getCount())
            return null;

        return mImgDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if(convertView ==  null){
            convertView = mInflater.inflate(R.layout.photo_item_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.photoImg = convertView.findViewById(R.id.item_image);
            viewHolder.stateImg = convertView.findViewById(R.id.selected_state);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.photoImg.setLayoutParams(mItemParams);

        PickerAlbumActivity.AlbumImgData item = getItem(position);
        if(item.id.equals("-1")){
            viewHolder.photoImg.setImageBitmap(cameraBitmap);
            viewHolder.stateImg.setVisibility(View.GONE);
        }else {
            loadBitmap(item.path, viewHolder.photoImg);
            setStateUI(viewHolder.stateImg, item);
        }

        setListener(viewHolder, item);
        return convertView;
    }

    private void setStateUI(ImageView stateView, final PickerAlbumActivity.AlbumImgData item) {
        if(mMode == PickerAlbumActivity.MULTIMODE){
            stateView.setVisibility(View.VISIBLE);

            Drawable drawable = null;
            if(mContext.isContain(item)){
                drawable = mContext.getResources().getDrawable(R.mipmap.checkbox_on);
            }else {
                drawable = mContext.getResources().getDrawable(R.mipmap.checkbox_off);
            }
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            stateView.setImageDrawable(drawable);
        }else {
            stateView.setVisibility(View.GONE);
        }
    }

    private void setListener(final ViewHolder viewHolder, final PickerAlbumActivity.AlbumImgData item){
        viewHolder.photoImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickerAlbumActivity activity = mContext;

                if(mMode == PickerAlbumActivity.MULTIMODE){
                    if(item.id.equals("-1")){
                        activity.cameraPhoto();
                        return;
                    }

                    Drawable drawable = null;
                    if(activity.isContain(item)){
                        activity.removeOne(item);
                        drawable = mContext.getResources().getDrawable(R.mipmap.checkbox_off);
                    }else {
                        if(activity.ContainSelectedSum() < activity.getMaxNum()){
                            activity.addOne(item);
                            drawable = mContext.getResources().getDrawable(R.mipmap.checkbox_on);
                        }else {
                            CommonFunction.showToast(String.format("最多只能选择%d张", activity.getMaxNum()));
                        }
                    }

                    if(drawable != null){
                        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                        viewHolder.stateImg.setImageDrawable(drawable);
                    }
                    activity.updateSendBtnUI();
                }else { //单选
                    if(!item.id.equals("-1")){
                        activity.clear();
                        activity.addOne(item);
                        activity.onClickPreviewBtn();
                    }else {
                        activity.cameraPhoto();
                    }
                }
            }
        });
    }

    static class ViewHolder{
        ImageView photoImg;
        ImageView stateImg;
    }


    static class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>{

        private WeakReference<PhotoAlbumAdapter> adapterReference;
        private WeakReference<ImageView> imgViewPreference;
        private String path;

        public BitmapWorkerTask(PhotoAlbumAdapter adapter, ImageView imageView){
            super();
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imgViewPreference = new WeakReference<ImageView>(imageView);
            adapterReference = new WeakReference<>(adapter);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            path = params[0];
            PhotoAlbumAdapter adapter = adapterReference.get();
            if(adapter != null) {
                return CacheManager.getInstance().addCacheData(path, adapter.mItemWidth, adapter.mItemWidth, false);//加入内存、磁盘缓存
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(isCancelled()){
                bitmap = null;
            }

            if(adapterReference != null && imgViewPreference != null && bitmap != null){
                final ImageView imageView = imgViewPreference.get();
                final PhotoAlbumAdapter adapter = adapterReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkTask(imageView);
                if(this == bitmapWorkerTask && imageView != null && adapter != null){
                    GlideView(adapter.mContext, bitmap, imageView);
                }
            }
        }
    }

    private static void GlideView(Context context, Bitmap bitmap, ImageView imageView){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();

        // ImageView.ScaleType.CENTER_CROP:
        //Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions (width and height) of the image will be equal to
        // or larger than the corresponding dimension of the view (minus padding).
//        new GlideBuilder(mContext).

        Glide.with(context)
                .load(bytes)
                .placeholder(R.mipmap.msg_pic_fail)
                .error(R.mipmap.msg_pic_fail)
//                .crossFade()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE) //不缓存到SD卡
                .skipMemoryCache(true) //不缓存内存
                .into(imageView);
    }

    public void loadBitmap(String path, ImageView imageView) {

        Bitmap bitmap = CacheManager.getInstance().getBitmapFormCache(path);
        if(bitmap != null){
            GlideView(mContext, bitmap, imageView);
            return;
        }

        if(cancelPotentialWork(path, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(this, imageView);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(), mPlaceholderBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(path);
        }
    }


    static class AsyncDrawable extends BitmapDrawable {
        private WeakReference<BitmapWorkerTask> bitmapWorkerTaskWeakReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask){
            super(res, bitmap);
            bitmapWorkerTaskWeakReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask(){
            return bitmapWorkerTaskWeakReference.get();
        }
    }

    private static BitmapWorkerTask getBitmapWorkTask(ImageView imageView){
        if(imageView != null){
            final Drawable drawable = imageView.getDrawable();
            if(drawable instanceof AsyncDrawable){
                final AsyncDrawable asyncDrawable = (AsyncDrawable)drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public static boolean cancelPotentialWork(String path, ImageView imageView){
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkTask(imageView);

        if(bitmapWorkerTask != null){
            final String bitmapPath = bitmapWorkerTask.path;
            // If bitmapPath is not yet set or it differs from the new data
            if(TextUtils.isEmpty(bitmapPath) || !bitmapPath.equals(path)){
                // Attempts to cancel previous task(通过调用thread的interrupt()方法，置中断位。如果当前thread处于Waitting(调用wait、sleep方法)会清除中断状态位，并抛出InterruptedException异常;
                // 如果当前thread处于Blocking(例如调用synchronized、lock.lock()), 那么依然处于Blocking；
                // 如果正在运行Running， 则依然在运行。只能主动调用Thread.currentThread().isInterrupted()来检测中断位)
                bitmapWorkerTask.cancel(true);
            }else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

}
