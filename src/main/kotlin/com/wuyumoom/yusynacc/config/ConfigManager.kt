package com.wuyumoom.yusynacc.config

import com.wuyumoom.yusynacc.YuSynAcc
import com.wuyumoom.yucore.api.Message
import org.yaml.snakeyaml.error.YAMLException



object ConfigManager {
    var config = YuSynAcc.INSTANCE.config
    var message: Message= Message(config)
    var mysql_host = config.getString("Storage.mysql.host")
    var mysql_prot = config.getString("Storage.mysql.prot")
    var mysql_database = config.getString("Storage.mysql.database")
    var mysql_username = config.getString("Storage.mysql.username")
    var mysql_password = config.getString("Storage.mysql.password")


    //加载
    fun load(){
    message = Message(config)
    mysql_host = config.getString("Storage.mysql.host")
    mysql_prot = config.getString("Storage.mysql.prot")
    mysql_database = config.getString("Storage.mysql.database")
    mysql_username = config.getString("Storage.mysql.username")
    mysql_password = config.getString("Storage.mysql.password")

    }
    /**
     * 重载
     *
    **/
    fun reload(){
      YuSynAcc.INSTANCE.reloadConfig()
      config = YuSynAcc.INSTANCE.config
    }

}
