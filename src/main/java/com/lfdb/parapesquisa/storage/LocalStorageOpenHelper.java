package com.lfdb.parapesquisa.storage;

import android.database.sqlite.*;

/**
 * Created by Igor on 8/18/13.
 */
public class LocalStorageOpenHelper extends SQLiteOpenHelper {

    LocalStorageOpenHelper(android.content.Context ctx) {
        super(ctx, "uppsclient", null, 10);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE submissions (id INTEGER, form_id INTEGER, started_at BIGINT(20), state INTEGER, `data` TEXT, reschedule_date BIGINT(20), stop_reason INTEGER, page_index INTEGER, updated_at BIGINT(20), etag TEXT);");
        db.execSQL("CREATE TABLE submissions_reviewdata (submission_id INTEGER, field_name TEXT, message TEXT, user_id INTEGER, date BIGINT(20));");
        db.execSQL("CREATE TABLE submissions_log (id INTEGER, submission_id INTEGER, action TEXT, user_id INTEGER, reason_id INTEGER, date BIGINT(20));");
        db.execSQL("CREATE TABLE submissions_alternatives (submission_id INTEGER, id INTEGER, `data` TEXT, etag TEXT);");

        db.execSQL("CREATE TABLE forms (id INTEGER, title TEXT, description TEXT, start_time BIGINT(20), end_time BIGINT(20), quota INTEGER, max_reschedules INTEGER, users TEXT, allow_transfer INTEGER, allow_new_submissions INTEGER, undefined_mode INTEGER, assignment_id INTEGER, updated_at BIGINT(20), etag TEXT);");
        db.execSQL("CREATE TABLE forms_sections (id INTEGER, form_id INTEGER, name TEXT, order_id INTEGER);");
        db.execSQL("CREATE TABLE forms_fields (id INTEGER, form_id INTEGER, section_id INTEGER, `rawdata` TEXT);");
        db.execSQL("CREATE TABLE forms_stopreasons (form_id INTEGER, id INTEGER, reason TEXT, reschedule INTEGER);");
        db.execSQL("CREATE TABLE forms_quotas (form_id INTEGER, user_id INTEGER, quota INTEGER);");


        db.execSQL("CREATE TABLE session (server TEXT, token TEXT, user_id INTEGER);");
        db.execSQL("CREATE TABLE users (id INTEGER, name TEXT, username TEXT, role TEXT, created BIGINT(20), avatar_url TEXT, etag TEXT);");
        db.execSQL("CREATE TABLE notifications (title TEXT, description TEXT, date BIGINT(20), read INTEGER);");

        db.execSQL("CREATE TABLE sync_queue (action INTEGER, resource_id INTEGER, moderate_action TEXT, update_newstate INTEGER, created_at BIGINT(20));");

        db.execSQL("CREATE TABLE appdata (title_line_1 TEXT, title_line_2 TEXT, logo BLOB);");


        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS  id ON submissions (id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS submission_id ON submissions_reviewdata (submission_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS submission_id ON submissions_log (submission_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS submission_id ON submissions_alternatives (submission_id)");

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS id ON forms (id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS form_id ON forms_sections (form_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS form_id ON forms_fields (form_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS form_id ON forms_stopreasons (form_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS form_id ON forms_quotas (form_id)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS id ON submissions_log (id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int currVersion, int newVersion) {
        if(currVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE submissions ADD COLUMN page_index INTEGER;");
        }
        if(currVersion < 3 && newVersion >= 3) {
            db.execSQL("ALTER TABLE sync_queue ADD COLUMN moderate_action TEXT;");
            db.execSQL("ALTER TABLE sync_queue ADD COLUMN update_newstate INTEGER;");
        }
        if(currVersion < 4 && newVersion >= 4) {
            db.execSQL("CREATE TABLE forms_quotas (form_id INTEGER, user_id INTEGER, quota INTEGER);");
        }
        if(currVersion < 5 && newVersion >= 5) {
            db.execSQL("ALTER TABLE forms ADD COLUMN allow_transfer INTEGER");
            db.execSQL("ALTER TABLE forms ADD COLUMN allow_new_submissions INTEGER");
            db.execSQL("ALTER TABLE sync_queue ADD COLUMN created_at BIGINT(20)");
        }
        if(currVersion < 6 && newVersion >= 6) {
            db.execSQL("ALTER TABLE forms ADD COLUMN undefined_mode INTEGER");
        }
        if(currVersion < 7 && newVersion >= 7) {
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS  id ON submissions (id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS submission_id ON submissions_reviewdata (submission_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS submission_id ON submissions_log (submission_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS submission_id ON submissions_alternatives (submission_id)");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS id ON forms (id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS form_id ON forms_sections (form_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS form_id ON forms_fields (form_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS form_id ON forms_stopreasons (form_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS form_id ON forms_quotas (form_id)");
        }
        if(currVersion < 8 && newVersion >= 8) {
            db.execSQL("ALTER TABLE submissions_log ADD COLUMN id BIGINT(20)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS id ON submissions_log (id)");
        }
        if(currVersion < 9 && newVersion >= 9) {
            db.execSQL("ALTER TABLE forms ADD COLUMN assignment_id BIGINT(20)");
        }
        if(currVersion < 10 && newVersion >= 10) {
            db.execSQL("ALTER TABLE submissions ADD COLUMN updated_at BIGINT(20)");
            db.execSQL("ALTER TABLE forms ADD COLUMN updated_at BIGINT(20)");
        }
    }

    public void clear(SQLiteDatabase db) {
        db.execSQL("DELETE FROM submissions");
        db.execSQL("DELETE FROM submissions_reviewdata");
        db.execSQL("DELETE FROM submissions_log");

        db.execSQL("DELETE FROM forms");
        db.execSQL("DELETE FROM forms_sections");
        db.execSQL("DELETE FROM forms_fields");
        db.execSQL("DELETE FROM forms_stopreasons");
        db.execSQL("DELETE FROM forms_quotas");

        db.execSQL("DELETE FROM session");
        db.execSQL("DELETE FROM users");
        db.execSQL("DELETE FROM notifications");

        db.execSQL("DELETE FROM sync_queue");
        db.execSQL("DELETE FROM appdata");
    }
}
