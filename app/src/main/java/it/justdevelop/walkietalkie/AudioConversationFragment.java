package it.justdevelop.walkietalkie;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import it.justdevelop.walkietalkie.helpers.FirestoreHelper;


/**
 * A simple {@link Fragment} subclass.
 */
public class AudioConversationFragment extends Fragment {


    public AudioConversationFragment() {
        // Required empty public constructor
    }

    private MediaRecorder mRecorder;
    View view;
    Context context;
    String dest_phone, dest_name, dest_profile_pic;
    String my_phone;
    String document_path;
    String LOG_TAG = " : AudioConversation :  ";
    Button holdToRecordButton;
    private static final int RECORD_TIMEOUT = 15000;
    String mFileName = null;
    FirebaseFirestore db;
    private static final String APP_LOG_TAG = "WalkieTalkie2018";
    final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 854;
    TextView permission_denied_text;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.fragment_audio_conversation, container, false);
        initializeViews();


        if(!isRecordAudioPermissionProvided())
            requestRecordAudioPermission();
        return view;
    }






    @SuppressLint("ClickableViewAccessibility")
    private void initializeViews(){
        context = getActivity().getApplicationContext();
        Bundle bundle = getArguments();
        dest_phone = bundle.getString("dest_number");
        dest_name = bundle.getString("dest_name");
        dest_profile_pic = bundle.getString("dest_profile_pic");
        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = context.getSharedPreferences("wt_v1",Context.MODE_PRIVATE);
        my_phone = sharedPreferences.getString("phoneno","");
        permission_denied_text = view.findViewById(R.id.permission_denied_text);


        if(Long.parseLong(dest_phone) > Long.parseLong(my_phone)){
            document_path = dest_phone+"_"+my_phone;
        }else{
            document_path = my_phone+"_"+dest_phone;
        }
        mFileName = context.getExternalCacheDir().getAbsolutePath()+"/wt.3gp";
        Log.i(APP_LOG_TAG, LOG_TAG+"mFileName : "+mFileName);


        holdToRecordButton = view.findViewById(R.id.holdToRecordButton);

        holdToRecordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(isRecordAudioPermissionProvided()){
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        startRecording();
                    }else if(event.getAction() == MotionEvent.ACTION_UP){
                        stopRecording();
                    }
                }
                return false;
            }
        });



    }

    private boolean isRecordAudioPermissionProvided(){
        return ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }




    private void requestRecordAudioPermission(){
        Log.d(APP_LOG_TAG, LOG_TAG+"Request record audio permission!");

        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.RECORD_AUDIO)) {

            final AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(getActivity());
            }
            builder.setTitle("Record Audio permissions needed")
                    .setCancelable(false)
                    .setMessage("Allow record permissions to start Walkie Talkie Audio Messages")
                    .setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                            dialog.dismiss();
                        }

                    })
                    .setNegativeButton("Nope", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();

        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull  int[] grantResults) {
        Log.i(APP_LOG_TAG, LOG_TAG+"permission results invoked");

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    permission_denied_text.setVisibility(View.VISIBLE);
                    permission_denied_text.setText("Record Audio Permissions Missing");

                }
            }
        }
    }














    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(APP_LOG_TAG, LOG_TAG+"prepare() failed");
        }
        mRecorder.start();
        Log.i(APP_LOG_TAG, LOG_TAG+ "Started recording!");
    }


    private void stopRecording() {
        Log.i(APP_LOG_TAG, LOG_TAG+ "Stopped recording!");
        try{
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;

            uploadAudio();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void uploadAudio(){
        File file = new File(mFileName);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        String voiceString = Base64.encodeToString(bytes, Base64.DEFAULT);

        HashMap<String, Object> myAudio = new HashMap<>();
        myAudio.put("audio", voiceString);
        myAudio.put("audio_status", 1);
        Log.i("CRITICAL", my_phone);
        db.collection("conversations/"+dest_phone+"/"+my_phone).document("file")
                .set(myAudio)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(APP_LOG_TAG, LOG_TAG+ "Avinash, you are amazing!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(APP_LOG_TAG, LOG_TAG+ "Oh Okay, it didn't work : "+e);
                    }
                });
    }





}
