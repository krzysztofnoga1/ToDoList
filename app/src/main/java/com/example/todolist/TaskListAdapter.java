package com.example.todolist;

import static android.content.Context.MODE_PRIVATE;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;

public class TaskListAdapter extends RecyclerView.Adapter<AdapterItem> {
    SQLiteDatabase database;
    ArrayList<Task> taskList;
    AlarmManager alarmManager;
    public Context context;

    public TaskListAdapter(ArrayList<Task> taskList, Context context, SQLiteDatabase database, AlarmManager alarmManager){
        this.database=database;
        this.taskList=taskList;
        this.context=context;
        this.alarmManager=alarmManager;
    }

    @NonNull
    @Override
    public AdapterItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.task, parent, false);
        return new AdapterItem(view).linkAdapter(this);
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterItem holder, int position) {
        holder.checkBox.setText(taskList.get(position).title);
        holder.taskId=(taskList.get(position).id);
        holder.taskTitle=(taskList.get(position).title);
        if(taskList.get(position).attachment==1){
            holder.attachment.setImageResource(R.drawable.baseline_attachment_24_green);
        }
        else{
            holder.attachment.setImageResource(R.drawable.baseline_attachment_24_gray);
        }
        holder.checkBox.setChecked(taskList.get(position).done == 1);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }
}

class AdapterItem extends RecyclerView.ViewHolder{
    int taskId;
    String taskTitle;
    CheckBox checkBox;
    ImageButton deleteButton;
    ImageButton editButton;
    ImageView attachment;
    private TaskListAdapter taskListAdapter;

    public AdapterItem(@NonNull View itemView){
        super(itemView);
        checkBox=itemView.findViewById(R.id.task_check_box);
        deleteButton=itemView.findViewById(R.id.delete_btn);
        editButton=itemView.findViewById(R.id.edit_btn);
        deleteButton.setOnClickListener(view -> deleteTask());
        editButton.setOnClickListener(view -> goToEditTask());
        checkBox.setOnClickListener(view->changeTaskStatus());
        attachment=itemView.findViewById(R.id.attachment_set);
    }

    public AdapterItem linkAdapter(TaskListAdapter taskListAdapter){
        this.taskListAdapter=taskListAdapter;
        return this;
    }

    public void goToEditTask(){
        Intent intent=new Intent(taskListAdapter.context, EditTask.class);
        Bundle bundle=new Bundle();
        bundle.putInt("id", this.taskId);
        intent.putExtras(bundle);
        taskListAdapter.context.startActivity(intent);
    }

    public void deleteTask(){
        cancelNotification();
        taskListAdapter.taskList.remove(this.getAdapterPosition());
        taskListAdapter.notifyItemRemoved(this.getAdapterPosition());
        deleteFromDatabase();
        File attachment=new File(taskListAdapter.context.getDir("attachments", MODE_PRIVATE).getAbsolutePath()+"/"+ this.taskId);
        if(attachment.isDirectory()){
            for(File content:attachment.listFiles()){
                content.delete();
            }
            attachment.delete();
        }
        NotificationManagerCompat.from(taskListAdapter.context).cancel(taskId);
    }

    private void deleteFromDatabase(){
        String sql="DELETE FROM taskslist WHERE id="+this.taskId;
        taskListAdapter.database.execSQL(sql);
    }

    private void changeTaskStatus(){
        if(taskListAdapter.taskList.get(getAdapterPosition()).done==1){
            taskListAdapter.taskList.get(getAdapterPosition()).done=0;
            changeTaskStatusInDatabase(0);
            Toast.makeText(taskListAdapter.context, "Zmienono status zadania na niezakończone", Toast.LENGTH_SHORT).show();
        }
        else{
            taskListAdapter.taskList.get(getAdapterPosition()).done=1;
            changeTaskStatusInDatabase(1);
            Toast.makeText(taskListAdapter.context, "Zmieniono status zadania na zakończone", Toast.LENGTH_SHORT).show();
        }
        taskListAdapter.notifyItemChanged(getAdapterPosition());
    }

    private void changeTaskStatusInDatabase(int status){
        String sql="UPDATE taskslist SET done='";
        sql+=status;
        sql+="' WHERE id=";
        sql+=this.taskId;
        taskListAdapter.database.execSQL(sql);
    }

    private void cancelNotification(){
        Intent intent=new Intent(taskListAdapter.context, NotificationBroadcast.class);
        intent.putExtra("task", this.taskTitle);
        intent.putExtra("id", this.taskId);
        PendingIntent pendingIntent=PendingIntent.getBroadcast(taskListAdapter.context, this.taskId, intent, PendingIntent.FLAG_IMMUTABLE);
        taskListAdapter.alarmManager.cancel(pendingIntent);
    }
}
