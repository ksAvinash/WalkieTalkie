package it.justdevelop.walkietalkie;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import it.justdevelop.walkietalkie.helpers.SQLiteDatabaseHelper;
import it.justdevelop.walkietalkie.helpers.contact_adapter;

public class SplasherActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String LOG_TAG = "FirebaseAuth";
    SQLiteDatabaseHelper helper;
    FirebaseFirestore db;
    HashMap<String, Long> myProfileList = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splasher);

        mAuth = FirebaseAuth.getInstance();
        helper = new SQLiteDatabaseHelper(this);
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);


        fetchMyUserProfileDocument();

        if(isContactsPermissionProvided()){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    fetchContacts();
                }
            }, 2000);
        }

    }

    private void fetchMyUserProfileDocument(){
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            String email = currentUser.getEmail();
            db.collection("users")
                    .document(email)
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                       myProfileList.putAll((Map<? extends String, ? extends Long>) task.getResult().get("list"));
                       Log.i(LOG_TAG, "Fetching my user profile complete");
                    } else {
                        Log.w(LOG_TAG, "Error getting my user profiles.", task.getException());
                    }
                }
            });
        }
    }




    private boolean isContactsPermissionProvided(){
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }


    private void fetchContacts(){
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        while(cursor.moveToNext()){
            final String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID+" = ? ", new String[]{id}, null
            );
            String[] phonenos = new String[phoneCursor.getCount()];
            int i = 0;
            while(phoneCursor.moveToNext()){
                String phoneno = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        .trim().replace(" ","").replace("-","").replace("(","")
                        .replace(")","").replace("+","");
                if(phoneno.length() > 10){
                    phoneno = phoneno.substring(phoneno.length()-10, phoneno.length());
                }
                if(phoneno.length() != 10){
                    continue;
                }
                phonenos[i++] = phoneno;
            }
            phoneCursor.close();


            for(final String number : phonenos){
                if(number != null){
                    db.collection("profiles")
                            .document(number)
                            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                if(task.getResult().getData() != null){
                                    String profile_pic = task.getResult().getString("profile_pic");
                                    if(myProfileList != null){
                                        Long status = myProfileList.get(number);
                                        if(status == null){
                                            Log.i(LOG_TAG, "number :"+number+" state : "+2);
                                            helper.insertIntoUserProfiles(number, name, 2, profile_pic);
                                        }else{
                                            Log.i(LOG_TAG, "number :"+number+" state : "+status);
                                            helper.insertIntoUserProfiles(number, name, status.intValue(), profile_pic);
                                        }
                                    }else{
                                        Log.i(LOG_TAG, "My profile is null for "+number);
                                    }

                                }else{
                                    Log.i(LOG_TAG, "number :"+number+" state : "+1);
                                    helper.insertIntoUserProfiles(number, name, 1, null);
                                }
                            } else {
                                Log.w(LOG_TAG, "Error getting phone profiles.", task.getException());
                            }
                        }
                    });
                }
            }
        }
        cursor.close();
    }




    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            Log.i(LOG_TAG, "Jumping to Google SignIn Activity");
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
        }, 6000);
    }




}
