package com.sagacity.docs.wxss.music;

import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.qiniu.http.Response;
import com.sagacity.docs.extend.ResponseCode;
import com.sagacity.docs.wxss.WXSSBaseController;

@ControllerBind(controllerKey = "/wxss/music", viewPath = "/wxss")
public class MusicController extends WXSSBaseController{

    @Override
    public void index(){

    }

    /**
     * 随机从音乐库中获取一首音乐
     */
    public void getBackMusic(){
        int doc_id = getParaToInt("doc_id");

        String sql = "select * from music where state=1 ORDER BY RAND() LIMIT 1";

        renderJson(ResponseCode.DATA, Db.findFirst(sql));
    }
}
