package com.milanac007.scancode;


import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by zqguo on 2016/11/8.
 *显示操作项列表的dialog
 */
public class OperateListDialog {
    private int mDialogHeightInDp = 310;
    private OperateDialog mOperateDialog;
    private Context mContext;
    public OperateListDialog(Context context, int operateItemCount) {
        mContext = context;
        mDialogHeightInDp = 45 + operateItemCount * 51;
        mOperateDialog = new OperateDialog(context, R.style.Loadingdialog, R.layout.operate_list_dialog);
    }
    public OperateListDialog(Context context) {
        mContext = context;
        mOperateDialog = new OperateDialog(context, R.style.Loadingdialog, R.layout.operate_list_dialog);
    }

    public void setOperateItems(ArrayList<OperateItem> operateItems) {
        mOperateDialog.setOperateItems(operateItems);
    }

    public void updateOperateItems(ArrayList<OperateItem> operateItems) {
        mOperateDialog.updateData(operateItems);
        setItemCount(operateItems.size());
    }

    public void setOnItemClickListener(int position,OperateItemClickListener listener) {
        mOperateDialog.setOnItemClickListener(position,listener);
    }

    public void dismiss() {
        mOperateDialog.dismiss();
    }

    public void show() {
        mOperateDialog.show();
    }

    public void setTitle(String title) {
        mOperateDialog.setDialogTitle(title);
    }

