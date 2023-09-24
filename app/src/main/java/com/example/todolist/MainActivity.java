package com.example.todolist;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    boolean undoneOnly=false;
    boolean sort=false;
    String category;
    EditText categoryFilter;
    EditText popupMinutes;
    Button okButton;
    CheckBox popupUndoneOnly;
    CheckBox popupSort;
    ArrayList<Task>tasks;
    ArrayList<Task>allTasks;
    RecyclerView recyclerView;
    SQLiteDatabase sqLiteDatabase;
    TaskListAdapter taskListAdapter;
    EditText searchText;
    Cursor resultSet;
    int minutesBeforeNotification;
    ImageButton settingsButton;
    NotificationManager notificationManager;
    AlarmManager alarmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createTasksFolder();

        tasks=new ArrayList<>();
        allTasks=new ArrayList<>();

        searchText=findViewById(R.id.search_input);
        recyclerView=findViewById(R.id.recycler_view);
        settingsButton=findViewById(R.id.settings_btn);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        settingsButton.setOnClickListener(view -> showSettings());
        notificationManager=getSystemService(NotificationManager.class);
        alarmManager=(AlarmManager) getSystemService(ALARM_SERVICE);
        taskListAdapter=new TaskListAdapter(tasks, this, sqLiteDatabase, this.alarmManager);
        recyclerView.setAdapter(taskListAdapter);
        searchText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                getResultsByTaskName(searchText.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        createNotificationChannel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tasks.clear();
        sqLiteDatabase=openOrCreateDatabase("tasks", MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS taskslist(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task VARCHAR(40), description VARCHAR(150), category VARCHAR(50),date DATE, time TIME, notifications BOOLEAN, attachment BOOLEAN, done BOOLEAN)");

        readMinutes();
        getAllTasks();
        getRecordsFromDatabase();
        getMillisecondsForTasksAndSetNotifications();
    }

    private void getResultsByTaskName(String taskName){
        if(taskName.equals("")){
            getRecordsFromDatabase();
        }
        else{
            String sql="SELECT * FROM taskslist WHERE task LIKE '"+taskName+"%'";
            resultSet=sqLiteDatabase.rawQuery(sql, null);
            tasks.clear();
            while(resultSet.moveToNext()){
                tasks.add(new Task(resultSet.getInt(0), resultSet.getString(1), resultSet.getString(2),
                        resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getInt(6),
                        resultSet.getInt(7), resultSet.getInt(8)));
            }
            taskListAdapter.taskList=tasks;
            taskListAdapter.notifyDataSetChanged();
            taskListAdapter.database=sqLiteDatabase;
        }
    }

    private void readMinutes(){
        SharedPreferences sharedPreferences=getSharedPreferences("shared_preferences", MODE_PRIVATE);
        minutesBeforeNotification= sharedPreferences.getInt("minutes", 0);
    }

    private void saveMinutes(int minutes){
        SharedPreferences sharedPreferences=getSharedPreferences("shared_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putInt("minutes", minutes);
        editor.apply();
    }

    public void goToNewTask(View view){
        Intent intent=new Intent(this, NewTask.class);
        startActivity(intent);
    }

    private void showSettings(){
        Dialog dialog=new Dialog(this);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.settings_popup);
        popupUndoneOnly=dialog.findViewById(R.id.popup_undone_only);
        popupSort=dialog.findViewById(R.id.sort);
        okButton=dialog.findViewById(R.id.ok_btn);
        categoryFilter=dialog.findViewById(R.id.category_filter);
        popupMinutes=dialog.findViewById(R.id.minutes);
        popupMinutes.setText(String.valueOf(minutesBeforeNotification));
        if(category!=null){
            categoryFilter.setText(category);
        }
        popupUndoneOnly.setChecked(undoneOnly);
        popupSort.setChecked(sort);
        popupUndoneOnly.setOnClickListener(view -> undoneOnly= !undoneOnly);
        popupSort.setOnClickListener(view -> sort=!sort);
        okButton.setOnClickListener(view->{
            try{
                category=categoryFilter.getText().toString();
                String minutes=popupMinutes.getText().toString();
                int minutesInt=Integer.parseInt(minutes);
                if(minutesInt>=0){
                    dialog.dismiss();
                    minutesBeforeNotification=minutesInt;
                    saveMinutes(minutesInt);
                    getMillisecondsForTasksAndSetNotifications();
                    getRecordsFromDatabase();
                }else{
                    Toast.makeText(this, "Nie można podać wartości ujemnej", Toast.LENGTH_SHORT).show();
                }

            }catch(NumberFormatException e){
                Toast.makeText(this, "Podana wartosć jest nieprawidłowa", Toast.LENGTH_SHORT).show();
            }

        });
        dialog.show();
    }

    private String buildSql(){
        String sql;
        if(undoneOnly){
            sql="SELECT * FROM taskslist WHERE done='0'";
        }
        else{
            sql="SELECT * FROM taskslist";
        }

        if(category!=null && category.length()>0){
            if(undoneOnly){
                sql+=" AND category='";
            }else{
                sql+=" WHERE category='";
            }
            sql+=category;
            sql+="'";
        }

        if(sort){
            sql+=" ORDER BY date ASC, time ASC";
        }
        return sql;
    }

    private void getRecordsFromDatabase(){
        String sql=buildSql();
        resultSet=sqLiteDatabase.rawQuery(sql, null);
        tasks.clear();
        while(resultSet.moveToNext()){
            tasks.add(new Task(resultSet.getInt(0), resultSet.getString(1), resultSet.getString(2),
                    resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getInt(6),
                    resultSet.getInt(7), resultSet.getInt(8)));
        }
        taskListAdapter.taskList=tasks;
        taskListAdapter.notifyDataSetChanged();
        taskListAdapter.database=sqLiteDatabase;
    }

    private void getAllTasks(){
        String sql="SELECT * FROM taskslist";
        resultSet=sqLiteDatabase.rawQuery(sql, null);
        allTasks.clear();
        while(resultSet.moveToNext()){
            allTasks.add(new Task(resultSet.getInt(0), resultSet.getString(1), resultSet.getString(2),
                    resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getInt(6),
                    resultSet.getInt(7), resultSet.getInt(8)));
        }
    }

    private void createTasksFolder(){
        File directory=this.getDir("attachments", MODE_PRIVATE);
        if(!directory.exists()){
            directory.mkdir();
        }
    }

    private void createNotificationChannel(){
        String name="ToDoListReminderChannel";
        String description="Channel for ToDo List notifications.";
        NotificationChannel channel=new NotificationChannel("notifyTask", name, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(description);

        notificationManager.createNotificationChannel(channel);
    }

    private void setAlarmManagerForTaskAndSendNotification(long milliseconds, String task, int id){
        Intent intent=new Intent(MainActivity.this, NotificationBroadcast.class);
        intent.putExtra("task", task);
        intent.putExtra("id", id);
        PendingIntent pendingIntent=PendingIntent.getBroadcast(MainActivity.this, id, intent, PendingIntent.FLAG_IMMUTABLE);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, milliseconds, pendingIntent);
    }

    private long getMillisecondsFromDate(String date){
        long res=0;
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
        try{
            Date mDate=simpleDateFormat.parse(date);
            res=mDate.getTime();
        }catch(ParseException e){
            e.printStackTrace();
        }
        return res;
    }

    private long getMillisecondsFromTime(String time){
        long res=0;
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("HH:mm");
        try{
            Date mDate=simpleDateFormat.parse(time);
            res=mDate.getTime();
        }catch(ParseException e){
            e.printStackTrace();
        }
        return res;
    }

    private void getMillisecondsForTasksAndSetNotifications(){
        cancelAllTasks();
        for(Task t:allTasks){
            if(t.notification==1){
                long milliseconds=getMillisecondsFromDate(t.date);
                milliseconds+=getMillisecondsFromTime(t.hour);
                milliseconds+=3600000;
                milliseconds-=(long)minutesBeforeNotification*60000;
                Calendar calendar= Calendar.getInstance();
                calendar.setTimeInMillis(milliseconds);
                if(milliseconds>System.currentTimeMillis())
                    setAlarmManagerForTaskAndSendNotification(milliseconds, t.title, t.id);
            }
        }
    }

    private void cancelAllTasks(){
        for(Task t:allTasks){
            Intent intent=new Intent(MainActivity.this, NotificationBroadcast.class);
            intent.putExtra("task", t.title);
            intent.putExtra("id", t.id);
            PendingIntent pendingIntent=PendingIntent.getBroadcast(MainActivity.this, t.id, intent, PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
        }
    }
}