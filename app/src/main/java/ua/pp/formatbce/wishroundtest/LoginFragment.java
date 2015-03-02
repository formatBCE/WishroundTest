package ua.pp.formatbce.wishroundtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by format on 24.02.2015
 */
public class LoginFragment extends Fragment {

    @InjectView(R.id.authButton)
    LoginButton authButton;
    private UiLifecycleHelper uiHelper;
    private static final String PUBL_PERM = "publish_actions";
    private static final List<String> PERMISSIONS = Arrays.asList(PUBL_PERM);

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_login, null);
        ButterKnife.inject(this, root);
        authButton.setFragment(this);
        authButton.setReadPermissions("public_profile", "user_friends");
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
        return root;
    }

    private Session.StatusCallback callback = this::onSessionStateChange;

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if(checkPermissions()){
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        }
    }

    public static boolean checkPermissions() {
        Session s = Session.getActiveSession();
        if (s != null) {
            return s.getPermissions().contains("publish_actions");
        } else
            return false;
    }

    public void requestPublishPermissions() {
        Session s = Session.getActiveSession();
        if (s != null)
            s.requestNewPublishPermissions(new Session.NewPermissionsRequest(
                    this, PERMISSIONS));
    }

    @Override
    public void onResume() {
        super.onResume();
        Session session = Session.getActiveSession();
        if (session != null &&
                (session.isOpened() || session.isClosed())) {
            onSessionStateChange(session, session.getState(), null);
        }
        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
        if (Session.getActiveSession().isOpened()) {
            if (checkPermissions()) {
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
            } else {
                requestPublishPermissions();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

}
