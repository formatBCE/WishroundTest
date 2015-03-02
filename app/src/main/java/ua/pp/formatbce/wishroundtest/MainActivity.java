package ua.pp.formatbce.wishroundtest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AppEventsLogger;
import com.facebook.Session;
import com.shamanland.fab.FloatingActionButton;
import com.shamanland.fab.ShowHideOnScroll;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity
        implements DataWorker.OnFBDataCollectedListener {

    @InjectView(R.id.etSearch)
    EditText etSearch;
    @InjectView(R.id.lvUsers)
    ListView lvUsers;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @InjectView(R.id.fab)
    FloatingActionButton fab;


    private FBUsersAdapter adapter;
    private static ProgressDialog progress;
    private List<DataWorker.FBUser> allUsers;

    private static final int REQUEST_FB_LOGIN = 23;
    private String userId;
    NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        setSupportActionBar(toolbar);
        mDrawerLayout.setStatusBarBackgroundColor(
                getResources().getColor(android.R.color.black));

        mNavigationDrawerFragment.setUp(
                R.id.navigation_holder,
                mDrawerLayout);
        Session s = Session.getActiveSession();
        if (s == null || !s.isOpened()) {
            login();
        }
        fab.setOnClickListener((v) -> {
            preparePost();
        });
        adapter = new FBUsersAdapter(this, R.layout.list_item_user, new ArrayList<>());
        lvUsers.setAdapter(adapter);
        lvUsers.setOnItemClickListener((parent, view, position, id) -> {
            DataWorker.FBUser.showInfo(adapter.getItem(position), MainActivity.this);
        });
        lvUsers.setOnTouchListener(new ShowHideOnScroll(fab));
        progress = new ProgressDialog(this);
        progress.setTitle("Loading data");
        progress.setCancelable(false);
        progress.setIndeterminate(true);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.getFilter().filter(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void preparePost() {
        Session s = Session.getActiveSession();
        if (s == null || !s.isOpened()) {
            Toast.makeText(this, "Session closed, please login", Toast.LENGTH_SHORT).show();
            login();
        } else {
            FBPostFragment frg = new FBPostFragment();
            frg.show(getSupportFragmentManager(), "FB post");
        }
    }

    private void login() {
        startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_FB_LOGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FB_LOGIN) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            } else if (resultCode == RESULT_OK) {
                Log.e("OnActivityResult", "Starting worker");
                progress.show();
                new DataWorker(this, this).loadData();
            }
        }
    }

    @Override
    public void onCurrentUserInfo(DataWorker.FBUser user) {
        setProgress("Current user loaded");
        userId = user.getId();
        mNavigationDrawerFragment.init(user);
    }

    @Override
    public void onDataCollected(Set<DataWorker.FBUser> data) {
        setProgress("Friends loaded");
        progress.dismiss();
        adapter.addAll(data);
        allUsers = new ArrayList<>(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void setProgress(String whereWeAre) {
        Log.e("Progress", whereWeAre);
        progress.setMessage(whereWeAre);
    }

    @Override
    public void onError(String error) {
        progress.dismiss();
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        Session.getActiveSession().closeAndClearTokenInformation();
        super.onDestroy();
    }

    @Override
    public void onNewImageUrlReady(String id) {
        if (id.equals(this.userId)) {
            mNavigationDrawerFragment.onUserImageReady();
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    class FBUsersAdapter extends ArrayAdapter<DataWorker.FBUser> {

        private Filter filter;

        public FBUsersAdapter(Context context, int resource, List<DataWorker.FBUser> objects) {
            super(context, resource, objects);
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        String c = constraint == null ? "" : constraint.toString().trim().toLowerCase();
                        List<DataWorker.FBUser> res = new ArrayList<>();
                        if (c.isEmpty()) {
                            res.addAll(allUsers);
                        } else {
                            for (DataWorker.FBUser u : allUsers) {
                                if (u.getFName().toLowerCase().contains(c) || u.getLName().toLowerCase().contains(c)) {
                                    res.add(u);
                                }
                            }
                        }
                        FilterResults fr = new FilterResults();
                        fr.count = res.size();
                        fr.values = res;
                        return fr;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        List<DataWorker.FBUser> res = (List<DataWorker.FBUser>) results.values;
                        clear();
                        addAll(res);
                        notifyDataSetChanged();
                    }
                };
            }
            return filter;
        }

        class ViewHolder {
            @InjectView(R.id.ivUser)
            ImageView iv;
            @InjectView(R.id.tvUser)
            TextView tv;

            ViewHolder(View v) {
                ButterKnife.inject(this, v);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_user, null);
                convertView.setTag(new ViewHolder(convertView));
            }
            ViewHolder vh = (ViewHolder) convertView.getTag();
            DataWorker.FBUser u = getItem(position);
            vh.tv.setText(u.toString());
            if (u.getImgUrl() != null) {
                Picasso.with(MainActivity.this).load(u.getImgUrl()).resize(60, 60).into(vh.iv);
            }
            return convertView;
        }
    }
}
