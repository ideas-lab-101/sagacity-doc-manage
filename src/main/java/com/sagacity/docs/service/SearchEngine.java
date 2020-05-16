package com.sagacity.docs.service;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import java.util.List;

public class SearchEngine {

    public final static SearchEngine dao = new SearchEngine();

    public List<Record> doSearch(String key){
        String sql = "select di.id,di.title,di.`desc`,di.cover,di.source,di.is_end\n" +
                ",dc.id doc_class_id,dc.title doc_class,u.UserID,u.Caption \n" +
                "from doc_info di\n" +
                "left join doc_class dc on dc.id=di.doc_class_id\n" +
                "left join sys_users u on u.UserID=di.user_id\n" +
                "where di.title like '%"+key+"%' or u.Caption like '%"+key+"%'";

        List<Record> rs = Db.find(sql);
        return rs;
    }
}
