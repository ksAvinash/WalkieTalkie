package it.justdevelop.walkietalkie;


import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.justdevelop.walkietalkie.helpers.SQLiteDatabaseHelper;
import it.justdevelop.walkietalkie.helpers.contact_adapter;
import it.justdevelop.walkietalkie.helpers.profile_object;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {


    public ChatFragment() {
        // Required empty public constructor
    }

    private View view;
    private FirebaseFirestore db;
    private Context context;
    private String email;
    private ListView chatList;
    private List<profile_object> contactAdapters = new ArrayList<>();
    SQLiteDatabaseHelper helper;
    String LOG_TAG = "CHAT_FRAGMENT";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.fragment_chat, container, false);

        initializeViews();

        return view;
    }


    private void initializeViews(){
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);


        context = getActivity().getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences("wk_v1", Context.MODE_PRIVATE);
        email = sharedPreferences.getString("email", "hello@123.com");

        chatList = view.findViewById(R.id.chatList);
        helper = new SQLiteDatabaseHelper(context);
        fetchContactFromLocalDB();
    }



    private void fetchContactFromLocalDB(){
        Cursor cursor =  helper.getAllFriends();
        Log.d(LOG_TAG, "Total Friends : "+cursor.getCount());
        while(cursor.moveToNext()){
            contactAdapters.add(new profile_object(cursor.getString(0),
                    cursor.getString(1), cursor.getString(3)));
        }
        cursor.close();


        displayList();
    }



    private void displayList(){
        Log.i(LOG_TAG, "Adapter Count : "+contactAdapters.size());
        ArrayAdapter<profile_object> adapter = new myChatAdapterClass();
        chatList.setAdapter(adapter);
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
