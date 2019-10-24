package com.sagacity.docs.model.video;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="video_episode",pkName="id")
public class VideoEpisode extends Model<VideoEpisode> {
    public final static VideoEpisode dao = new VideoEpisode();
}
