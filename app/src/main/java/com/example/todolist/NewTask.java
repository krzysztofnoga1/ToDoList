package com.example.todolist;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;

public class NewTask extends AppCompatActivity {

    private boolean attachment=false;
    private DocumentFile source;
    private String filename;
    private String uri;
    int taskId;
    private EditText taskName;
    private EditText taskDescription;
    private EditText taskCategory;
    private EditText dateInput;
    private EditText hourInput;
    private CheckBox notifications;
    private TextView attachmentPath;
    SQLiteDatabase sqLiteDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);
        taskName=findViewById(R.id.task_name);
        taskDescription=findViewById(R.id.task_description);
        taskCategory=findViewById(R.id.task_category);
        dateInput=findViewById(R.id.date_input);
        hourInput=findViewById(R.id.hour_input);
        notifications=findViewById(R.id.notifications_on);
        attachmentPath=findViewById(R.id.attachment_path_add);
        this.uri="";
        sqLiteDatabase=openOrCreateDatabase("tasks", MODE_PRIVATE, null);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("attachment", attachment);
        savedInstanceState.putString("uri", uri);
        savedInstanceState.putString("filename", filename);
        savedInstanceState.putInt("taskId", taskId);
        savedInstanceState.putString("taskName", taskName.getText().toString());
        savedInstanceState.putString("taskDescription", taskDescription.getText().toString());
        savedInstanceState.putString("taskCategory", taskCategory.getText().toString());
        savedInstanceState.putString("hourInput", hourInput.getText().toString());
        savedInstanceState.putString("dateInput", dateInput.getText().toString());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceBundle){
        super.onRestoreInstanceState(savedInstanceBundle);
        this.attachment=savedInstanceBundle.getBoolean("attachment");
        this.uri= savedInstanceBundle.getString("uri");
        if(attachment){
            Uri newUri=Uri.parse(this.uri);
            this.source=DocumentFile.fromSingleUri(this, newUri);
            this.attachmentPath.setText("Dodano");
        }
        this.filename=savedInstanceBundle.getString("filename");
        this.taskId=savedInstanceBundle.getInt("taskId");
        this.taskName.setText(savedInstanceBundle.getString("taskName"));
        this.taskDescription.setText(savedInstanceBundle.getString("taskDescription"));
        this.taskCategory.setText(savedInstanceBundle.getString("taskCategory"));
        this.hourInput.setText(savedInstanceBundle.getString("hourInput"));
        this.dateInput.setText(savedInstanceBundle.getString("dateInput"));
        this.sqLiteDatabase=openOrCreateDatabase("tasks", MODE_PRIVATE, null);
    }

    public void addTask(View view){
        if(taskName.getText().toString().equals("") || dateInput.getText().toString().equals("") || dateInput.getText().toString().equals("")){
            Toast.makeText(this, "Nazwa zadania, data i godzina nie mogą być puste", Toast.LENGTH_SHORT).show();
        }else{
            String sql=buildSQL();
            sqLiteDatabase.execSQL(sql);
            getLastIdAndCreateDirectory();
            if(attachment){
                try{
                    createDirectoryAndSaveFile();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
            sqLiteDatabase.close();
            this.finish();
        }
    }

    private void getLastIdAndCreateDirectory(){
        String sql="SELECT MAX(id) from taskslist";
        Cursor res=sqLiteDatabase.rawQuery(sql, null);
        res.moveToFirst();
        this.taskId=res.getInt(0);
        String folderName=String.valueOf(this.taskId);
        File task_directory=new File(this.getDir("attachments", MODE_PRIVATE), folderName);
        if(!task_directory.exists()){
            task_directory.mkdir();
        }
        res.close();
    }

    private String buildSQL(){
        String sql="INSERT INTO taskslist(task, description, category, date, time, notifications, attachment, done) VALUES ('";
        sql+=taskName.getText().toString();
        sql+="','";
        sql+=taskDescription.getText().toString();
        sql+="','";
        sql+=taskCategory.getText().toString();
        sql+="','";
        sql+=dateInput.getText().toString();
        sql+="','";
        sql+=hourInput.getText().toString();
        sql+="','";
        if(notifications.isChecked())
            sql+="1";
        else
            sql+="0";
        sql+="','";
        if(attachment)
            sql+="1";
        else
            sql+="0";
        sql+="','0')";
        return sql;
    }

    public void chooseDate(View view){
        final Calendar calendar=Calendar.getInstance();

        int year=calendar.get(Calendar.YEAR);
        int month=calendar.get(Calendar.MONTH);
        int day=calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog=new DatePickerDialog(NewTask.this, R.style.DatePickerTheme,
                (datePicker, year1, month1, day1) -> setDate(year1,month1,day1),year,month,day);
        datePickerDialog.show();
    }

    public void chooseTime(View view){
        final Calendar calendar=Calendar.getInstance();

        int hour=calendar.get(Calendar.HOUR);
        int minute=calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker=new TimePickerDialog(NewTask.this, R.style.DatePickerTheme,
                (timePicker1, hour1, minute1) -> setTime(hour1, minute1), hour, minute, true);
        timePicker.show();
    }

    private void setDate(int year, int month, int day){
        String yearStr=String.valueOf(year);
        String monthStr="";
        String dayStr="";
        if((month+1)<10)
            monthStr+="0";
        monthStr+=String.valueOf(month+1);
        if(day<10)
            dayStr+="0";
        dayStr+=String.valueOf(day);
        String finalStr=yearStr+"-"+monthStr+"-"+dayStr;
        dateInput.setText(finalStr);
    }

    private void setTime(int hour, int minute){
        String finalStr="";
        if(hour<10)
            finalStr+="0";
        finalStr+=String.valueOf(hour);
        finalStr+=":";
        if(minute<10)
            finalStr+="0";
        finalStr+=String.valueOf(minute);
        hourInput.setText(finalStr);
    }

    public void goToMain(View view){
        this.sqLiteDatabase.close();
        this.finish();
    }

    ActivityResultLauncher<Intent> sActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode()==Activity.RESULT_OK){
                    Intent data=result.getData();
                    Uri uri=data.getData();
                    this.uri=uri.toString();
                    source=DocumentFile.fromSingleUri(this, uri);
                    filename=source.getName();
                    attachment=true;
                    attachmentPath.setText("Dodano");

                }
            }
    );

    private void createDirectoryAndSaveFile() throws IOException {
        File destination=new File(this.getDir("attachments", MODE_PRIVATE).getAbsolutePath()+"/"+ this.taskId +"/"+filename);
        Files.copy(getContentResolver().openInputStream(source.getUri()), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void addAttachment(View view){
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        sActivityResultLauncher.launch(intent);
    }

    public void deleteAttachment(View view){
        attachmentPath.setText("Brak załącznika");
        if(attachment){
            Toast.makeText(this, "Usunięto załącznik", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Nie dodano żadnego załącznika", Toast.LENGTH_SHORT).show();
        }
        attachment=false;
    }
}
