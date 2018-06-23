package it.justdevelop.walkietalkie;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import it.justdevelop.walkietalkie.helpers.SQLiteDatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    DocumentReference user_states_reference;
    SharedPreferences sharedPreferences;
    SQLiteDatabaseHelper helper;


    private static final String APP_LOG_TAG = "WalkieTalkie2018";
    private static final String LOG_TAG = "MainActivity";



    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentManager fragmentManager;
            FragmentTransaction fragmentTransaction;

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    ChatFragment chatFragment = new ChatFragment();
                    fragmentManager = getSupportFragmentManager();
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragment_container, chatFragment).commit();
                    return true;


                case R.id.navigation_dashboard:
                    ContactsFragment contactsFragment = new ContactsFragment();
                    fragmentManager = getSupportFragmentManager();
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragment_container, contactsFragment).commit();
                    return true;

                case R.id.navigation_notifications:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();

        setFirestoreDocumentListeners();

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        ChatFragment chatFragment = new ChatFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, chatFragment).commit();


    }

    private void initializeViews(){
        sharedPreferences = getSharedPreferences("wk_v1", Context.MODE_PRIVATE);
        helper = new SQLiteDatabaseHelper(this);
    }


    private void setFirestoreDocumentListeners(){

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }



}
