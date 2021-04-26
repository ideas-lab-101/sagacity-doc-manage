package com.sagacity.docs.wxapp.controller.v1;

import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.sagacity.docs.model.UserDao;
import com.sagacity.docs.model.comment.Comment;
import com.sagacity.docs.service.ContentCheckApi;
import com.sagacity.docs.wxapp.common.WXSSBaseController;
import com.sagacity.docs.wxapp.common.WXSSLoginInterceptor;
import com.sagacity.utility.DateUtils;

@ControllerBind(controllerKey = "/wxss/comment", viewPath = "/wxss")
public class CommentController extends WXSSBaseController{

    @Override
    public void index(){

    }

    /**
     * 获取评论列表
     */
    @Clear(WXSSLoginInterceptor.class)
    public void getCommentList(){
        int page = getParaToInt("page"); //数据分页

        int dataId = getParaToInt("dataId");
        String type = getPara("type");

        String sqlSelect = "select ac.id,u1.user_id,p1.nick_name,p1.avatar_url,ac.refer_id,u2.user_id ref_user_id,p2.nick_name ref_nick_name\n" +
                ",ac.content,ac.created_at";
        String sqlFrom ="from comment ac\n" +
                "left join sys_users u1 on u1.user_id=ac.user_id\n" +
                "left join user_profile p1 on p1.user_id=u1.user_id\n" +
                "left join comment acr on acr.id=ac.refer_id\n" +
                "left join sys_users u2 on u2.user_id=acr.user_id\n" +
                "left join user_profile p2 on p2.user_id=u2.user_id\n" +
                "where ac.state=1 and ac.source_id=? and ac.source=?";
        sqlFrom +=" order by ac.created_at DESC";
        Page<Record> commentList = Db.paginate(page, getParaToInt("pageSize", 10),
                sqlSelect, sqlFrom, dataId , type);
//        List<Record> cList = commentList.getList();
//        String sqlChild = "select ac.id,u2.user_id,p2.nick_name,p2.avatar_url,ac.refer_id\n" +
//                ",ac.content,ac.created_at\n" +
//                "from comment ac\n" +
//                "left join sys_users u2 on u2.user_id=ac.user_id\n" +
//                "left join user_profile p2 on p2.user_id=u2.user_id\n" +
//                "where ac.state=1 and ac.refer_id=?\n" +
//                "order by ac.created_at DESC";
//        for (Record rs : cList) {
//            rs.set("child", Db.find(sqlChild, rs.getInt("id")));
//        }

        rest.success().setData(commentList);
        renderJson(rest);
    }

    /**
     * 需要对content进行合法性检查
     */
    @Before(Tx.class)
    public void addComment(){
        UserDao userInfo = getCurrentUser(getPara("token"));

        int dataId = getParaToInt("dataId");
        int referId = getParaToInt("referId", 0);
        String type = getPara("type");

        String content = getPara("content");
        boolean r = false;

        ApiResult result = ContentCheckApi.msgCheck(content);
        if(result.isSucceed()){ //检查成功
            Comment ac = new Comment().set("refer_id",referId).set("source_id", dataId).set("source", type)
                    .set("user_id", userInfo.getUser_id()).set("form_id", getPara("form_id"))
                    .set("title", getPara("title", "")).set("content", content)
                    .set("state", 1).set("created_at", DateUtils.nowDateTime());
            r = ac.save();
            //推送模板消息
            if(referId >0){
//            WXMessage.dao.sendCommentReply(Module.Activity, refer_id, ac.getInt("CommentID"));
            }
            if(r){
                rest.success("留言成功！");
            }else{
                rest.error("留言失败！");
            }
        }else{
            rest.error("内容非法！");
        }

        renderJson(rest);
    }

    /**
     * 删除留言
     */
    @Before(Tx.class)
    public void delComment(){
        UserDao userInfo = getCurrentUser(getPara("token"));
        int dataId = getParaToInt("dataId");
        boolean r = false;

//        r = Comment.dao.deleteById(commentID);
        r = Comment.dao.findById(dataId).set("state", 0).update();
        if(r){
            rest.success("删除成功！");
        }else{
            rest.error("删除失败！");
        }
        renderJson(rest);
    }

}