    private boolean showTitle = true;
    public void showTitle(boolean show){
        showTitle = show;
        mOperateDialog.mTitle.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private int mGravityType = 0;
    public void setGravityType(int gravityType){
        mGravityType = gravityType;
    }

    public void setTitle(int title) {
        mOperateDialog.setDialogTitle(title);
    }

    public void setConfirm(int title) {
        mOperateDialog.setDialogConfirm(title);
    }

    public void setConfirm(String title) {
        mOperateDialog.setDialogConfirm(title);
    }

    public void showConfirm(boolean show){
        mOperateDialog.mConfirm.setVisibility(show ? View.VISIBLE : View.GONE);
        mOperateDialog.mListView.setPadding(0, 0, 0, 0);
    }

    public void setItemCount(int count){
        if(showTitle){
            mDialogHeightInDp = 45 + count * 51;
            mOperateDialog.setBackground(R.color.transparent);
        }else {
            mDialogHeightInDp = 2 + count * 51;
            mOperateDialog.setBackground(R.color.whites);
        }

        mOperateDialog.refreshWindow();
    }

    public void setOnConfirmClickListener(OnClickListener listener){
        if (listener != null) {
            mOperateDialog.mConfirm.setOnClickListener(listener);
        }
    }

    /**
     * 设置该Dialog点击back键时是否可以dismiss
     * @param cancleable
     */
    public void setCancleable(boolean cancleable) {
        mOperateDialog.setCancelable(cancleable);
    }

    /**
     * 设置点击Dialog外部区域该Dialog是否可以dismiss
     * @param cancleable
     */
    public void setCanceledOnTouchOutside(boolean cancleable) {
        mOperateDialog.setCanceledOnTouchOutside(cancleable);
    }

    public OperateDialog getDialog() {
        return mOperateDialog;
    }

    public OperateItem getOperateItem(int position)
    {
        return mOperateDialog.getOperateItem(position);
    }

    public void setIconType(EIconType type){
        mOperateDialog.mAdapter.setIconType(type);
    }
    /**
     */
    private class OperateDialog extends Dialog {
        private TextView mTitle,mConfirm;
        private ListView mListView;
        private OperateListAdapter mAdapter;
        private int widthPixels;

        private  float scale = 0.0f; // 密度

        public  void init() {
            WindowManager windowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();// 初始化一个结构
            windowManager.getDefaultDisplay().getMetrics(metrics); // 对该结构赋值
            widthPixels = metrics.widthPixels;

            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
            scale = dm.density;
        }

        /**
         * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
         */
        public  int dip2px(float dpValue) {
            if (scale == 0) {
                init();
            }
            return (int) (dpValue * scale + 0.5f);
        }

        public OperateDialog(Context context, int theme, int layout) {
            super(context, theme);
            init();
            setContentView(layout);
            initView(context);
        }

        private void initView(Context context) {
            refreshWindow();
            mTitle = (TextView) findViewById(R.id.operate_list_title);
            mConfirm = (TextView) findViewById(R.id.operate_list_confirm);
            mListView = (ListView) findViewById(R.id.operate_listview);
            mListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            mAdapter = new OperateListAdapter(context);
            mListView.setAdapter(mAdapter);
        }

        public void setBackground(int bg){
            mListView.setBackgroundColor(mContext.getResources().getColor(bg));
        }

        public void refreshWindow(){
            Window window = getWindow();
            WindowManager.LayoutParams params = window.getAttributes();

            if(mGravityType == 0){ //默认居中显示
                params.gravity = Gravity.CENTER;
                params.width = widthPixels * 8/10;
            }else if(mGravityType == 1){ //底部居中显示
                params.gravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;
                params.width = widthPixels;
            }

            // dialog 的最大高度为屏幕高度的 6/10；
            int maxHeightInPx = widthPixels * 6/10;
            int dialogHeightInPx = dip2px(mDialogHeightInDp);
            params.height = Math.min(maxHeightInPx, dialogHeightInPx);
            window.setAttributes(params);
        }
        /**
         * 设置操作的item
         *
         * @param operateItems :
         */
        private void setOperateItems(ArrayList<OperateItem> operateItems) {
            mAdapter.setData(operateItems);
        }

        private void updateData(ArrayList<OperateItem> operateItems) {
            mAdapter.updateData(operateItems);
        }

        private void setDialogTitle(String title) {
            mTitle.setText(title);
        }

        private void setDialogTitle(int title) {
            mTitle.setText(title);
        }

        private void setDialogConfirm(int title) {
            mConfirm.setText(title);
        }

        private void setDialogConfirm(String title) {
            mConfirm.setText(title);
        }

        public void setOnItemClickListener(int position, OperateItemClickListener listener) {
            mAdapter.setOnItemClickListener(position, listener);
        }

        public OperateItem getOperateItem(int position)
        {
            return mAdapter.getItem(position);
        }

    }

    /**
     * 可操作的条目类
     */
    public class OperateItem {

        private String mOperateKey;
        /**item的名称的字符串*/
        private String mItemName;
        private int mOperateImageResId = -1;
        private OperateItemClickListener itemClickLister;

        public OperateItem() {

        }

        public String getmOperateKey() {
            return mOperateKey;
        }

        public void setmOperateKey(String mOperateKey) {
            this.mOperateKey = mOperateKey;
        }

        public String getmItemNameStr() {
            return mItemName;
        }

        public void setmItemNameStr(String mItemNameStr) {
            this.mItemName = mItemNameStr;
        }

        public int getmOperateImageResId() {
            return mOperateImageResId;
        }

        public void setmOperateImageResId(int mOperateImageResId) {
            this.mOperateImageResId = mOperateImageResId;
        }


        public void setItemClickLister(OperateItemClickListener itemClickLister) {
            this.itemClickLister = itemClickLister;
        }

    }

    /**
     * dialog中listview 的适配器
     */
    private class OperateListAdapter extends BaseAdapter {
        List<OperateItem> mOperateItems = new ArrayList<>();
        private LayoutInflater mInflater;
        private EIconType mIconType = EIconType.DEFAULT;


        public OperateListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public void setData(ArrayList<OperateItem> operateItem) {
            if (operateItem != null) {
                mOperateItems.addAll(operateItem);
            }
            notifyDataSetChanged();
        }

        public void updateData(ArrayList<OperateItem> operateItems) {
            if (operateItems != null) {
                mOperateItems.clear();
                mOperateItems.addAll(operateItems);
            }
            notifyDataSetChanged();
        }

        private void setIconType(EIconType type){
            mIconType = type;
        }

        @Override
        public int getCount() {
            return mOperateItems.size();
        }

        @Override
        public OperateItem getItem(int position) {
            if (position > mOperateItems.size() - 1) {
                return null;
            }

            return mOperateItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.operate_list_item, parent, false);
                holder = new ViewHolder(convertView);
                setViewHolder(convertView, holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            OperateItem item = getItem(position);
            switch (mIconType) {
                case DEFAULT:
                case LEFT:
                    holder.operateIconRight.setVisibility(View.GONE);
                    if (item.getmOperateImageResId()  == -1) {
                        holder.operateIcon.setVisibility(View.GONE);
                    } else {
                        holder.operateIcon.setVisibility(View.VISIBLE);
                        holder.operateIcon.setImageResource(item.mOperateImageResId);
                    }
                    break;
                case RIGHT:
                    holder.operateIcon.setVisibility(View.GONE);
                    if (item.getmOperateImageResId()  == -1) {
                        holder.operateIconRight.setVisibility(View.GONE);
                    } else {
                        holder.operateIconRight.setVisibility(View.VISIBLE);
                        holder.operateIconRight.setImageResource(item.mOperateImageResId);
                    }
                    break;

                default:
                    break;
            }

            holder.operateName.setText(item.mItemName);
            setOnItemClickListener(convertView, position);
            if (position != getCount()-1) {
                holder.divider.setVisibility(View.VISIBLE);
            }else {
                holder.divider.setVisibility(View.GONE);
            }
            return convertView;
        }

        private void setOnItemClickListener(View convertView, final int position) {
            convertView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    OperateItem item = mOperateItems.get(position);
                    if (item != null && item.itemClickLister  !=  null) {
                        item.itemClickLister.clickItem(position);
                    }
                    notifyDataSetChanged();
                }
            });
        }

        public void setOnItemClickListener(int position, OperateItemClickListener listener) {
            OperateItem item = mOperateItems.get(position);
            if (item != null) {
                item.setItemClickLister(listener);
            }
        }

        private void setViewHolder(View convertView, ViewHolder holder) {
            holder.operateIcon = (ImageView) convertView.findViewById(R.id.operate_list_item_img);
            holder.operateIconRight = (ImageView) convertView.findViewById(R.id.operate_list_item_img_right);
            holder.operateName = (TextView) convertView.findViewById(R.id.operate_list_item_name);
            holder.divider = convertView.findViewById(R.id.operate_list_item_divider);

        }

        private class ViewHolder {
            private ImageView operateIcon;
            private ImageView operateIconRight;
            private TextView operateName;
            private View divider;

            public ViewHolder(View convertView) {
                super();
                convertView.setTag(this);
            }
        }
    }

    public interface OperateItemClickListener {
        void clickItem(int position);
    }

    public enum EIconType{
        DEFAULT,RIGHT,LEFT
    }

}