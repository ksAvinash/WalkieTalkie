package it.justdevelop.walkietalkie;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
    Button holdToRecordButton, test;
    private static final int RECORD_TIMEOUT = 15000;
    String mFileName = null;
    FirebaseFirestore db;
    private static final String APP_LOG_TAG = "WalkieTalkie2018";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.fragment_audio_conversation, container, false);

        initializeViews();
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
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    startRecording();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    stopRecording();
                }
                return false;
            }
        });


        Log.i(APP_LOG_TAG, LOG_TAG+"audio conversation document path : "+document_path);

        setAudioMessagesListener();

        test = view.findViewById(R.id.test);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("/conversations/9449634042/9880430068/").document("file").get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                Log.i("CRITICAL", "Got the string");
                                String voice_string = documentSnapshot.getString("audio");
                                byte[] bytes = Base64.decode(voice_string, Base64.DEFAULT);

                                try (FileOutputStream fos = new FileOutputStream(mFileName)) {
                                    fos.write(bytes);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Log.i("CRITICAL", "Writing to file complete");

                                MediaPlayer mPlayer = new MediaPlayer();
                                try {
                                    mPlayer.setDataSource(mFileName);
                                    mPlayer.prepare();
                                    mPlayer.start();
                                } catch (IOException e) {
                                    Log.e(LOG_TAG, "prepare() failed");
                                }
                            }
                        });
            }
        });
    }


    private void setAudioMessagesListener(){

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


    private void startTimer(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(APP_LOG_TAG, LOG_TAG+ "Timeout, stopped recording");
                stopRecording();
            }
        }, RECORD_TIMEOUT);
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
