package com.wangdaye.mysplash.main.view.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.wangdaye.mysplash.Mysplash;
import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash._common.data.entity.unsplash.Photo;
import com.wangdaye.mysplash._common.i.model.DownloadModel;
import com.wangdaye.mysplash._common.i.presenter.DownloadPresenter;
import com.wangdaye.mysplash._common.ui._basic.MysplashFragment;
import com.wangdaye.mysplash._common.ui.adapter.PhotoAdapter;
import com.wangdaye.mysplash._common.ui.widget.CircleImageView;
import com.wangdaye.mysplash._common.ui.widget.SwipeBackCoordinatorLayout;
import com.wangdaye.mysplash._common.utils.DisplayUtils;
import com.wangdaye.mysplash._common.utils.NotificationUtils;
import com.wangdaye.mysplash._common.utils.helper.DownloadHelper;
import com.wangdaye.mysplash._common.utils.manager.AuthManager;
import com.wangdaye.mysplash._common.i.model.DrawerModel;
import com.wangdaye.mysplash._common.i.presenter.DrawerPresenter;
import com.wangdaye.mysplash._common.i.presenter.FragmentManagePresenter;
import com.wangdaye.mysplash._common.i.presenter.MeManagePresenter;
import com.wangdaye.mysplash._common.i.presenter.MessageManagePresenter;
import com.wangdaye.mysplash._common.i.view.DrawerView;
import com.wangdaye.mysplash._common.i.view.MeManageView;
import com.wangdaye.mysplash._common.ui.activity.IntroduceActivity;
import com.wangdaye.mysplash._common.utils.BackToTopUtils;
import com.wangdaye.mysplash._common.utils.manager.ShortcutsManager;
import com.wangdaye.mysplash._common.utils.manager.ThreadManager;
import com.wangdaye.mysplash._common.utils.widget.runnable.PriorityRunnable;
import com.wangdaye.mysplash.main.model.activity.DownloadObject;
import com.wangdaye.mysplash.main.model.activity.DrawerObject;
import com.wangdaye.mysplash.main.model.activity.FragmentManageObject;
import com.wangdaye.mysplash._common.i.model.FragmentManageModel;
import com.wangdaye.mysplash._common.ui._basic.MysplashActivity;
import com.wangdaye.mysplash._common.i.view.MessageManageView;
import com.wangdaye.mysplash.main.presenter.activity.DownloadImplementor;
import com.wangdaye.mysplash.main.presenter.activity.DrawerImplementor;
import com.wangdaye.mysplash.main.presenter.activity.FragmentManageImplementor;
import com.wangdaye.mysplash.main.presenter.activity.MeManageImplementor;
import com.wangdaye.mysplash.main.presenter.activity.MessageManageImplementor;
import com.wangdaye.mysplash._common.utils.widget.SafeHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main activity.
 * */

