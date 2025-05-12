package com.orange.obd.test.utils

import android.content.Context
import android.util.Log

class SqlController(context: Context) {
    val db = SqlDb(context)

    fun createAndCheckTable() {
        db.exec("""
            CREATE TABLE IF NOT EXISTS `AddCommand` (
              `id`   INTEGER PRIMARY KEY AUTOINCREMENT,
              `command` TEXT NOT NULL
            );
        """.trimIndent())
    }

    // 新增添加指令
    fun addCommand(cmd: String) {
        val sql = """
            INSERT INTO `AddCommand`
            (`command`)
            VALUES
            ('${cmd.replace("0x", "")}')
        """.trimIndent()
        Log.e(".sql", "$sql")
        db.exec(sql)
    }

    // 刪除添加指令
    fun deleteAddCommand(id: String) {
        val sql = """
            DELETE FROM `AddCommand`
            WHERE `id` = '${id}'
        """.trimIndent()
        Log.e(".sql", "$sql")
        db.exec(sql)
    }

    // 取得添加指令
    fun getAddCommand(): ArrayList<MutableMap<String, Any>> {
        val sql = """
            SELECT * FROM `AddCommand` ORDER BY ID DESC;
        """.trimIndent()
        val rs = db.query(sql)
        Log.e(".sql", "$sql, $rs")
        return rs
    }
}