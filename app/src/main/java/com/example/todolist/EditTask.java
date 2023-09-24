package com.example.todolist;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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

public class EditTask extends AppCompatActivity {
    private int taskId;
    private DocumentFile source;
    private String filename;
    private String uri;
    private TextView attachmentPath;
    private ImageView attachmentImg;
    private SQLiteDatabase sqLiteDatabase;
    private Cursor result;
    private ImageButton backButton;
    private EditText taskName;
    private EditText taskDescription;
    private EditText taskCategory;
    private EditText taskDate;
    private EditText taskHour;
    private CheckBox notifications;
    private Button deleteAttachmentBtn;
    private Button editAttachmentBtn;
    private Task currentTask;
    private boolean deleteAttachment;
    private boolean editAttachment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskId=getIntent().getExtras().getInt("id");
        setContentView(R.layout.activity_edit_task);
        backButton=findViewById(R.id.edit_back_btn);
        taskName=findViewById(R.id.edit_task_name);
        taskDescription=findViewById(R.id.edit_task_description);
        taskCategory=findViewById(R.id.edit_task_category);
        taskDate=findViewById(R.id.edit_task_date);
        taskHour=findViewById(R.id.edit_task_hour);
        notifications=findViewById(R.id.edit_notifications_on);
        attachmentPath=findViewById(R.id.attachment_path);
        attachmentImg=findViewById(R.id.attachment);
        sqLiteDatabase=openOrCreateDatabase("tasks", MODE_PRIVATE, null);
        deleteAttachment=false;
        editAttachment=false;
        getTaskFromDatabase();
        setFields();
        if(attachmentPath.getText().toString().equals("Brak załącznika")){
            attachmentImg.setImageResource(R.drawable.baseline_broken_image_100);
        }else{
            attachmentImg.setImageDrawable(Drawable.createFromPath(attachmentPath.getText().toString()));
        }
        notifications.setOnClickListener(view -> changeTaskNotificationStatus());
        attachmentPath.setOnClickListener(view-> showAttachmentOptions());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("taskId", this.taskId);
        savedInstanceState.putString("uri", this.uri);
        savedInstanceState.putString("filename", this.filename);
        savedInstanceState.putString("attachmentPath", this.attachmentPath.getText().toString());
        savedInstanceState.putString("taskName", this.taskName.getText().toString());
        savedInstanceState.putString("taskDescription", this.taskDescription.getText().toString());
        savedInstanceState.putString("taskCategory", this.taskCategory.getText().toString());
        savedInstanceState.putString("taskDate", this.taskDate.getText().toString());
        savedInstanceState.putString("taskHour", this.taskHour.getText().toString());
        savedInstanceState.putBoolean("deleteAttachment", this.deleteAttachment);
        savedInstanceState.putBoolean("editAttachment", this.editAttachment);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceBundle) {
        super.onRestoreInstanceState(savedInstanceBundle);
        this.uri=savedInstanceBundle.getString("uri");
        this.deleteAttachment=savedInstanceBundle.getBoolean("deleteAttachment");
        this.editAttachment=savedInstanceBundle.getBoolean("editAttachment");
        if(editAttachment){
            Uri newUri=Uri.parse(this.uri);
            attachmentImg.setImageURI(newUri);
            this.source=DocumentFile.fromSingleUri(this, newUri);
        }
        if(deleteAttachment){
            attachmentImg.setImageResource(R.drawable.baseline_broken_image_100);
        }
        this.taskId=savedInstanceBundle.getInt("taskId");
        this.filename=savedInstanceBundle.getString("filename");
        this.attachmentPath.setText(savedInstanceBundle.getString("attachmentPath"));
        this.taskName.setText(savedInstanceBundle.getString("taskName"));
        this.taskDescription.setText(savedInstanceBundle.getString("taskDescription"));
        this.taskCategory.setText(savedInstanceBundle.getString("taskCategory"));
        this.taskDate.setText(savedInstanceBundle.getString("taskDate"));
        this.taskHour.setText(savedInstanceBundle.getString("taskHour"));
    }

    public void goBackToMain(View view){
        sqLiteDatabase.close();
        this.finish();
    }

    private void getTaskFromDatabase(){
        String sql="SELECT * FROM taskslist WHERE id="+this.taskId;
        this.result=sqLiteDatabase.rawQuery(sql, null);

        this.result.moveToFirst();
        currentTask=new Task(result.getInt(0), result.getString(1), result.getString(2),
                result.getString(3), result.getString(4), result.getString(5), result.getInt(6),
                result.getInt(7), result.getInt(8));
    }

    private void setFields(){
        taskName.setText(currentTask.title);
        taskDescription.setText(currentTask.description);
        taskCategory.setText(currentTask.category);
        taskDate.setText(currentTask.date);
        taskHour.setText(currentTask.hour);
        if(currentTask.notification==1){
            notifications.setChecked(true);
        }
        getTaskPath();
    }

    private String buildSQL(){
        String sql="UPDATE taskslist SET id=";
        sql+=this.taskId;
        sql+=", task='";
        sql+=taskName.getText().toString();
        sql+="', description='";
        sql+=taskDescription.getText().toString();
        sql+="', category='";
        sql+=taskCategory.getText().toString();
        sql+="', date='";
        sql+=taskDate.getText().toString();
        sql+="', time='";
        sql+=taskHour.getText().toString();
        sql+="', notifications=";
        if(notifications.isChecked())
            sql+="'1'";
        else
            sql+="'0'";
        sql+=", attachment=";
        if(attachmentPath.getText().toString().equals("Brak załącznika"))
            sql+="'0'";
        else
            sql+="'1'";
        sql+=" WHERE id=";
        sql+=this.taskId;
        return sql;
    }

    public void chooseDate(View view){
        final Calendar calendar=Calendar.getInstance();

        int year=calendar.get(Calendar.YEAR);
        int month=calendar.get(Calendar.MONTH);
        int day=calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog=new DatePickerDialog(EditTask.this, R.style.DatePickerTheme,
                (datePicker, year1, month1, day1) -> setDate(year1,month1,day1),year,month,day);
        datePickerDialog.show();
    }

    public void chooseTime(View view){
        final Calendar calendar=Calendar.getInstance();

        int hour=calendar.get(Calendar.HOUR);
        int minute=calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker=new TimePickerDialog(EditTask.this, R.style.DatePickerTheme,
                (timePicker1, hour1, minute1) -> setTime(hour1, minute1), hour, minute, true);
        timePicker.show();
    }

    private void getTaskPath(){
        File attachmentDir=new File(this.getDir("attachments", MODE_PRIVATE).getAbsolutePath()+"/"+ this.taskId);
        if(attachmentDir.isDirectory()){
            File[] attachments=attachmentDir.listFiles();
            if(attachments!=null && attachments.length>0){
                File attachment=attachments[0];
                attachmentPath.setText(attachment.getAbsolutePath());
            }
            else{
                attachmentPath.setText("Brak załącznika");
            }
        }
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
        taskDate.setText(finalStr);
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
        taskHour.setText(finalStr);
    }

    public void editTask(View view){
        if(taskName.getText().toString().equals("") || taskDate.getText().toString().equals("") || taskHour.getText().toString().equals("")){
            Toast.makeText(this, "Nazwa zadania, data i godzina nie mogą być puste", Toast.LENGTH_SHORT).show();
        }else{
            String sql=buildSQL();
            sqLiteDatabase.execSQL(sql);
            if(editAttachment){
                createDirectoryAndSaveFile();
            }
            if(deleteAttachment){
                File attachmentDir=new File(this.getDir("attachments", MODE_PRIVATE).getAbsolutePath()+"/"+ this.taskId);
                if(attachmentDir.isDirectory()){
                    File[] attachments=attachmentDir.listFiles();
                    if(attachments!=null && attachments.length>0){
                        File attachment=attachments[0];
                        attachment.delete();
                    }
                }
            }
            this.finish();
        }
    }

    private void createDirectoryAndSaveFile() {
        try{
            File attachmentDir=new File(this.getDir("attachments", MODE_PRIVATE).getAbsolutePath()+"/"+ this.taskId);
            if(attachmentDir.isDirectory()){
                File[] attachments=attachmentDir.listFiles();
                if(attachments!=null && attachments.length>0){
                    File attachment=attachments[0];
                    attachment.delete();
                }
            }
            File destination=new File(this.getDir("attachments", MODE_PRIVATE).getAbsolutePath()+"/"+ this.taskId +"/"+filename);
            Files.copy(getContentResolver().openInputStream(source.getUri()), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void changeTaskNotificationStatus(){
        if(currentTask.notification==1){
            currentTask.notification=0;
            notifications.setChecked(false);
            Toast.makeText(this, "Wyłączono powiadomienia dla zadania", Toast.LENGTH_SHORT).show();
        }
        else{
            currentTask.notification=1;
            notifications.setChecked(true);
            Toast.makeText(this, "Włączono powiadomienia dla zadania", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAttachmentOptions(){
        Dialog dialog=new Dialog(this);
        dialog.setContentView(R.layout.attachment_popup);
        deleteAttachmentBtn=dialog.findViewById(R.id.delete_attachment_popup);
        editAttachmentBtn=dialog.findViewById(R.id.edit_attachment_popup);
        deleteAttachmentBtn.setOnClickListener(view ->{deleteAttachmentClick();});
        editAttachmentBtn.setOnClickListener(view->{addAttachment();});
        dialog.show();
    }

    private void deleteAttachmentClick(){
        attachmentImg.setImageResource(R.drawable.baseline_broken_image_100);
        attachmentPath.setText("Brak załącznika");
        deleteAttachment=true;
    }

    ActivityResultLauncher<Intent> sActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode()== Activity.RESULT_OK){
                    Intent data=result.getData();
                    Uri uri=data.getData();
                    this.uri=uri.toString();
                    source= DocumentFile.fromSingleUri(this, uri);
                    filename=source.getName();
                    String newPath=this.getDir("attachments", MODE_PRIVATE).getAbsolutePath()+"/"+this.taskId+"/"+filename;
                    attachmentPath.setText(newPath);
                    attachmentImg.setImageURI(uri);
                    deleteAttachment=false;
                    editAttachment=true;
                }
            }
    );

    private void addAttachment(){
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        sActivityResultLauncher.launch(intent);
    }
}
