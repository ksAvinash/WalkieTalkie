package it.justdevelop.walkietalkie.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "walkieTalkie_dB";
    private static final String TABLE_CONTACT_PROFILES = "wt_profiles";
    private static final String TABLE_GROUP_PROFILES = "wt_groups";


    private static final String USER_PHONE_NO = "user_phoneno";
    private static final String USER_NAME = "user_name";
    private static final String USER_STATE = "user_state";
    private static final String USER_PROFILE_PIC = "user_profile_pic";

    private static final String GROUP_ID = "group_id";
    private static final String GROUP_NAME = "group_name";
    private static final String GROUP_MEMBERS = "group_members";
    private static final String GROUP_PROFILE_PIC = "group_profile_pic";

    String LOG = "DatabaseHelper";

    public SQLiteDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
        Log.i(LOG, "Creating database!");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(LOG, "Creating tables!");

        String create_contact_profile_table =
                "create table "+TABLE_CONTACT_PROFILES+"("+USER_PHONE_NO+" text primary key, "+USER_NAME+
                        " text, "+USER_STATE+" integer, "+USER_PROFILE_PIC+" text);";
        db.execSQL(create_contact_profile_table);


        String create_group_profile_table = "create table "+TABLE_GROUP_PROFILES+" ("+GROUP_ID+" text primary key, "+GROUP_NAME
                +" text, "+GROUP_MEMBERS+" text, "+GROUP_PROFILE_PIC+" text)";
        db.execSQL(create_group_profile_table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists "+TABLE_GROUP_PROFILES);
        db.execSQL("drop table if exists "+TABLE_CONTACT_PROFILES);
        onCreate(db);
    }


    public void deleteTables(){
        SQLiteDatabase db = this.getWritableDatabase();
        onUpgrade(db,0,1);
    }


    public void insertIntoUserProfiles(String phoneno, String name, int state, String profile_pic){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select count(*) from "+TABLE_CONTACT_PROFILES+" where "+USER_PHONE_NO+" = "+phoneno+";",null);
        cursor.moveToNext();
        int count = cursor.getInt(0);
        cursor.close();
        if(count > 0){
            Log.i(LOG, "Updating entries for phoneno : "+phoneno);
            ContentValues contentValues = new ContentValues();
            contentValues.put(USER_NAME, name);
            contentValues.put(USER_STATE, state);
            contentValues.put(USER_PROFILE_PIC, profile_pic);
            db.update(TABLE_CONTACT_PROFILES, contentValues, USER_PHONE_NO+" = "+phoneno, null);
        }else{
            Log.i(LOG, "inserting entries for phoneno : "+phoneno);
            ContentValues contentValues = new ContentValues();
            contentValues.put(USER_PHONE_NO, phoneno);
            contentValues.put(USER_NAME, name);
            contentValues.put(USER_STATE, state);
            contentValues.put(USER_PROFILE_PIC, profile_pic);
            db.insert(TABLE_CONTACT_PROFILES, null, contentValues);
        }
        db.close();
    }

    public Cursor getAllFriends(){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("select * from "+TABLE_CONTACT_PROFILES+" where "+USER_STATE+" = 4;",null);
    }


}
