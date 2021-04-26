package com.sagacity.docs.wxapp.controller.v1;

import com.jfinal.aop.Clear;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.wxapp.common.WXSSBaseController;
import com.sagacity.docs.wxapp.common.WXSSLoginInterceptor;

@ControllerBind(controllerKey = "/wxss/music", viewPath = "/wxss")
public class MusicController extends WXSSBaseController{

    @Override
    public void index(){

    }

    /**
     * 随机从音乐库中获取一首音乐
     */
    @Clear(WXSSLoginInterceptor.class)
    public void getBackMusic(){
        int pageId = getParaToInt("pageId", 0);

        String sql = "select m.* from music m left join doc_page dp on dp.music_id=m.id where dp.id=?";
        if(Db.findFirst(sql, pageId) != null){
            rest.success().setData(Db.findFirst(sql, pageId));
        }else{
            sql = "select * from music where state=1 ORDER BY RAND() LIMIT 1";
            rest.success().setData(Db.findFirst(sql));
        }
        renderJson(rest);
    }

    @Clear(WXSSLoginInterceptor.class)
    public void getPageMusic(){
        int pageId = getParaToInt("pageId", 0);

        String sql = "select m.* from music m left join doc_page dp on dp.music_id=m.id where dp.id=?";
        rest.success().setData(Db.findFirst(sql, pageId));
        renderJson(rest);
    }
}
