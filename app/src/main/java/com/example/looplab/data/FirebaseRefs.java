package com.example.looplab.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public final class FirebaseRefs {
    private FirebaseRefs() {}

    public static FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    public static FirebaseDatabase rtdb() {
        return FirebaseDatabase.getInstance();
    }

    public static FirebaseStorage storage() {
        return FirebaseStorage.getInstance();
    }

    public static CollectionReference users() {
        return db().collection("users");
    }

    public static CollectionReference courses() {
        return db().collection("courses");
    }

    public static CollectionReference announcements() {
        return db().collection("announcements");
    }

    public static CollectionReference events() {
        return db().collection("events");
    }

    public static CollectionReference lectures() {
        return db().collection("lectures");
    }

    public static CollectionReference enrollments() {
        return db().collection("enrollments");
    }

    public static CollectionReference progress() {
        return db().collection("progress");
    }

    public static CollectionReference badges() {
        return db().collection("badges");
    }

    public static CollectionReference leaderboard() {
        return db().collection("leaderboard");
    }

    public static CollectionReference chats() {
        return db().collection("chats");
    }

    public static CollectionReference messages() {
        return db().collection("messages");
    }

    public static CollectionReference team() {
        return db().collection("team");
    }

    public static CollectionReference feedback() {
        return db().collection("feedback");
    }

    public static CollectionReference analytics() {
        return db().collection("analytics");
    }

    public static CollectionReference liveSessions() {
        return db().collection("liveSessions");
    }

    public static CollectionReference conversations() {
        return db().collection("conversations");
    }
}


