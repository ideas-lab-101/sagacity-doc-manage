package com.sagacity.docs.doc;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="doc_page",pkName="id")
public class DocPage extends Model<DocPage> {

    public final static DocPage dao = new DocPage();

}
