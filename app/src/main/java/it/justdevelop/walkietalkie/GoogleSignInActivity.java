package it.justdevelop.walkietalkie;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GoogleSignInActivity extends AppCompatActivity {


    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private static final String LOG_TAG = "GoogleActivity : ";
    private static final String APP_LOG_TAG = "WalkieTalkie2018";

    private static final int RC_SIGN_IN = 8003;

    ProgressDialog progressDialog;
    String email = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_sign_in);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        SignInButton googleSignInButton = findViewById(R.id.googleSignInButton);
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
    }






    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                email = account.getEmail();
                Log.d(APP_LOG_TAG, LOG_TAG+ email+" signin successful");
                updateFirebaseDatabase(account);
            } catch (ApiException e) {
                Log.d(APP_LOG_TAG, LOG_TAG+ "Google sign in failed", e);
            }
        }
    }





    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct, final String email, final String bio, final String phoneno, final String profile_pic) {

        progressDialog.setCancelable(false);
        progressDialog.setMessage("Fetching user details..");
        progressDialog.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(APP_LOG_TAG, LOG_TAG+ "Firebase auth successful");
                            saveUserDetails(email, bio, phoneno, profile_pic);
                        } else {
                            Log.w(APP_LOG_TAG, LOG_TAG+ "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(R.id.google_signin_activity), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            if(progressDialog.isShowing())
                                progressDialog.dismiss();

                        }

                    }
                });
    }


    private void updateFirebaseDatabase(final GoogleSignInAccount account) {
        if(progressDialog.isShowing()){
            progressDialog.setMessage("One moment..");
            progressDialog.show();
        }

        if (email != null) {
            final FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("quick_base")
                    .document("references")
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        Map<String, Object> email_phone_reference = task.getResult().getData();
                        if(email_phone_reference.containsKey(email.replace(".","_"))){
                            Log.w(APP_LOG_TAG, LOG_TAG+"gmail id is present : "+email);
                            final String phoneno = (String) email_phone_reference.get(email.replace(".","_"));

                            db.collection("users").document(phoneno).get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    if(documentSnapshot.exists()){
                                        firebaseAuthWithGoogle(account, documentSnapshot.getString("email"), documentSnapshot.getString("bio"), phoneno, documentSnapshot.getString("profile_pic"));
                                    }else {
                                        jumpToSignUpActivity();
                                        Log.w(APP_LOG_TAG, LOG_TAG+"User was not found!");
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(APP_LOG_TAG, LOG_TAG+"Error getting users documents.", e);
                                }
                            });

                        }else{
                            jumpToSignUpActivity();
                        }
                    } else {
                        Log.w(APP_LOG_TAG, LOG_TAG+"Error getting quick_base documents.", task.getException());
                    }
                }
            });
        }else{
            if(progressDialog.isShowing())
                progressDialog.dismiss();
        }



    }





    private void saveUserDetails(String email, String bio, String phoneno, String profile_pic){
        Log.i(APP_LOG_TAG, LOG_TAG+"saving user details "+email+" : "+phoneno+" : "+profile_pic);

        SharedPreferences sharedPreferences = getSharedPreferences("wt_v1", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("bio", bio);
        editor.putString("phoneno", phoneno);
        editor.putString("profile_pic", profile_pic);
        editor.apply();

        jumpToMainActivity();
    }


    private void jumpToMainActivity(){
        if(progressDialog.isShowing())
            progressDialog.dismiss();

        Intent intent = new Intent(GoogleSignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void jumpToSignUpActivity(){
        if(progressDialog.isShowing())
            progressDialog.dismiss();

        Intent intent = new Intent(GoogleSignInActivity.this, UserSignup.class);

        Bundle bundle = new Bundle();
        bundle.putString("email", email);

        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }






}
