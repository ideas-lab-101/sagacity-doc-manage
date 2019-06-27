package com.sagacity.docs.video;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="video_info",pkName="id")
public class VideoInfo extends Model<VideoInfo> {

    public final static VideoInfo dao = new VideoInfo();

}
