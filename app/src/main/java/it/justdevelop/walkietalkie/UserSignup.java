package it.justdevelop.walkietalkie;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserSignup extends AppCompatActivity {


    TextView signup_email;
    EditText signup_phoneno;
    Button signup_button;
    boolean isFetchingPhoneNumbers = false;
    private String LOG_TAG = " : signup :  ";
    private static final String APP_LOG_TAG = "WalkieTalkie2018";

    Context context;
    FirebaseFirestore db = null;
    CollectionReference collection_ref;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_signup);

        initializeViews();

    }



    private void initializeViews(){
        context = getApplicationContext();
        db = FirebaseFirestore.getInstance();
        collection_ref = FirebaseFirestore.getInstance().collection("users");
        signup_email = findViewById(R.id.signup_email);
        signup_phoneno = findViewById(R.id.signup_phoneno);
        signup_button = findViewById(R.id.signup_button);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            String email = bundle.getString("email");
            signup_email.setText(email);
        }


        Typeface type = Typeface.createFromAsset(getAssets(),"fonts/sans_pro.ttf");
        signup_phoneno.setTypeface(type);
        signup_button.setTypeface(type);

        signup_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneno = signup_phoneno.getText().toString();
                String email = signup_email.getText().toString();


                if(validateFields(phoneno)){
                    progressDialog = new ProgressDialog(UserSignup.this);
                    progressDialog.setMessage("Checking our servers..");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    validatePhoneNumber(email, phoneno);
                }
            }
        });

    }

    private void createNewUser(final String email, final String phoneno){
        progressDialog.setMessage("Creating new profile");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Map<String, Object> docData = new HashMap<>();
        docData.put("email", email);
        docData.put("pro_user", false);


        db.collection("users").document(phoneno)
                .set(docData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        db.collection("quick_base").document("references").update( email.replace(".","_"), phoneno);
                        saveUserDetails(email, phoneno);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(APP_LOG_TAG, LOG_TAG+ "Error adding user document", e);
                        if(progressDialog.isShowing())
                            progressDialog.dismiss();
                    }
                });
    }



    private void saveUserDetails(String email, String phoneno){
        if(progressDialog.isShowing())
            progressDialog.dismiss();

        SharedPreferences sharedPreferences = getSharedPreferences("wt_v1", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("phoneno", phoneno);
        editor.apply();

        jumpToMainActivity();
    }


    private void jumpToMainActivity(){

        Intent intent = new Intent(UserSignup.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    private boolean validateFields(String phoneno){
        if(phoneno.length() != 10){
            Toast.makeText(context, "phone number should have 10 numbers", Toast.LENGTH_SHORT).show();
            return false;
        }else if(!phoneno.matches("[0-9]*")){
            Toast.makeText(context, "phone can contain only numbers", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void validatePhoneNumber(final String email, final String phoneno){
        isFetchingPhoneNumbers = true;
        db.collection("users").document(phoneno).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()){
                    if(progressDialog.isShowing())
                        progressDialog.dismiss();
                    Toast.makeText(context, "Phone number exists in database, choose a new one", Toast.LENGTH_SHORT).show();
                }else {
                    createNewUser(email, phoneno);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }




}
