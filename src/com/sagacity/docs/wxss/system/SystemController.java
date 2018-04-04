package com.sagacity.docs.wxss.system;

import com.jfinal.aop.Before;
import com.jfinal.aop.Enhancer;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.weixin.sdk.utils.HttpUtils;
import com.jfinal.weixin.sdk.utils.JsonUtils;
import com.qiniu.util.Auth;
import com.sagacity.docs.doc.DocInfo;
import com.sagacity.docs.extend.ConstantValue;
import com.sagacity.docs.extend.ResponseCode;
import com.sagacity.docs.extend.RoleType;
import com.sagacity.docs.openapi.Qiniu;
import com.sagacity.docs.user.UserInfo;
import com.sagacity.docs.user.UserProfile;
import com.sagacity.docs.utility.DateUtils;
import com.sagacity.docs.utility.PinyinUtil;
import com.sagacity.docs.utility.PropertiesFactoryHelper;
import com.sagacity.docs.utility.StringTool;
import com.sagacity.docs.weixin.WXSetting;
import com.sagacity.docs.weixin.WXUser;
import com.sagacity.docs.wxss.WXSSBaseController;
import freemarker.template.utility.StringUtil;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by mulaliu on 16/4/15.
 */
@ControllerBind(controllerKey = "/wxss/system", viewPath = "/wxss")
public class SystemController extends WXSSBaseController {


    @Override
    public void index() {

    }

    /**
     * ---------------------
     *      以下为用户入口
     * ---------------------
     */

    /**
     * 基础调用
     */
    @Before(Tx.class)
    public void accountLogin(){
        boolean r = true;
        String code = getPara("code"); //获取openid
        JSONObject userInfo = JSONObject.fromObject(getPara("userInfo"));
        String openid;

        String getApiUrl = "https://api.weixin.qq.com/sns/jscode2session?grant_type=authorization_code";
        Map<String, String> params = new HashMap<String, String>();
        params.put("appid", PropertiesFactoryHelper.getInstance()
                .getConfig("wxss.appid"));
        params.put("secret", PropertiesFactoryHelper.getInstance()
                .getConfig("wxss.appsecret"));
        params.put("js_code", code);
        JSONObject res = JSONObject.fromObject(HttpUtils.get(getApiUrl, params));
        System.out.println(res.toString());
        openid = res.getString("openid");

        WXUser wxUser = Enhancer.enhance(WXUser.class).createUser(openid, userInfo);
        if(wxUser != null){
            r = true;
        }
        if(r){
            responseData.put("user", wxUser);
            responseData.put("token", openid);
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    public void getUploadToken(){
        String token = getPara("token");
        renderJson("uptoken", Qiniu.dao.getUploadToken());
    }

    /**
     * 获取微信小程序二维码
     */
    public void getWXSSCode(){
        boolean r = false;
        //菊花码地址
        String getApiUrl = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token="+ WXSetting.dao.getAccessToken();
        Map<String, Object> params = new HashMap<String, Object>();
        String type = getPara("type"); // d- 文档；c- 课程；
        String dataID = getPara("data_id");
        params.put("scene", type+"_"+dataID);
        params.put("path", "pages/index/index");
        params.put("width", 430);
        //生成图片
        try {
            InputStream inputStream = HttpUtils.download(getApiUrl, JsonUtils.toJson(params));

            String fileName = System.currentTimeMillis()+".png";
            File f2 = new File(PropertiesFactoryHelper.getInstance().getConfig("resource.dir")+"qr_code/");
            if (!f2.exists()) {
                f2.mkdirs();
            }
            File file = new File(PropertiesFactoryHelper.getInstance().getConfig("resource.dir")+"qr_code/"+fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream outputStream = new FileOutputStream(file);
            int len = 0;
            byte[] buf = new byte[1024];
            while ((len = inputStream.read(buf, 0, 1024)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.flush();
            r = true;
            responseData.put("qr_code", PropertiesFactoryHelper.getInstance().getConfig("resource.url")+"qr_code/"+fileName);
        }catch (Exception ex){
            ex.printStackTrace();
            r = false;
            responseData.put(ResponseCode.MSG, "生成二维码出错！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }


    /**
     * 扫描二维码登陆网页
     */
    private String AccountScanCacheName = "AccountScanCacheName";
    public void scanLogin(){
        boolean r = false;
        String msg = "";

        String url = PropertiesFactoryHelper.getInstance().getConfig("base.url");
        String token = getPara("token");
        String key = getPara("key");
        String password = StringTool.generateMixString(6);

        //检查此wx号是否创建了创作者账户，如果没有则创建新用户
        UserInfo userInfo = UserInfo.dao.findFirst("select * from sys_users where OpenID=?", token);
        if(userInfo == null){
            WXUser wxUser = WXUser.dao.findFirst("select * from wx_user where open_id=?", token);
            UserInfo ui = new UserInfo().set("UserID", UUID.randomUUID().toString()).set("RoleID", RoleType.Author).set("OpenID", token)
                    .set("Caption", wxUser.get("nick_name")).set("LoginName", PinyinUtil.getFirstLettersLo(wxUser.getStr("nick_name"))).set("Password",password)
                    .set("AddTime", DateUtils.nowDateTime()).set("CreateUserID", ConstantValue.DEFAULT_ADMINID)
                    .set("intState", 0);
            r = ui.save(); //此时创建的用户未启用
            UserProfile up = new UserProfile().set("UserID", ui.get("UserID")).set("Gender", wxUser.get("gender"))
                    .set("AvatarURL", wxUser.get("avatar_url")).set("UserPoint", 0.0f);
            r = up.save();
        }
        if(userInfo != null && userInfo.getInt("intState") == 1){
            //写入缓存，确认用户扫码验证通过;网页端循循环登陆
            CacheKit.put(AccountScanCacheName, key, userInfo.get("UserID")); //key由网页端生成时传入
            r = true;
            msg = "认证已通过，网页即将跳转！";
        }else{
            r = false;
            msg = "账号未启用，请联系管理员！";
        }
        responseData.put(ResponseCode.MSG, msg);
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);

    }

    /**
     * 结果搜索，包括doc和video
     */
    public void search(){
        String key = getPara("key");
        //文档
        String sql1 = "select dc.id,dc.cover,dc.title from doc_info dc\n" +
                "where dc.title like '%"+key+"%'";
        responseData.put("doc", Db.find(sql1));
        //视频
        String sql2 = "select vi.id,vi.cover,vi.title from video_info vi\n" +
                "where vi.title like '%"+key+"%'";
        responseData.put("video", Db.find(sql2));
        //全文检索结果
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("total", 0);
        result.put("searchTime", 0);
        result.put("items", null);
        responseData.put("result", result);
        renderJson(responseData);
    }

    /**
     * 提示搜索
     */
    public void tipSearch(){

    }

    /**
     * 获得热搜词
     */
    public void getHotSearch(){
        String sql = "select name,count(name) searchCount\n" +
                "from doc_search ds\n" +
                "group by ds.name\n" +
                "order by searchCount DESC";
        renderJson(ResponseCode.LIST, Db.find(sql));
    }

}
