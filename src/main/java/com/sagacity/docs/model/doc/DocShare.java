package com.sagacity.docs.model.doc;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="doc_share",pkName="id")
public class DocShare extends Model<DocShare> {

    public final static DocShare dao = new DocShare();

}
