package it.justdevelop.walkietalkie;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import it.justdevelop.walkietalkie.helpers.FirestoreHelper;

public class SplasherActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String LOG_TAG = " : SplasherActivity :  ";
    private static final String APP_LOG_TAG = "WalkieTalkie2018";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splasher);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        mAuth = FirebaseAuth.getInstance();


        FirestoreHelper helper = new FirestoreHelper();
        helper.refreshContactStates(this);

    }


    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            Log.d(APP_LOG_TAG, LOG_TAG+ "Jumping to Google SignIn Activity");
            jumpToGoogleSignInActivity();
        }else{
            jumpToMainActivity();
        }
    }


    private void jumpToGoogleSignInActivity(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplasherActivity.this, GoogleSignInActivity.class);
                startActivity(intent);
                finish();
            }
        }, 4000);
    }


    private void jumpToMainActivity(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplasherActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 4000);
    }




}
