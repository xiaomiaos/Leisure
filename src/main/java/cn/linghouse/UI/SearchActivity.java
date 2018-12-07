package cn.linghouse.UI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.gyf.barlibrary.ImmersionBar;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;
import com.zyao89.view.zloading.ZLoadingDialog;
import com.zyao89.view.zloading.Z_TYPE;

import org.angmarch.views.NiceSpinner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.linghouse.Adapter.Search_Adapter;
import cn.linghouse.App.ActivityController;
import cn.linghouse.Entity.Search_Entity;
import cn.linghouse.Util.ToastUtil;
import cn.linghouse.leisure.R;
import okhttp3.Call;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener{
    private ListView lvsearch;
    private SmartRefreshLayout refreshLayout;
    private TextView tvsearch;
    private EditText etsearch;
    private NiceSpinner spinner;
    private ImageView searchback;
    private Search_Adapter adapter;
    private TextView searchfaild;
    private Search_Entity entity;
    private TextView tvclassify;
    private ZLoadingDialog dialog;
    private Button restart,sure;
    private SlidingMenu slidingMenu;
    private RadioGroup rgutils;
    private EditText slmin,slmax;
    private LinearLayout linbutton;
    private List<Search_Entity> search_entity;
    private String search_url = "http://192.168.137.1:8080/leisure/commodities/search";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ActivityController.addActivity(this);
        //沉浸式状态栏
        ImmersionBar.with(SearchActivity.this).init();
        //初始化控件
        initview();
        //初始化侧滑菜单
        intislidemenu();
        //检查是否存在虚拟按键
        checkDeviceHasNavigationBar(SearchActivity.this);
        search_entity = new ArrayList<>();
        adapter = new Search_Adapter(search_entity,SearchActivity.this);
        lvsearch.setAdapter(adapter);
    }

    //初始化第三方对话框
    private void initdialog(){
        dialog = new ZLoadingDialog(SearchActivity.this);
        dialog.setLoadingBuilder(Z_TYPE.DOUBLE_CIRCLE)//对话框样式
                .setLoadingColor(Color.BLACK)//颜色
                .setHintText("努力搜索中.....")//提示文字
                .setHintTextSize(16)//提示文字大小
                .setHintTextColor(Color.GRAY)//提示文字颜色
                .setDurationTime(0.5)//动画时间百分比 0.5倍
                .setDialogBackgroundColor(Color.parseColor("#DEDEDE"))//设置背景色，默认白色
                .show();
    }

    private void initview() {
        refreshLayout = findViewById(R.id.refresh);
        searchfaild = findViewById(R.id.tv_search_faild);
        searchback = findViewById(R.id.iv_search_back);
        etsearch = findViewById(R.id.et_search_goods);
        lvsearch = findViewById(R.id.lv_search);
        tvsearch = findViewById(R.id.tv_search);
        tvclassify = findViewById(R.id.tv_classify);
        spinner = findViewById(R.id.ns_sorting);
        //设置禁止下拉刷新
        refreshLayout.setEnableRefresh(false);
        searchfaild.setVisibility(View.GONE);//默认设置没有搜索出结果的视图为不可见
        tvclassify.setOnClickListener(this);
        searchback.setOnClickListener(this);
        tvsearch.setOnClickListener(this);
        final List<String> data = new ArrayList<>();
        data.add("综合排序");
        data.add("价格升序");
        data.add("价格降序");
        spinner.attachDataSource(data);//设置下拉数据源
        //软键盘点击事件监听
        etsearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId==EditorInfo.IME_ACTION_SEARCH){
                    if (TextUtils.isEmpty(etsearch.getText().toString())){
                        etsearch.setError("输入宝贝名称试试看");
                    }else{
                        search_entity.clear();
                        initdialog();
                        searchGoogds_Default(etsearch.getText().toString(),"1","20");
                        hideSoftKeyboard(SearchActivity.this);
                    }
                    return true;
                }
                return false;
            }
        });

        /**
         * 下拉框的点击事件
         */
        spinner.addOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        ToastUtil.ShowShort("综合排序");
                        break;
                    case 1:
                        search_entity.clear();
                        initdialog();
                        priceWay(etsearch.getText().toString(),"desc");
                        ToastUtil.ShowShort("价格升序");
                        break;
                    case 2:
                        search_entity.clear();
                        initdialog();
                        priceWay(etsearch.getText().toString(),"asc");
                        ToastUtil.ShowShort("价格降序");
                        break;
                }
            }
        });

        lvsearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String price = search_entity.get(position).getPice();
                String title = search_entity.get(position).getName();
                Intent intent = new Intent();
                intent.putExtra("title",title);
                intent.putExtra("price",price);
                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                intent.setClass(SearchActivity.this,GoodDetailsActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * 初始化侧滑菜单
     */
    private void intislidemenu(){
        slidingMenu = new SlidingMenu(this);
        slidingMenu.attachToActivity(this,SlidingMenu.SLIDING_WINDOW);
        slidingMenu.setMode(SlidingMenu.RIGHT);//侧滑菜单滑出方向
        slidingMenu.setBehindWidth(800);//侧滑菜单宽度
        slidingMenu.setFadeDegree(0.35f);
        slidingMenu.setMenu(R.layout.right_menu);//侧滑菜单布局视图
        //初始化侧滑菜单中的控件
        restart = slidingMenu.findViewById(R.id.btn_restart);
        sure = slidingMenu.findViewById(R.id.btn_sure);
        linbutton = slidingMenu.findViewById(R.id.lin_button);
        rgutils = slidingMenu.findViewById(R.id.rg_utils);
        slmin = slidingMenu.findViewById(R.id.sl_et_price_min);
        slmax = slidingMenu.findViewById(R.id.sl_et_price_max);

        restart.setOnClickListener(this);
        sure.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_classify:
                if (!slidingMenu.isSecondaryMenuShowing()){
                    slidingMenu.showSecondaryMenu();
                }else{
                    //隐藏侧滑菜单
                    slidingMenu.toggle();
                }
                break;
            case R.id.tv_search:
                if (TextUtils.isEmpty(etsearch.getText().toString())){
                    etsearch.setError("输入宝贝名称试试看");
                }else{
                    search_entity.clear();
                    searchGoogds_Default(etsearch.getText().toString(),"0","20");
                    initdialog();
                }
                break;
            case R.id.iv_search_back:
                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                SearchActivity.this.finish();
                break;
            //侧滑菜单中的重置按钮,清除所有已经设置了的数据
            case R.id.btn_restart:
                rgutils.clearCheck();
                slmin.setText("");
                slmax.setText("");
                slidingMenu.toggle();
                break;
                //侧滑菜单中的确定按钮,发起网络请求，将筛选条件发送到后端
            case R.id.btn_sure:

                break;
                default:
                    break;
        }
    }

    /**
     * 隐藏软键盘(只适用于Activity，不适用于Fragment)
     */
    public static void hideSoftKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 判断是否存在NavigationBar
     * @param context：上下文环境
     * @return：返回是否存在(true/false)
     */
    public boolean checkDeviceHasNavigationBar(Context context) {
        boolean hasNavigationBar = false;
        Resources rs = context.getResources();
        int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id);
        }
        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                //不存在底部导航栏
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                //存在底部导航栏
                hasNavigationBar = true;
                //手动设置控件的margin
                LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) linbutton.getLayoutParams();
                layout.setMargins(0,0,0,getNavigationBarHeight(this)+10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasNavigationBar;
    }

    /**
     * 测量底部导航栏的高度
     * @param mActivity:上下文环境
     * @return：返回测量出的底部导航栏高度
     */
    private int getNavigationBarHeight(Activity mActivity) {
        Resources resources = mActivity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height","dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }

    /**
     * 搜索商品
     * @param name:商品名称
     * @param page：商品页码
     * @param size：一页显示的商品数量
     */
    private void searchGoogds_Default(String name,String page,String size){
        OkHttpUtils.post()
                .url(search_url)
                .addParams("commodityName",name)
                .addParams("searchMethod","default")
                .addParams("page",page)
                .addParams("size",size)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {

                    }

                    @Override
                    public void onResponse(String response, int id) {
                        dialog.dismiss();
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONObject data = jsonObject.getJSONObject("data");
                            JSONArray commd = data.getJSONArray("commodities");
                            for (int i =0;i<commd.length();i++){
                                JSONObject object = commd.getJSONObject(i);
                                String name = object.getString("commodityName");
                                String price = object.getString("price");
                                String score = object.getString("score");
                                entity = new Search_Entity();
                                entity.setName(name);
                                entity.setPice(price);
                                entity.setScore(score);
                                search_entity.add(entity);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * 通过价格升序商品
     * @param name：商品名称
     */
    private void priceWay(String name,String priceway){
        OkHttpUtils.post()
                .url(search_url)
                .addParams("commodityName",name)
                .addParams("searchMethod","price")
                .addParams("order",priceway)
                .addParams("page","0")
                .addParams("size","20")
                .build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {

            }

            @Override
            public void onResponse(String response, int id) {
                dialog.dismiss();
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject data = jsonObject.getJSONObject("data");
                    JSONArray commd = data.getJSONArray("commodities");
                    for (int i =0;i<commd.length();i++){
                        JSONObject object = commd.getJSONObject(i);
                        String name = object.getString("commodityName");
                        String price = object.getString("price");
                        String score = object.getString("score");
                        entity = new Search_Entity();
                        entity.setName(name);
                        entity.setPice(price);
                        entity.setScore(score);
                        search_entity.add(entity);
                    }
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImmersionBar.with(this).destroy();
        ActivityController.removeActivity(this);
    }
}