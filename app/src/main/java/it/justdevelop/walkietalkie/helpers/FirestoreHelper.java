package it.justdevelop.walkietalkie.helpers;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;


public class FirestoreHelper {
    private static final String APP_LOG_TAG = "WalkieTalkie2018";

    private String LOG_TAG = " : firestoreHelper : ";
    private HashMap<String, Long> backendContactsList = new HashMap<>();
    private FirebaseFirestore db;

    private void updateState(final String my_number, final String number, final int new_state){
        db = FirebaseFirestore.getInstance();

        if(backendContactsList.containsKey(number))
            backendContactsList.remove(number);

        backendContactsList.put(number, Long.parseLong(new_state+""));

        db.collection("users").document(my_number).update("list", backendContactsList)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(APP_LOG_TAG, LOG_TAG+" updated state of number "+number+" : "+new_state);
                    }
                });
    }



    public void refreshContactStates(final Context context){
        final String my_phoneno = context.getSharedPreferences("wt_v1",Context.MODE_PRIVATE).getString("phoneno","");

        db = FirebaseFirestore.getInstance();
        if(!my_phoneno.equals("")){
            db.collection("users").document(my_phoneno).get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if(documentSnapshot.exists() && documentSnapshot.get("list") != null){
                                backendContactsList.putAll((Map<? extends String, ? extends Long>) documentSnapshot.get("list"));
                            }

                            if(isContactsPermissionProvided(context))
                                refreshContacts(my_phoneno, context);
                        }
                    });
        }else{
            Log.d(APP_LOG_TAG, LOG_TAG+" phone empty!");
        }


    }

    private void refreshContacts(final String my_phoneno, Context context){
        final SQLiteDatabaseHelper helper = new SQLiteDatabaseHelper(context);
        ContentResolver contentResolver = context.getContentResolver();
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
                        .replace(")","").replace("+","").replace("*","").replace("#","");
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
                    db.collection("users").document(number).get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    if(documentSnapshot.exists()){
                                        if(backendContactsList.containsKey(number)){
                                            helper.insertIntoUserProfiles(number, name, backendContactsList.get(number).intValue(), null);
                                        }else{
                                            updateState(my_phoneno, number, 3);
                                            helper.insertIntoUserProfiles(number, name, 3, null);
                                        }
                                    }else{
                                        updateState(my_phoneno, number, 1);
                                        helper.insertIntoUserProfiles(number, name, 1, null);
                                    }
                                }
                            });
                }
            }
        }
        cursor.close();
    }

    private boolean isContactsPermissionProvided(Context context){
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

}
