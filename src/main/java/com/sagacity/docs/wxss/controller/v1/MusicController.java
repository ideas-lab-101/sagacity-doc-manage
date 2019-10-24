package com.sagacity.docs.wxss.controller.v1;

import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.wxss.common.WXSSBaseController;

@ControllerBind(controllerKey = "/wxss/music", viewPath = "/wxss")
public class MusicController extends WXSSBaseController{

    @Override
    public void index(){

    }

    /**
     * 随机从音乐库中获取一首音乐
     */
    public void getBackMusic(){
        int page_id = getParaToInt("page_id", 0);

        String sql = "select m.* from music m left join doc_page dp on dp.music_id=m.id where dp.id=?";
        if(Db.findFirst(sql, page_id) != null){
            renderJson(ResponseCode.DATA, Db.findFirst(sql, page_id));
        }else{
            sql = "select * from music where state=1 ORDER BY RAND() LIMIT 1";
            renderJson(ResponseCode.DATA, Db.findFirst(sql));
        }

    }

    public void getPageMusic(){
        int page_id = getParaToInt("page_id", 0);

        String sql = "select m.* from music m left join doc_page dp on dp.music_id=m.id where dp.id=?";
        renderJson(ResponseCode.DATA, Db.findFirst(sql, page_id));

    }
}
