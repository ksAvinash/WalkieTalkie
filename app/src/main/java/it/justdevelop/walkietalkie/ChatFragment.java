package it.justdevelop.walkietalkie;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import it.justdevelop.walkietalkie.helpers.FirestoreHelper;
import it.justdevelop.walkietalkie.helpers.SQLiteDatabaseHelper;
import it.justdevelop.walkietalkie.helpers.profile_object;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {


    public ChatFragment() {
        // Required empty public constructor
    }
    private static final String APP_LOG_TAG = "WalkieTalkie2018";
    final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 500;

    private View view;
    private FirebaseFirestore db;
    private Context context;
    private String email;
    private ListView chatList;
    private List<profile_object> contactAdapters = new ArrayList<>();
    SQLiteDatabaseHelper helper;
    DocumentReference backendContactsListReference;
    TextView permission_denied_text;


    String LOG_TAG = " : ChatFragment :  ";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.fragment_chat, container, false);
        initializeViews();


        if(!isContactsPermissionProvided()){
            requestContactsPermission();
        }else {
            fetchContacts();
        }

        return view;
    }



    private boolean isContactsPermissionProvided(){
        return ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void initializeViews(){
        context = getActivity().getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences("wk_v1", Context.MODE_PRIVATE);
        email = sharedPreferences.getString("email", "hello@123.com");
        final String my_phoneno = sharedPreferences.getString("phoneno","");
        permission_denied_text = view.findViewById(R.id.permission_denied_text);
        chatList = view.findViewById(R.id.chatList);
        helper = new SQLiteDatabaseHelper(context);

        db = FirebaseFirestore.getInstance();

        if(!my_phoneno.equals("")){
           setFirestoreDocumentListeners(my_phoneno);
        }
    }


    private void setFirestoreDocumentListeners(String my_phoneno){
        backendContactsListReference = FirebaseFirestore.getInstance().document("users/"+my_phoneno);
        backendContactsListReference.addSnapshotListener(getActivity(), new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot.exists() && documentSnapshot.get("list") != null){
                    HashMap<String, Long> myContactList = (HashMap<String, Long>) documentSnapshot.get("list");

                    for (String key : myContactList.keySet()) {
                        helper.insertIntoUserProfiles(key, null, myContactList.get(key).intValue(), null);
                    }
                    fetchContacts();
                }else {
                    Log.e(APP_LOG_TAG, LOG_TAG+"Got a list document exception "+e);
                }
            }
        });
    }


    private void requestContactsPermission(){
        Log.d(APP_LOG_TAG, LOG_TAG+"Request Contacts permission!");

        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.READ_CONTACTS)) {

            final AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(getActivity());
            }
            builder.setTitle("Contacts permissions needed")
                    .setCancelable(false)
                    .setMessage("Allow contacts permissions to start Walkie Talkie conversations")
                    .setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{Manifest.permission.READ_CONTACTS},
                                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
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
                    new String[]{Manifest.permission.READ_CONTACTS},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull  int[] grantResults) {
        Log.i(APP_LOG_TAG, LOG_TAG+"permission results invoked");

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the contacts read things now!

                    FirestoreHelper helper = new FirestoreHelper();
                    helper.refreshContactStates(context);

                    fetchContacts();
                } else {
                    chatList.setVisibility(View.GONE);
                    permission_denied_text.setVisibility(View.VISIBLE);
                    permission_denied_text.setText("Contacts permissions missing");

                }
            }
        }
    }


























    private void fetchContacts(){
        contactAdapters.clear();
        Cursor cursor =  helper.getAllFriends();
        Log.d(APP_LOG_TAG, LOG_TAG+"Total Friends : "+cursor.getCount());
        while(cursor.moveToNext()){
            contactAdapters.add(new profile_object(cursor.getString(0),
                    cursor.getString(1), cursor.getInt(2), cursor.getString(3)));
        }
        cursor.close();
        displayList();
    }



    private void displayList(){
        final ArrayAdapter<profile_object> adapter = new myChatAdapterClass();
        chatList.setAdapter(adapter);
        chatList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                profile_object clicked_profile = contactAdapters.get(position);

                Bundle bundle = new Bundle();
                bundle.putString("dest_number", clicked_profile.getPhoneno());
                bundle.putString("dest_name", clicked_profile.getName());
                bundle.putString("dest_profile_pic", clicked_profile.getProfile_pic());

                AudioConversationFragment audioConversationFragment = new AudioConversationFragment();
                audioConversationFragment.setArguments(bundle);
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, audioConversationFragment).addToBackStack(null).commit();
            }
        });
    }

    public class myChatAdapterClass extends ArrayAdapter<profile_object> {

        myChatAdapterClass() {
            super(context, R.layout.profile_item, contactAdapters);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                itemView = inflater.inflate(R.layout.profile_item, parent, false);
            }
            profile_object current = contactAdapters.get(position);

            Button invite_request = itemView.findViewById(R.id.invite_request);
            invite_request.setVisibility(View.GONE);


            TextView profile_name = itemView.findViewById(R.id.profile_username);
            profile_name.setText(current.getName());


            return itemView;
        }
    }


}
