package com.sagacity.docs.system;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="main",pkName="id")
public class MainInfo extends Model<MainInfo> {

    public final static MainInfo dao = new MainInfo();

}
