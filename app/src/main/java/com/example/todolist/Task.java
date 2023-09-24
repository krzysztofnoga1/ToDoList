package com.example.todolist;

public class Task {
    int id;
    String title;
    String description;
    String category;
    String date;
    String hour;
    int done;
    int notification;
    int attachment;

    Task(int id,String title, String description, String category, String date, String time,int notification, int attachment, int done){
        this.id=id;
        this.title=title;
        this.description=description;
        this.category=category;
        this.date=date;
        this.hour=time;
        this.attachment=attachment;
        this.done=done;
        this.notification=notification;
    }
}
