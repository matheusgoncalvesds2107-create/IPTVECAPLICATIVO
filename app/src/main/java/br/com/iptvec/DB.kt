package br.com.iptvec

import android.content.Context
import android.database.sqlite.SQLiteDatabase

class DB(ctx: Context): android.database.sqlite.SQLiteOpenHelper(ctx, "iptvec.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,grp TEXT,logo TEXT,url TEXT)")
        db.execSQL("CREATE INDEX idx_grp ON items(grp)")
        db.execSQL("CREATE INDEX idx_name ON items(name)")
        db.execSQL("CREATE INDEX idx_grp_name ON items(grp,name)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun clear(){ writableDatabase.execSQL("DELETE FROM items") }
    fun begin(){ writableDatabase.beginTransaction() }
    fun commit(){ if (writableDatabase.inTransaction()) { writableDatabase.setTransactionSuccessful(); writableDatabase.endTransaction() } }
    fun insert(n:String,g:String,l:String,u:String){
        writableDatabase.execSQL("INSERT INTO items(name,grp,logo,url) VALUES(?,?,?,?)", arrayOf(n,g,l,u))
    }
    fun categories(): List<Pair<String,Int>> {
        val out= mutableListOf<Pair<String,Int>>()
        readableDatabase.rawQuery("SELECT grp,COUNT(*) FROM items GROUP BY grp ORDER BY grp COLLATE NOCASE",null).use {
            while(it.moveToNext()) out += it.getString(0) to it.getInt(1)
        }
        return out
    }
    fun page(group:String, query:String, offset:Int, limit:Int): List<Array<String>> {
        val out=mutableListOf<Array<String>>()
        val args=mutableListOf<String>()
        var sql="SELECT name,logo,url FROM items WHERE grp=?"
        args += group
        if(query.isNotBlank()){ sql+=" AND name LIKE ?"; args += "%$query%" }
        sql+=" ORDER BY name COLLATE NOCASE LIMIT ? OFFSET ?"
        args += limit.toString(); args += offset.toString()
        readableDatabase.rawQuery(sql,args.toTypedArray()).use{
            while(it.moveToNext()) out += arrayOf(it.getString(0),it.getString(1),it.getString(2))
        }
        return out
    }
}
