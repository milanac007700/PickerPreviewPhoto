package com.example.milanac007.pickerandpreviewphoto;

import android.app.Activity;
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

    private Activity mContext;
    private List<PickerAlbumActivity.AlbumImgData> mImgDatas;
    private final LayoutInflater mInflater;
    private int mItemWidth;
    private FrameLayout.LayoutParams mItemParams;
    private final Bitmap mPlaceholderBitmap;
    private int mMode = PickerAlbumActivity.MULTIMODE;


    public void setChooseMode(int mode){
        mMode = mode;
    }

    public PhotoAlbumAdapter(Activity context, List<PickerAlbumActivity.AlbumImgData> imgDatas){
        super();
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mImgDatas = imgDatas;

        if(mContext instanceof PickerAlbumActivity){
            PickerAlbumActivity albumActivity = (PickerAlbumActivity)mContext;
            int srceenWidth = albumActivity.getmSrceenWidth();
            mItemWidth = (srceenWidth - 4*4)/3; //item减4个像素
        }

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
            viewHolder.photoImg = (ImageView) convertView.findViewById(R.id.item_image);
            viewHolder.stateImg = (ImageView) convertView.findViewById(R.id.selected_state);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        if(mItemParams == null) {
            mItemParams = new FrameLayout.LayoutParams(mItemWidth, mItemWidth);
        }
        viewHolder.photoImg.setLayoutParams(mItemParams);

        PickerAlbumActivity.AlbumImgData item = getItem(position);
        if(item.id.equals("-1")){
            Bitmap cameraBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.msg_camera);
            viewHolder.photoImg.setImageBitmap(cameraBitmap);
            viewHolder.stateImg.setVisibility(View.GONE);
        }else {
            loadBitmap(item.path, viewHolder.photoImg);
            if(mMode == PickerAlbumActivity.MULTIMODE){
                viewHolder.stateImg.setVisibility(View.VISIBLE);

                Drawable drawable = null;
                if(mContext instanceof  PickerAlbumActivity){
                    PickerAlbumActivity activity = (PickerAlbumActivity)mContext;

                    if(activity.isContain(item)){
                        drawable = mContext.getResources().getDrawable(R.mipmap.checkbox_on);
                    }else {
                        drawable = mContext.getResources().getDrawable(R.mipmap.checkbox_off);
                    }
                    drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                    viewHolder.stateImg.setImageDrawable(drawable);
                }
            }else {
                viewHolder.stateImg.setVisibility(View.GONE);
            }

        }

        setListener(viewHolder, item);
        return convertView;
    }

    private void setListener(final ViewHolder viewHolder, final PickerAlbumActivity.AlbumImgData item){
        viewHolder.photoImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mContext instanceof  PickerAlbumActivity){
                    PickerAlbumActivity activity = (PickerAlbumActivity)mContext;

                    if(mMode == PickerAlbumActivity.MULTIMODE){
                        Drawable drawable = null;
                        if(activity.isContain(item)){
                            activity.removeOne(item);
                            drawable = mContext.getResources().getDrawable(R.mipmap.checkbox_off);
                        }else {
                            if(!item.id.equals("-1")){
                                if(activity.ContainSelectedSum() < activity.getMaxNum()){
                                    drawable = mContext.getResources().getDrawable(R.mipmap.checkbox_on);
                                    activity.addOne(item);
                                }else {
                                    CommonFunction.showToast(String.format("最多只能选择%d张", activity.getMaxNum()));
                                }

                            }else {
                                //TODO 拍照
                                activity.cameraPhoto();
                                return;
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
                            //TODO 拍照
                            activity.cameraPhoto();
                        }
                    }

                }

            }
        });
    }

    static class ViewHolder{
        ImageView photoImg;
        ImageView stateImg;
    }


    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>{

        private  WeakReference<ImageView> imgViewPrference;
        private String path;

        public BitmapWorkerTask(ImageView imageView){
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imgViewPrference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            path = params[0];
            Bitmap bitmap = CacheManager.getInstance().addCacheData(path, mItemWidth, mItemWidth);//加入内存、磁盘缓存
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(isCancelled()){
                bitmap = null;
            }

            if(imgViewPrference != null && bitmap != null){
                final ImageView imageView = imgViewPrference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkTask(imageView);
                if(this == bitmapWorkerTask && imageView != null){
                    GlideView(bitmap, imageView);
                }
            }
        }
    }

    private void GlideView(Bitmap bitmap, ImageView imageView){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes=baos.toByteArray();

        // ImageView.ScaleType.CENTER_CROP:
        //Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions (width and height) of the image will be equal to
        // or larger than the corresponding dimension of the view (minus padding).
//        new GlideBuilder(mContext).

        Glide.with(mContext)
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

        Bitmap bitmap =  CacheManager.getInstance().getBitmapFormCache(path);
        if(bitmap != null){
            GlideView(bitmap, imageView);
            return;
        }

        if(cancelPotentialWork(path, imageView)){
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(), mPlaceholderBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(path);
        }
    }


    static class AsyncDrawable extends BitmapDrawable{
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
                // Cancel previous task
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
