package it.justdevelop.walkietalkie;


import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import it.justdevelop.walkietalkie.helpers.contact_adapter;


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
    String LOG = "ContactFetch";
    List<contact_adapter> contactsAdapter = new ArrayList<>();
    ListView contactList;
    FirebaseFirestore db;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_contacts, container, false);
        initializeViews();

        if(isContactsPermissionProvided()){
            fetchContacts();
        }else{
            requestContactsPermission();
        }

        return view;
    }

    private void initializeViews(){
        context = getActivity().getApplicationContext();
        db = FirebaseFirestore.getInstance();
        contactList = view.findViewById(R.id.contactsList);
    }


    private boolean isContactsPermissionProvided(){
        return ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestContactsPermission(){
        Log.d(LOG,"Request Contacts permission!");
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
                    fetchContacts();
                } else {
                    Toast.makeText(context,"Sorry cannot proceed without contacts", Toast.LENGTH_LONG).show();
                }
            }

        }
    }





    private void fetchContacts(){
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        while(cursor.moveToNext()){
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
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
            contactsAdapter.add(new contact_adapter(id, name, phonenos));
            phoneCursor.close();
        }
        cursor.close();


        displayList();
    }


    private void displayList(){
        ArrayAdapter<contact_adapter> adapter = new myContactsAdapterClass();
        contactList.setAdapter(adapter);
    }

    public class myContactsAdapterClass extends ArrayAdapter<contact_adapter> {
        boolean isExistingUser = false;
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
            contact_adapter current = contactsAdapter.get(position);

            TextView profile_name = itemView.findViewById(R.id.profile_username);
            profile_name.setText(current.getName());

            final Button invite_request = itemView.findViewById(R.id.invite_request);

            isExistingUser = false;
            String[] numbers = current.getPhonenos();

            for(final String number : numbers){
                if(number != null){
                        db.collection("profiles")
                                .document(number)
                                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    if(task.getResult().getData() == null){
                                        Log.w(LOG, "phone not found : "+number);
                                        invite_request.setText("INVITE");
                                    }else{
                                        invite_request.setText("REQUEST");
                                        invite_request.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_green_button));
                                        isExistingUser = true;
                                        Log.w(LOG, "user found");
                                        Log.i(LOG, "Data : "+task.getResult().getData());
                                    }
                                } else {
                                    Log.w(LOG, "Error getting phone profiles.", task.getException());
                                }
                            }
                        });
                }
            }





            return itemView;
        }
    }





}
