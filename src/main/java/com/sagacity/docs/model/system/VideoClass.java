package com.sagacity.docs.model.system;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="video_class",pkName="id")
public class VideoClass extends Model<VideoClass> {

    public final static VideoClass dao = new VideoClass();

}
