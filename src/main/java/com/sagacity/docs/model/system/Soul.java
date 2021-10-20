package com.sagacity.docs.model.system;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="soul",pkName="id")
public class Soul extends Model<Soul> {

    public final static Soul dao = new Soul();

}
