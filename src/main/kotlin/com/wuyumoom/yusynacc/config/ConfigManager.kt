package com.wuyumoom.yusynacc.config

import com.wuyumoom.yucore.api.Message
import com.wuyumoom.yusynacc.YuSynAcc

object ConfigManager {
    var config = YuSynAcc.INSTANCE.config
    var message: Message = Message(config)
    var mysql_host = config.getString("Storage.mysql.host")?:"localhost"
    var mysql_prot = config.getString("Storage.mysql.prot")?:"3306"
    var mysql_database = config.getString("Storage.mysql.database")?:"yusynacc"
    var mysql_username = config.getString("Storage.mysql.username")?:"username"
    var mysql_password = config.getString("Storage.mysql.password")?:"password"

    // 加载
    fun load() {
        message = Message(config)
        mysql_host = config.getString("Storage.mysql.host")?:"localhost"
        mysql_prot = config.getString("Storage.mysql.prot")?: "3306"
        mysql_database = config.getString("Storage.mysql.database")?:"yusynacc"
        mysql_username = config.getString("Storage.mysql.username")?:"username"
        mysql_password = config.getString("Storage.mysql.password")?:"password"
    }
    /** 重载 */
    fun reload() {
        YuSynAcc.INSTANCE.reloadConfig()
        config = YuSynAcc.INSTANCE.config
    }
}
