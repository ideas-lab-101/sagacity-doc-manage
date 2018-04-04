package com.sagacity.docs.system;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="doc_class",pkName="id")
public class DocClass extends Model<DocClass> {

    public final static DocClass dao = new DocClass();

}
