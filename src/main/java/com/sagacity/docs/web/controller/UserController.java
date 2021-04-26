package com.sagacity.docs.web.controller;

import com.jfinal.aop.Before;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.sagacity.docs.base.extend.Consts;
import com.sagacity.docs.web.common.WebBaseController;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.model.UserDao;
import com.sagacity.utility.StringTool;

@ControllerBind(controllerKey = "/admin/user", viewPath = "/admin/user")
public class UserController extends WebBaseController{

    @Override
    public void index(){

    }

    public void getUserList(){
        String sql_select = "select u.user_id,u.account,u.created_at,u.state,up.nick_name, r.role_desc, IFNULL(dc.dCount,0) dcCount, IFNULL(vc.vCount,0) vcCount";
        String sql_from = "from sys_users u\n" +
                "left join user_profile up on up.user_id=u.user_id\n" +
                "left join sys_roles r on r.role_id=u.role_id\n" +
                "left join (select count(*) dCount, user_id from doc_info group by  user_id) dc on dc.user_id=u.user_id\n" +
                "left join (select count(*) vCount, user_id from video_info group by  user_id) vc on vc.user_id=u.user_id\n" +
                "where 1=1 ";
        if(StringTool.notNull(getPara("key")) && StringTool.notBlank(getPara("key"))){
            sql_from += " and (u.account like '%"+getPara("key")+"%' or up.nick_name like '%"+getPara("key")+"%') ";
        }
        sql_from += "order by created_at DESC";
        if (StringTool.notNull(getPara("pageIndex")) && !StringTool.isBlank(getPara("pageIndex"))){
            Page<Record> dataList = Db.paginate(getParaToInt("pageIndex", 1),
                    getParaToInt("pageSize", 10), sql_select, sql_from);
            rest.success().setData(dataList);
        }else {
            data.put(ResponseCode.LIST, Db.find(sql_select + "\n" + sql_from));
            rest.success().setData(data);
        }
        renderJson(rest);
    }

    public void setUserState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        r = Db.update("update sys_users set state=? where user_id=?", state, getPara("userId"))>0? true:false;
        if(r){
            if(state == Consts.STATE_VALID){
                rest.success("用户启用！");
            }else if(state == Consts.STATE_INVALID){
                rest.success("用户停用！");
            }
        }else{
            rest.error("操作失败！");
        }
        renderJson(rest);
    }

    public  void getPayList(){
        UserDao userInfo = getCurrentUser();

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
            Page<Record> dataList = Db.paginate(getParaToInt("pageIndex", 1),
                    getParaToInt("pageSize", 10), sql_select, sql_from, userInfo.getUser_id());
            rest.success().setData(dataList);
        }else {
            data.put(ResponseCode.LIST, Db.find(sql_select + "\n" + sql_from, userInfo.getUser_id()));
            rest.success().setData(data);
        }
        renderJson(rest);
    }

    /**
     * 提现
     */
    @Before(Tx.class)
    public void withdraw(){

    }
}
