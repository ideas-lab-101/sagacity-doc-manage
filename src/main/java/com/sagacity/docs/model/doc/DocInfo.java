package com.sagacity.docs.model.doc;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

@TableBind(tableName="doc_info",pkName="id")
public class DocInfo extends Model<DocInfo> {

    public final static DocInfo dao = new DocInfo();

    public Record getSource(String sourceType, int sourceID){

        if(sourceType.equals("doc")){
            return Db.findFirst("select di.*,0 page_id from doc_info di where di.id=?", sourceID);
        }else if(sourceType.equals("page")){
            Record sourceInfo = Db.findFirst("select CONCAT(di.title,\"-\",dp.title) title,dp.id page_id,di.id,di.desc,di.cover\n" +
                    "from doc_page dp\n" +
                    "left join doc_info di on di.id=dp.doc_id\n" +
                    "where dp.id=?", sourceID);
            return sourceInfo;
        }
        return null;
    }

    public int getViewCount(int doc_id){
        return Db.findFirst("select IFNULL(sum(dc.view_count),0) view_count\n" +
                "from doc_page dc\n" +
                "where dc.doc_id=?", doc_id).getInt("view_count");
    }

}