public class MainActivity extends MysplashActivity
        implements MessageManageView, MeManageView, DrawerView,
        View.OnClickListener, NavigationView.OnNavigationItemSelectedListener,
        PhotoAdapter.OnDownloadPhotoListener, AuthManager.OnAuthDataChangedListener,
        SafeHandler.HandlerContainer {
    // model.
    private FragmentManageModel fragmentManageModel;
    private DrawerModel drawerModel;
    private DownloadModel downloadModel;

    // view
    private DrawerLayout drawer;
    private NavigationView nav;
    private ImageView appIcon;
    private CircleImageView navAvatar;
    private TextView navTitle;
    private TextView navSubtitle;
    private ImageButton navButton;
    private SafeHandler<MainActivity> handler;

    // presenter.
    private FragmentManagePresenter fragmentManagePresenter;
    private MessageManagePresenter messageManagePresenter;
    private MeManagePresenter meManagePresenter;
    private DrawerPresenter drawerPresenter;
    private DownloadPresenter downloadPresenter;

    // data.
    private final String KEY_MAIN_ACTIVITY_FRAGMENT_ID_LIST = "main_activity_fragment_id_list";
    private final String KEY_MAIN_ACTIVITY_SELECTED_ID = "main_activity_selected_id";

    /** <br> life cycle. */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initModel(savedInstanceState);
        initPresenter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isStarted()) {
            setStarted();
            initView();
            buildFragmentStack();
            ThreadManager.getInstance().execute(runnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AuthManager.getInstance().removeOnWriteDataListener(this);
        AuthManager.getInstance().cancelRequest();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(
                KEY_MAIN_ACTIVITY_FRAGMENT_ID_LIST,
                (ArrayList<Integer>) fragmentManagePresenter.getIdList());
        outState.putInt(
                KEY_MAIN_ACTIVITY_SELECTED_ID,
                drawerPresenter.getCheckedItemId());
        for (int i = 0; i < fragmentManagePresenter.getFragmentCount(); i ++) {
            fragmentManagePresenter.getFragmentList().get(i).writeBundle(outState);
        }
    }

    @Override
    public void handleBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_main_drawerLayout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            int fragmentCounts = fragmentManagePresenter.getFragmentList().size();
            MysplashFragment f = fragmentManagePresenter.getFragmentList().get(fragmentCounts - 1);
            if (f != null
                    && f.needPagerBackToTop() && BackToTopUtils.isSetBackToTop(true)) {
                f.backToTop();
            } else if (fragmentCounts > 1) {
                fragmentManagePresenter.popFragment(this);
            } else {
                finishActivity(SwipeBackCoordinatorLayout.DOWN_DIR);
            }
        }
    }

    @Override
    protected void setTheme() {
        if (Mysplash.getInstance().isLightTheme()) {
            setTheme(R.style.MysplashTheme_light);
        } else {
            setTheme(R.style.MysplashTheme_dark);
        }
    }

    @Override
    protected void backToTop() {
        // do nothing.
    }

    @Override
    protected boolean needSetStatusBarTextDark() {
        return true;
    }

    @Override
    public void finishActivity(int dir) {
        finish();
    }

    @Override
    public View getSnackbarContainer() {
        int count = fragmentManagePresenter.getFragmentList().size();
        return fragmentManagePresenter.getFragmentList().get(count - 1).getSnackbarContainer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Mysplash.ME_ACTIVITY:
                drawMeAvatar();
                break;
        }
    }

    public void changeTheme() {
        DisplayUtils.changeTheme(this);
        reboot();
    }

    public void reboot() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int enter_anim = android.R.anim.fade_in;
        int exit_anim = android.R.anim.fade_out;
        startActivity(intent);
        overridePendingTransition(enter_anim, exit_anim);
    }

    /** <br> presenter. */

    private void initPresenter() {
        this.fragmentManagePresenter = new FragmentManageImplementor(fragmentManageModel);
        this.messageManagePresenter = new MessageManageImplementor(this);
        this.meManagePresenter = new MeManageImplementor(this);
        this.drawerPresenter = new DrawerImplementor(drawerModel, this);
        this.downloadPresenter = new DownloadImplementor(downloadModel);
    }

    /** <br> view. */

    // init.

    private void initView() {
        this.handler = new SafeHandler<>(this);

        this.drawer = (DrawerLayout) findViewById(R.id.activity_main_drawerLayout);

        this.nav = (NavigationView) findViewById(R.id.activity_main_navView);
        if (Mysplash.getInstance().isLightTheme()) {
            nav.inflateMenu(R.menu.activity_main_drawer_light);
        } else {
            nav.inflateMenu(R.menu.activity_main_drawer_dark);
        }
        nav.setCheckedItem(drawerPresenter.getCheckedItemId());
        nav.setNavigationItemSelectedListener(this);

        if (AuthManager.getInstance().isAuthorized()) {
            nav.getMenu().getItem(1).setVisible(true);
        } else {
            nav.getMenu().getItem(1).setVisible(false);
        }

        View header = nav.getHeaderView(0);
        header.setOnClickListener(this);

        this.navAvatar = (CircleImageView) header.findViewById(R.id.container_nav_header_avatar);

        this.appIcon = (ImageView) header.findViewById(R.id.container_nav_header_appIcon);
        Glide.with(this)
                .load(R.drawable.ic_launcher)
                .into(appIcon);

        this.navTitle = (TextView) header.findViewById(R.id.container_nav_header_title);
        DisplayUtils.setTypeface(this, navTitle);

        this.navSubtitle = (TextView) header.findViewById(R.id.container_nav_header_subtitle);
        DisplayUtils.setTypeface(this, navSubtitle);

        this.navButton = (ImageButton) header.findViewById(R.id.container_nav_header_button);
        navButton.setOnClickListener(this);
    }

    private void buildFragmentStack() {
        List<Integer> idList;
        if (getBundle() != null) {
            idList = getBundle().getIntegerArrayList(KEY_MAIN_ACTIVITY_FRAGMENT_ID_LIST);
            if (idList == null) {
                idList = new ArrayList<>();
            }
            if (idList.size() == 0) {
                idList.add(R.id.action_home);
            }
        } else {
            idList = new ArrayList<>();
            idList.add(R.id.action_home);
            if (getIntent() != null && !TextUtils.isEmpty(getIntent().getAction())
                    && getIntent().getAction().equals("com.wangdaye.mysplash.Search")) {
                idList.add(R.id.action_search);
            }
        }
        fragmentManagePresenter.changeFragment(this, getBundle(), idList.get(0));
        for (int i = 1; i < idList.size(); i ++) {
            fragmentManagePresenter.addFragment(this, getBundle(), idList.get(i));
        }
    }

    // interface.

    public void changeFragment(int code) {
        fragmentManagePresenter.changeFragment(this, null, code);
    }

    public void insertFragment(int code) {
        fragmentManagePresenter.addFragment(this, null, code);
    }

    public void removeFragment() {
        fragmentManagePresenter.popFragment(this);
    }

    public MysplashFragment getTopFragment() {
        return fragmentManagePresenter.getTopFragment();
    }

    /** <br> model. */

    private void initModel(@Nullable Bundle savedInstanceState) {
        int selectedId = R.id.action_home;
        if (savedInstanceState != null) {
            selectedId = savedInstanceState.getInt(KEY_MAIN_ACTIVITY_SELECTED_ID, selectedId);
        }

        this.fragmentManageModel = new FragmentManageObject();
        this.drawerModel = new DrawerObject(selectedId);
        this.downloadModel = new DownloadObject();
    }

    /** <br> permission. */

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermission(int permissionCode, int type) {
        switch (permissionCode) {
            case Mysplash.WRITE_EXTERNAL_STORAGE:
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(
                            new String[] {
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            type);
                } else {
                    downloadPresenter.download();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permission, grantResult);
        for (int i = 0; i < permission.length; i ++) {
            switch (permission[i]) {
                case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                    if (grantResult[i] == PackageManager.PERMISSION_GRANTED) {
                        downloadPresenter.download();
                    } else {
                        NotificationUtils.showSnackbar(
                                getString(R.string.feedback_need_permission),
                                Snackbar.LENGTH_SHORT);
                    }
                    break;
            }
        }
    }

    /** <br> interface. */

    // on click listener.

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.container_nav_header:
                meManagePresenter.touchMeAvatar(this);
                break;

            case R.id.container_nav_header_button:
                meManagePresenter.touchMeButton(this);
                break;
        }
    }

    // on navigation item select listener.

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerPresenter.touchNavItem(item.getItemId());
        return true;
    }

    // on download photo listener. (photo adapter)

    @Override
    public void onDownload(Photo photo) {
        downloadPresenter.setDownloadKey(photo);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            downloadPresenter.download();
        } else {
            requestPermission(Mysplash.WRITE_EXTERNAL_STORAGE, DownloadHelper.DOWNLOAD_TYPE);
        }
    }

    // on write data listener. (authorize manager)

    @SuppressLint("SetTextI18n")
    @Override
    public void onWriteAccessToken() {
        nav.getMenu().getItem(1).setVisible(true);
        meManagePresenter.responseWriteAccessToken();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onWriteUserInfo() {
        meManagePresenter.responseWriteUserInfo();
    }

    @Override
    public void onWriteAvatarPath() {
        meManagePresenter.responseWriteAvatarPath();
    }

    @Override
    public void onLogout() {
        nav.getMenu().getItem(1).setVisible(false);
        meManagePresenter.responseLogout();
    }

    // handler.

    @Override
    public void handleMessage(Message message) {
        if (message.what == 1) {
            drawMeAvatar();
            drawMeTitle();
            drawMeSubtitle();
            drawMeButton();
        } else {
            messageManagePresenter.responseMessage(this, message.what, message.obj);
        }
    }

    // view.

    // message manage view.

    @Override
    public void sendMessage(final int what, final Object o) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                handler.obtainMessage(what, o).sendToTarget();
            }
        }, 400);
    }

    @Override
    public void responseMessage(int what, Object o) {
        // do nothing.
    }

    // me manage view.

    @Override
    public void drawMeAvatar() {
        if (!AuthManager.getInstance().isAuthorized()) {
            appIcon.setVisibility(View.VISIBLE);
            navAvatar.setVisibility(View.GONE);
        } else if (TextUtils.isEmpty(AuthManager.getInstance().getAvatarPath())) {
            navAvatar.setVisibility(View.VISIBLE);
            appIcon.setVisibility(View.GONE);
            Glide.with(Mysplash.getInstance())
                    .load(R.drawable.default_avatar)
                    .override(128, 128)
                    .into(navAvatar);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                navAvatar.setTransitionName(AuthManager.getInstance().getAccessToken());
            }
        } else {
            navAvatar.setVisibility(View.VISIBLE);
            appIcon.setVisibility(View.GONE);
            Glide.clear(navAvatar);
            DisplayUtils.loadAvatar(Mysplash.getInstance(), navAvatar, AuthManager.getInstance().getAvatarPath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                navAvatar.setTransitionName(AuthManager.getInstance().getAccessToken());
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void drawMeTitle() {
        if (!AuthManager.getInstance().isAuthorized()) {
            navTitle.setText("LOGIN");
        } else if (TextUtils.isEmpty(AuthManager.getInstance().getFirstName())
                || TextUtils.isEmpty(AuthManager.getInstance().getLastName())) {
            navTitle.setText("");
        } else {
            navTitle.setText(AuthManager.getInstance().getFirstName()
                    + " " + AuthManager.getInstance().getLastName());
        }
    }

    @Override
    public void drawMeSubtitle() {
        if (!AuthManager.getInstance().isAuthorized()) {
            navSubtitle.setText(getString(R.string.feedback_login_text));
        } else if (TextUtils.isEmpty(AuthManager.getInstance().getEmail())) {
            navSubtitle.setText("...");
        } else {
            navSubtitle.setText(AuthManager.getInstance().getEmail());
        }
    }

    @Override
    public void drawMeButton() {
        if (!AuthManager.getInstance().isAuthorized()) {
            if (Mysplash.getInstance().isLightTheme()) {
                navButton.setImageResource(R.drawable.ic_plus_mini_light);
            } else {
                navButton.setImageResource(R.drawable.ic_plus_mini_dark);
            }
        } else {
            if (Mysplash.getInstance().isLightTheme()) {
                navButton.setImageResource(R.drawable.ic_close_mini_light);
            } else {
                navButton.setImageResource(R.drawable.ic_close_mini_dark);
            }
        }
    }

    // drawer view.

    @Override
    public void touchNavItem(int id) {
        messageManagePresenter.sendMessage(id, null);
        drawer.closeDrawer(GravityCompat.START);
    }

    @Override
    public void setCheckedItem(int id) {
        nav.setCheckedItem(id);
    }

    /** <br> thread. */

    private PriorityRunnable runnable = new PriorityRunnable(false) {
        @Override
        public void run() {
            AuthManager.getInstance().addOnWriteDataListener(MainActivity.this);
            if (AuthManager.getInstance().isAuthorized()
                    && TextUtils.isEmpty(AuthManager.getInstance().getUsername())) {
                AuthManager.getInstance().refreshPersonalProfile();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                ShortcutsManager.refreshShortcuts(MainActivity.this);
            }
            IntroduceActivity.checkAndStartIntroduce(MainActivity.this);
            handler.obtainMessage(1).sendToTarget();
        }
    };
}
