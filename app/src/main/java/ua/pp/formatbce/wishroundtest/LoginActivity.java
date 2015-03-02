package ua.pp.formatbce.wishroundtest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.Session;

/**
 * Created by format on 24.02.2015
 */
public class LoginActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new LoginFragment()).commit();
    }

    @Override
    public void onBackPressed() {
        new MaterialDialog.Builder(this)
                .title("Exit")
                .content("Do you want to close application?")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                })
                .positiveText("Exit")
                .negativeText("No")
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }
}
