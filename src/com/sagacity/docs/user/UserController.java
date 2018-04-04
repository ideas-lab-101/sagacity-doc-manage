package com.sagacity.docs.user;

import com.google.gson.JsonObject;
import com.jfinal.aop.Before;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.sagacity.docs.common.WebBaseController;
import com.sagacity.docs.extend.ResponseCode;
import com.sagacity.docs.utility.StringTool;
import net.sf.json.JSONObject;

@ControllerBind(controllerKey = "/user")
public class UserController extends WebBaseController{

    @Override
    public void index(){

    }

    public void getUserList(){
        String sql_select = "select u.*, IFNULL(dc.dCount,0) dcCount, IFNULL(vc.vCount,0) vcCount";
        String sql_from = "from sys_users u\n" +
                "left join (select count(*) dCount, user_id from doc_info group by  user_id) dc on dc.user_id=u.UserID\n" +
                "left join (select count(*) vCount, user_id from video_info group by  user_id) vc on vc.user_id=u.UserID\n" +
                "where 1=1 ";
        sql_from += "order by AddTime DESC";
        if (StringTool.notNull(getPara("pageIndex")) && !StringTool.isBlank(getPara("pageIndex"))){
            Page<Record> noticeList = Db.paginate(getParaToInt("pageIndex", 1),
                    getParaToInt("pageSize", 10), sql_select, sql_from);
            renderJson(convertPageData(noticeList));
        }else {
            renderJson(ResponseCode.LIST, Db.find(sql_select + "\n" + sql_from));
        }
    }

    public void setUserState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        r = Db.update("update sys_users set intState=? where UserID=?", state, getPara("user_id"))>0? true:false;
        if(r){
            if(state == 1){
                responseData.put(ResponseCode.MSG, "用户启用！");
            }else if(state == 0){
                responseData.put(ResponseCode.MSG, "用户停用！");
            }
        }else{
            responseData.put(ResponseCode.MSG, "操作失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    public  void getPayList(){
        JSONObject jo = getCurrentUser();

        String sql_select = "select pay.*";
        String sql_from = "from (\n" +
                "select pi.pay_id,pi.cost,'文档' type,pi.data_id,di.title,pi.order_code,pi.created_at,di.user_id\n" +
                "from pay_info pi\n" +
                "left join doc_info di on di.id=pi.data_id\n" +
                "where pi.state=2 and pi.data_type='doc'\n" +
                "UNION\n" +
                "select pi.pay_id,pi.cost,'视频' type,pi.data_id,vi.title,pi.order_code,pi.created_at,vi.user_id\n" +
                "from pay_info pi\n" +
                "left join video_info vi on vi.id=pi.data_id\n" +
                "where pi.state=2 and pi.data_type='video' ) pay where pay.user_id=?";
        sql_from += " order by pay.created_at Desc";
        if (StringTool.notNull(getPara("pageIndex")) && !StringTool.isBlank(getPara("pageIndex"))){
            Page<Record> noticeList = Db.paginate(getParaToInt("pageIndex", 1),
                    getParaToInt("pageSize", 10), sql_select, sql_from, jo.get("UserID"));
            renderJson(convertPageData(noticeList));
        }else {
            renderJson(ResponseCode.LIST, Db.find(sql_select + "\n" + sql_from, jo.get("UserID")));
        }

    }

    /**
     * 提现
     */
    @Before(Tx.class)
    public void withdraw(){

    }
}
