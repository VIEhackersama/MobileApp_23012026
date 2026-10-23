package com.example.studentmanager;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.dal.StudentRepository;
import com.example.studentmanager.models.Account;
import com.example.studentmanager.models.Score;
import com.example.studentmanager.models.Student;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RepositoryInstrumentedTest {
    private static final String TEST_DB = "repository_instrumentation.db";
    private Context context;
    private DBHelper helper;
    private StudentRepository repo;
    private Account teacher, otherTeacher, student;

    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(TEST_DB);
        helper = new DBHelper(context, TEST_DB);
        repo = new StudentRepository(helper);
        teacher = repo.login("teacher01", "teacher123");
        otherTeacher = repo.login("teacher02", "teacher123");
        student = repo.login("student01", "student123");
    }

    @After public void cleanup() { helper.close(); context.deleteDatabase(TEST_DB); }

    @Test public void seedAndLoginHaveExpectedRoles() {
        assertTrue(teacher.isTeacher());
        assertFalse(student.isTeacher());
        assertEquals("SV001", student.studentCode);
        assertEquals(teacher.id, repo.login("TEACHER01", "teacher123").id);
        assertEquals(3, repo.students(teacher.id).size());
        expect(IllegalArgumentException.class, () -> repo.login("teacher01", "bad"));
        expect(IllegalArgumentException.class, () -> repo.login("' OR 1=1 --", "bad"));
        expect(SecurityException.class, () -> repo.accountById(-1));
    }

    @Test public void roleAndOwnershipAreEnforcedAtRepository() {
        List<Score> mine = repo.scores(student.id);
        assertEquals(2, mine.size());
        for (Score score : mine) assertEquals("SV001", score.studentCode);
        assertEquals(3, repo.scores(teacher.id).size());
        Score foreign = repo.scores(otherTeacher.id).get(0);
        expect(SecurityException.class, () -> repo.deleteScore(teacher.id, foreign.id));
        expect(SecurityException.class, () -> repo.saveScore(teacher.id, foreign.id, "SV001", "Other", 9));
        expect(SecurityException.class, () -> repo.students(student.id));
        expect(SecurityException.class, () -> repo.createStudent(student.id, "NEW", "Name", "n@test.com", "new-user", "abc123"));
        expect(SecurityException.class, () -> repo.saveScore(student.id, 0, "SV001", "Other", 9));
        expect(SecurityException.class, () -> repo.deleteScore(student.id, mine.get(0).id));
        assertEquals(3, repo.scores(otherTeacher.id).size());
    }

    @Test public void studentCreationUpdateAndDeletionAreAtomic() {
        repo.createStudent(teacher.id, "sv004", "New Student", "new@test.com", "student04", "student123");
        Account created = repo.login("student04", "student123");
        assertEquals("SV004", created.studentCode);
        Student added = findStudent("SV004");
        repo.updateStudent(teacher.id, added.id, "Updated Name", "updated@test.com");
        assertEquals("Updated Name", repo.accountById(created.id).fullname);
        expect(SecurityException.class, () -> repo.updateStudent(student.id, added.id, "Hacked", "h@test.com"));
        expect(SecurityException.class, () -> repo.deleteStudent(student.id, added.id));
        repo.deleteStudent(teacher.id, added.id);
        expect(IllegalArgumentException.class, () -> repo.login("student04", "student123"));
        assertEquals(3, repo.students(teacher.id).size());
    }

    @Test public void duplicatesRollbackAccountAndLeaveNoOrphans() {
        expect(IllegalArgumentException.class, () -> repo.createStudent(teacher.id, "sv001", "Duplicate", "d@test.com", "orphan", "abc123"));
        expect(IllegalArgumentException.class, () -> repo.login("orphan", "abc123"));
        expect(IllegalArgumentException.class, () -> repo.createStudent(teacher.id, "SV004", "Duplicate", "d@test.com", "STUDENT01", "abc123"));
        assertEquals(3, repo.students(teacher.id).size());
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM Account", null)) {
            assertTrue(c.moveToFirst()); assertEquals(5, c.getInt(0));
        }
    }

    @Test public void studentsWithScoresCannotBeDeleted() {
        Student existing = findStudent("SV001");
        expect(IllegalArgumentException.class, () -> repo.deleteStudent(teacher.id, existing.id));
        assertEquals(2, repo.scores(student.id).size());
        assertEquals(student.id, repo.login("student01", "student123").id);
    }

    @Test public void scoreCrudBoundariesAndDuplicateRules() {
        repo.saveScore(teacher.id, 0, "SV001", "Boundary", 0);
        Score added = findScore("Boundary");
        repo.saveScore(teacher.id, added.id, "SV001", "Boundary", 10);
        assertEquals(10, findScore("Boundary").score, 0.0001);
        expect(IllegalArgumentException.class, () -> repo.saveScore(teacher.id, 0, "SV001", "boundary", 7));
        for (double bad : new double[]{-1, 10.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            expect(IllegalArgumentException.class, () -> repo.saveScore(teacher.id, 0, "SV001", "Invalid", bad));
        }
        expect(IllegalArgumentException.class, () -> repo.saveScore(teacher.id, 0, "MISSING", "Subject", 7));
        expect(IllegalArgumentException.class, () -> repo.saveScore(teacher.id, 0, "SV001", "  ", 7));
        repo.deleteScore(teacher.id, added.id);
        assertEquals(3, repo.scores(teacher.id).size());
    }

    @Test public void dataPersistsWithoutReseedingOnReopen() {
        repo.createStudent(teacher.id, "SV005", "Persisted", "p@test.com", "persisted", "abc123");
        helper.close();
        helper = new DBHelper(context, TEST_DB);
        repo = new StudentRepository(helper);
        assertEquals(4, repo.students(teacher.id).size());
        assertEquals("SV005", repo.login("persisted", "abc123").studentCode);
        assertEquals(3, repo.scores(teacher.id).size());
        try (Cursor c = helper.getReadableDatabase().rawQuery("PRAGMA foreign_key_check", null)) {
            assertFalse(c.moveToFirst());
        }
    }

    @Test public void passwordsAreNotStoredInPlaintext() {
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT password FROM Account WHERE id=?",
                new String[]{Long.toString(teacher.id)})) {
            assertTrue(c.moveToFirst());
            assertNotEquals("teacher123", c.getString(0));
            assertTrue(c.getString(0).startsWith("120000:"));
        }
    }

    @Test public void invalidStudentInputDoesNotChangeDatabase() {
        expect(IllegalArgumentException.class, () -> repo.createStudent(teacher.id, "SV006", "Name", "bad-email", "validname", "abc123"));
        expect(IllegalArgumentException.class, () -> repo.createStudent(teacher.id, "SV006", "Name", "a@test.com", "space user", "abc123"));
        expect(IllegalArgumentException.class, () -> repo.createStudent(teacher.id, "SV006", "Name", "a@test.com", "validname", "123"));
        expect(IllegalArgumentException.class, () -> repo.createStudent(teacher.id, "  ", "Name", "a@test.com", "validname", "abc123"));
        assertEquals(3, repo.students(teacher.id).size());
    }

    private Student findStudent(String code) {
        for (Student s : repo.students(teacher.id)) if (s.code.equals(code)) return s;
        throw new AssertionError("Student not found: " + code);
    }
    private Score findScore(String subject) {
        for (Score s : repo.scores(teacher.id)) if (s.subject.equals(subject)) return s;
        throw new AssertionError("Score not found: " + subject);
    }
    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try { operation.run(); }
        catch (Throwable error) { if (type.isInstance(error)) return; throw new AssertionError("Wrong exception", error); }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
