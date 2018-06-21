package it.justdevelop.walkietalkie;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserSignup extends AppCompatActivity {


    TextView signup_email;
    EditText signup_username, signup_phoneno;
    Button signup_button;
    boolean isUsernameValid = false, isFetchingUsernames = false;
    private String TAG = "Signup";
    private Set<String> all_usernames = null;
    Context context;
    FirebaseFirestore db = null;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_signup);

        initializeViews();

        fetchAllUsernames();
    }



    private void initializeViews(){
        context = getApplicationContext();
        progressDialog = new ProgressDialog(this);
        db = FirebaseFirestore.getInstance();
        signup_email = findViewById(R.id.signup_email);
        signup_phoneno = findViewById(R.id.signup_phoneno);
        signup_username = findViewById(R.id.signup_username);
        signup_button = findViewById(R.id.signup_button);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            String email = bundle.getString("email");
            signup_email.setText(email);
        }


        Typeface type = Typeface.createFromAsset(getAssets(),"fonts/sans_pro.ttf");
        signup_phoneno.setTypeface(type);
        signup_username.setTypeface(type);
        signup_button.setTypeface(type);


        signup_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = signup_username.getText().toString();
                String phoneno = signup_phoneno.getText().toString();
                String email = signup_email.getText().toString();


                if(validateFields(username, phoneno) && validateUsername(username)){
                    progressDialog.setMessage("Registering user");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    createNewUser(email, username, phoneno);
                }
            }
        });

    }

    private void createNewUser(final String email, final String username, final String phoneno){

        Map<String, Object> docData = new HashMap<>();
        docData.put("phoneno", phoneno);
        docData.put("username", username);
        docData.put("pro_user", false);


        db.collection("users").document(email)
                .set(docData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "New user document added");

                        db.collection("quick_base").document("phonenos")
                                .update(phoneno, email)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "phoneno added to quick_base document");

                                        db.collection("quick_base").document("usernames")
                                                .update(username, email)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Log.d(TAG, "username added to quick_base document");

                                                        if(progressDialog.isShowing())
                                                            progressDialog.dismiss();
                                                        saveUserDetails(email, username, phoneno);
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.w(TAG, "Error adding username to quick_base : ", e);
                                                        if(progressDialog.isShowing())
                                                            progressDialog.dismiss();
                                                    }
                                                });


                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error adding phoneno to quick_base : ", e);
                                        if(progressDialog.isShowing())
                                            progressDialog.dismiss();

                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding user document", e);
                        if(progressDialog.isShowing())
                            progressDialog.dismiss();
                    }
                });


    }



    private void saveUserDetails(String email, String username, String phoneno){

        SharedPreferences sharedPreferences = getSharedPreferences("wt_v1", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("username", username);
        editor.putString("phoneno", phoneno);
        editor.apply();

        jumpToMainActivity();
    }


    private void jumpToMainActivity(){

        Intent intent = new Intent(UserSignup.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    private boolean validateFields(String username, String phoneno){
        if(username.length() < 4 || username.length() > 16){
            Toast.makeText(context, "username should be between 4 till 16 characters", Toast.LENGTH_SHORT).show();
            return false;
        }else if(!username.matches("[a-z0-9_]*")){
            Toast.makeText(context, "username can contain '0-9', '_', 'a-z' characters", Toast.LENGTH_SHORT).show();
            return false;
        }else if(phoneno.length() != 10){
            Toast.makeText(context, "phone number should have 10 numbers", Toast.LENGTH_SHORT).show();
            return false;
        }else if(!phoneno.matches("[0-9]*")){
            Toast.makeText(context, "phone can contain only numbers", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    private boolean validateUsername(String username){
        if(all_usernames != null){
            if(all_usernames.contains(username)){
                Toast.makeText(context, "username exists, choose a new one", Toast.LENGTH_SHORT).show();
                return false;
            }else{
                return true;
            }
        }else{
            if(!isFetchingUsernames)
                fetchAllUsernames();
        }
        return false;
    }



    private void fetchAllUsernames(){
        isFetchingUsernames = true;
        db.collection("quick_base").document("usernames")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        isFetchingUsernames = false;
                        if (task.isSuccessful()) {
                            Log.i(TAG, "users : "+task.getResult().getData().keySet());
                            all_usernames = task.getResult().getData().keySet();
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }




}
