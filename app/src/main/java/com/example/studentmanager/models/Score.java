package com.example.studentmanager.models;

public final class Score {
    public final long id;
    public final String studentCode;
    // The assignment calls this teacherCode, but its value references Teacher.id.
    public final long teacherCode;
    public final String subject;
    public final double score;
    public final String studentName;
    public final String teacherName;

    public Score(long id, String studentCode, long teacherCode, String subject, double score,
                 String studentName, String teacherName) {
        this.id = id;
        this.studentCode = studentCode;
        this.teacherCode = teacherCode;
        this.subject = subject;
        this.score = score;
        this.studentName = studentName;
        this.teacherName = teacherName;
    }
}
