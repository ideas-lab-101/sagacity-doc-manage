package com.sagacity.docs.model.system;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="music",pkName="id")
public class Music extends Model<Music> {

    public final static Music dao = new Music();

}
