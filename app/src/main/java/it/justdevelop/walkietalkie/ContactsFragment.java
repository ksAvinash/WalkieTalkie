package it.justdevelop.walkietalkie;


import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
public class ContactsFragment extends Fragment {


    public ContactsFragment() {
        // Required empty public constructor
    }

    View view;
    Context context;
    final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 7005;
    String LOG_TAG = " : ContactFetch :  ";
    List<profile_object> contactsAdapter = new ArrayList<>();
    ListView contactList;
    FirebaseFirestore db;
    DocumentReference backendContactListReference;
    SQLiteDatabaseHelper helper;
    private static final String APP_LOG_TAG = "WalkieTalkie2018";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_contacts, container, false);
        initializeViews();

        if(!isContactsPermissionProvided()){
            requestContactsPermission();
        }else {
            fetchContacts();
        }

        return view;
    }

    private void initializeViews(){
        context = getActivity().getApplicationContext();
        db = FirebaseFirestore.getInstance();

        contactList = view.findViewById(R.id.contactsList);
        helper = new SQLiteDatabaseHelper(context);

        final String my_phoneno = context.getSharedPreferences("wt_v1",Context.MODE_PRIVATE).getString("phoneno","");
        if(!my_phoneno.equals("")){
            setFirestoreDocumentListeners(my_phoneno);
        }
    }

    private void setFirestoreDocumentListeners(String my_phoneno) {
        backendContactListReference = FirebaseFirestore.getInstance().document("users/"+my_phoneno);
        backendContactListReference.addSnapshotListener(getActivity(), new EventListener<DocumentSnapshot>() {
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


        private boolean isContactsPermissionProvided(){
        return ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestContactsPermission(){
        Log.d(APP_LOG_TAG, LOG_TAG+"Request Contacts permission!");
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{android.Manifest.permission.READ_CONTACTS},
                MY_PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(APP_LOG_TAG, LOG_TAG+"contacts permission enabled");
                    FirestoreHelper helper = new FirestoreHelper();
                    helper.refreshContactStates(context);

                    fetchContacts();
                } else {
                    Toast.makeText(context,"Sorry cannot proceed without contacts", Toast.LENGTH_LONG).show();
                }
            }

        }
    }




















    private void fetchContacts(){
        contactsAdapter.clear();
        Cursor cursor = helper.getAllContacts();
        while(cursor.moveToNext()){
            contactsAdapter.add(new profile_object(cursor.getString(0),
                    cursor.getString(1), cursor.getInt(2), cursor.getString(3)));
        }
        cursor.close();
        displayList();
    }


    private void displayList(){
        Log.d(APP_LOG_TAG, LOG_TAG+"Adapter Count : "+contactsAdapter.size());
        ArrayAdapter<profile_object> adapter = new myContactsAdapterClass();
        contactList.setAdapter(adapter);
    }

    public class myContactsAdapterClass extends ArrayAdapter<profile_object> {
        myContactsAdapterClass() {
            super(context, R.layout.profile_item, contactsAdapter);
        }


        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                itemView = inflater.inflate(R.layout.profile_item, parent, false);
            }
            profile_object current = contactsAdapter.get(position);

            TextView profile_name = itemView.findViewById(R.id.profile_username);
            profile_name.setText(current.getName());

            Button invite_request = itemView.findViewById(R.id.invite_request);
            switch (current.getState()){
                case 1:
                    invite_request.setText("INVITE");
                    invite_request.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_dark_red_button));
                    break;

                case 2:
                    invite_request.setText("INVITED");
                    invite_request.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_red_button));
                    break;

                case 3:
                    invite_request.setText("REQUEST");
                    invite_request.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_orange_button));
                    break;

                case 4:
                    invite_request.setText("REQUESTED");
                    invite_request.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_yellow_button));
                    break;

                case 5:
                    invite_request.setText("FRIEND");
                    invite_request.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_green_button));
                    break;

                case 6:
                    invite_request.setText("MUTED");
                    invite_request.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_blue_button));
                    break;

                case 7:
                    invite_request.setText("BLOCKED");
                    invite_request.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_black_button));
                    break;
            }
            return itemView;
        }
    }





}
