package com.sagacity.docs.wxss.controller.v1;

import com.jfinal.aop.Before;
import com.jfinal.aop.Duang;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.wxaapp.api.WxaQrcodeApi;
import com.jfinal.wxaapp.api.WxaUserApi;
import com.sagacity.docs.base.extend.CacheKey;
import com.sagacity.docs.model.doc.DocInfo;
import com.sagacity.docs.model.doc.DocPage;
import com.sagacity.docs.base.extend.ConstantValue;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.base.extend.RoleType;
import com.sagacity.docs.service.Qiniu;
import com.sagacity.docs.model.user.UserInfo;
import com.sagacity.docs.model.user.UserProfile;
import com.sagacity.docs.model.video.VideoInfo;
import com.sagacity.docs.model.weixin.WXUser;
import com.sagacity.docs.wxss.common.WXSSBaseController;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.PinyinUtil;
import com.sagacity.utility.StringTool;
import net.sf.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.List;

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

        WxaUserApi wxaUserApi = new WxaUserApi();
        ApiResult apiResult = wxaUserApi.getSessionKey(code);
        openid = apiResult.getStr("openid");

        WXUser wxUser = WXUser.dao.createUser(openid, userInfo);
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
        String type = getPara("type"); // d- 文档；c- 课程； p- 单页；
        String dataID = getPara("data_id");
        try {
            //生成菊花码
            WxaQrcodeApi wxaQrcodeApi = Duang.duang(WxaQrcodeApi.class);
            InputStream inputStream = wxaQrcodeApi.getUnLimit(type+"_"+dataID, "pages/index/index", 430);

            String fileName = System.currentTimeMillis()+".png";
            File f2 = new File(PropKit.get("resource.dir")+"qr_code/");
            if (!f2.exists()) {
                f2.mkdirs();
            }
            File file = new File(PropKit.get("resource.dir")+"qr_code/"+fileName);
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

            String message = "";
            String bgImg="", qrCode="";
            if(type.equals("d")){ //文档
                DocInfo di = DocInfo.dao.findById(dataID);
                message = di.getStr("title").indexOf("《")>=0? di.getStr("title") : "《"+di.getStr("title")+"》";
                bgImg = di.getStr("cover");
            }else if(type.equals("p")){ //单页
                DocPage dp = DocPage.dao.findById(dataID);
                DocInfo di = DocInfo.dao.findById(dp.get("doc_id"));
                String t1 = di.getStr("title").indexOf("《")>=0? di.getStr("title") : "《"+di.getStr("title")+"》";
                message = dp.getStr("title")+" —— " + t1;
                bgImg = di.getStr("cover");
            }else if(type.equals("v")){ //视频
                VideoInfo vi = VideoInfo.dao.findById(dataID);
                message = vi.getStr("title");
                bgImg = vi.getStr("cover");
            }
            qrCode = PropKit.get("resource.url")+"qr_code/"+fileName;
            responseData.put("qr_code", PropKit.get("resource.url")+"qr_code/"+  mergeImage(bgImg, qrCode, message));
            r = true;
        }catch (Exception ex){
            ex.printStackTrace();
            r = false;
            responseData.put(ResponseCode.MSG, "生成二维码出错！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    /**
     * 将生成的二维码与活动宣传图片进行组合
     */
    private String mergeImage(String bgImg, String qrCode, String message){

        String coverImg = "cover_"+System.currentTimeMillis()+".png"; //合成的图片地址
        try {
            BufferedImage buffImg = ImageIO.read(new URL(bgImg));
            BufferedImage waterImg = ImageIO.read(new URL(qrCode));
            //创建一个新图，在下方增加白边用于叠加二维码
            int bufferImgWidth = buffImg.getWidth();
            int bufferImgHeight = buffImg.getHeight();
            if(bufferImgWidth > 640){
                bufferImgHeight = (int)bufferImgHeight*640/bufferImgWidth;
                bufferImgWidth = 640;
            }

            BufferedImage bg = new BufferedImage(bufferImgWidth, bufferImgHeight+ 120, BufferedImage.TYPE_INT_RGB);
            // 创建Graphics2D对象，用在底图对象上绘图
            Graphics2D g2d = bg.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, bufferImgWidth, bufferImgHeight+ 120);
            // 在图形和图像中实现混合和透明效果
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1.0f));
            //叠加背景图
            g2d.drawImage(buffImg, 0, 0, bufferImgWidth, bufferImgHeight, null);
            //叠加二维码
            g2d.drawImage(waterImg, bufferImgWidth-120, bufferImgHeight+10, 100, 100, null);
            //写文字信息
            g2d.setColor(Color.BLACK);
//            Font df = FontUtil.loadFont(PathKit.getWebRootPath() +"/asset/fonts/simsun.ttc", 20);
//            g2d.setFont(df);
            g2d.setFont(new Font("宋体", Font.PLAIN, 20));
            g2d.drawString(message, 10, bufferImgHeight + 60);
            g2d.dispose();// 释放图形上下文使用的系统资源
            ImageIO.write(bg, "PNG", new FileOutputStream(PropKit.get("resource.dir")+"qr_code/"+coverImg));

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return coverImg;
    }


    /**
     * 扫描二维码登陆网页
     */
    public void scanLogin(){
        boolean r = false;
        String msg = "";

        String url = PropKit.get("base.url");
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
            CacheKit.put(CacheKey.CACHE_ACCOUNT_SCAN, key, userInfo.get("UserID")); //key由网页端生成时传入
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

    /**
     * 分类数据，包括文档与视频
     */
    public void getClassList(){

        //文档分类
        String sqlDoc = "select dc.*\n" +
                "from doc_class dc\n" +
                "where dc.state=1 and dc.parent_id = ?\n" +
                "order by dc.order DESC";
        List<Record> dc = new ArrayList<Record>();
        for (Record p : Db.find(sqlDoc, 0)){
            List<Record> d1 = Db.find(sqlDoc, p.getInt("id"));
            for (Record c : d1){
                c.set("son", Db.find(sqlDoc, c.getInt("id")));
            }
            dc.addAll(d1);
        }
        responseData.put("doc", dc);
        //视频分类
        String sqlVideo = "select * from video_class";
        if(StringTool.notNull(getPara("token")) && !StringTool.isBlank(getPara("token"))){
            //传入token,判断用户权限，设置可打开内容，后续优化
            WXUser user = WXUser.dao.findFirst("select * from wx_user where open_id=?", getPara("token"));
            if (user != null){
                switch (user.getInt("level_id")){
                    case 1: //初级用户
                        sqlVideo += " where state=1";
                        break;
                    case 2: //中级用户
                        sqlVideo += " where 1=1";
                    default:
                        break;
                }
            }else{
                sqlVideo += " where state=1";
            }
        }else{
            sqlVideo += " where state=1";
        }
        responseData.put("video", Db.find(sqlVideo));
        renderJson(responseData);
    }

}
