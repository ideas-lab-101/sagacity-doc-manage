package com.sagacity.docs.wxapp.controller.v1;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.aop.Duang;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.kit.HashKit;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.wxaapp.api.WxaQrcodeApi;
import com.jfinal.wxaapp.api.WxaUserApi;
import com.sagacity.docs.base.extend.CacheKey;
import com.sagacity.docs.model.UserDao;
import com.sagacity.docs.model.doc.DocInfo;
import com.sagacity.docs.model.doc.DocPage;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.service.Qiniu;
import com.sagacity.docs.model.video.VideoInfo;
import com.sagacity.docs.model.wxapp.WxaUser;
import com.sagacity.docs.wxapp.common.WXSSBaseController;
import com.sagacity.docs.wxapp.common.WXSSLoginInterceptor;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.StringTool;

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
    @Clear(WXSSLoginInterceptor.class)
    public void accountLogin(){
        boolean r = false;
        String code = getPara("code"); //获取openid
        JSONObject userData = null;

        if(StringTool.notBlank(getPara("userData")) && StringTool.notNull(getPara("userData"))){
            userData = JSONObject.parseObject(getPara("userData"));
        }
        String openid, session_key;
        String token = HashKit.md5(DateUtils.getLongDateMilliSecond()+"");

        WxaUserApi wxaUserApi = new WxaUserApi();
        ApiResult apiResult = wxaUserApi.getSessionKey(code);
//        System.out.println(apiResult);
        openid = apiResult.getStr("openid");
        session_key = apiResult.getStr("session_key");

        UserDao user = WxaUser.dao.setWxappUser(openid, userData);
        if(user != null){
            r = true;
            //写入缓存
            JSONObject jo = new JSONObject();
            jo.put("loginTime", DateUtils.nowDateTime());
            jo.put("userInfo", user);
            jo.put("sessionKey", session_key); //缓存session_key，用于取电话号码等操作
            CacheKit.put(CacheKey.CACHE_WXAPP, token, jo);
        }
        if(r){
            data.put("user_info", user);
            data.put("token", token);
            rest.success().setData(data);
        }else{
            rest.error();
        }
        renderJson(rest);
    }

    public void getUploadToken(){
        UserDao userInfo = getCurrentUser(getPara("token"));
        data.put("uptoken", Qiniu.dao.getUploadToken());
        rest.success().setData(data);
    }

    /**
     * 获取微信小程序二维码
     */
    public void getWXSSCode(){
        boolean r = false;
        String type = getPara("type"); // d- 文档；c- 课程； p- 单页；
        String dataId = getPara("dataId");
        try {
            //生成菊花码
            WxaQrcodeApi wxaQrcodeApi = Duang.duang(WxaQrcodeApi.class);
            InputStream inputStream = wxaQrcodeApi.getUnLimit(type+"_"+dataId, "pages/index/index", 430);

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
                DocInfo di = DocInfo.dao.findById(dataId);
                message = di.getStr("title").indexOf("《")>=0? di.getStr("title") : "《"+di.getStr("title")+"》";
                bgImg = di.getStr("cover");
            }else if(type.equals("p")){ //单页
                DocPage dp = DocPage.dao.findById(dataId);
                DocInfo di = DocInfo.dao.findById(dp.get("doc_id"));
                String t1 = di.getStr("title").indexOf("《")>=0? di.getStr("title") : "《"+di.getStr("title")+"》";
                message = dp.getStr("title")+" —— " + t1;
                bgImg = di.getStr("cover");
            }else if(type.equals("v")){ //视频
                VideoInfo vi = VideoInfo.dao.findById(dataId);
                message = vi.getStr("title");
                bgImg = vi.getStr("cover");
            }
            qrCode = PropKit.get("resource.url")+"qr_code/"+fileName;
            data.put("qr_code", PropKit.get("resource.url")+"qr_code/"+  mergeImage(bgImg, qrCode, message));
            rest.success().setData(data);
        }catch (Exception ex){
            ex.printStackTrace();
            rest.error("生成二维码出错！");
        }

        renderJson(rest);
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
        UserDao userInfo = getCurrentUser(getPara("token"));

        boolean r = false;
        String msg = "";

        String url = PropKit.get("base.url");
        String key = getPara("key");
        String password = StringTool.generateMixString(6);

        if(userInfo != null){
            //写入缓存，确认用户扫码验证通过;网页端循循环登陆
            CacheKit.put(CacheKey.CACHE_ACCOUNT_SCAN, key, userInfo.getUser_id()); //key由网页端生成时传入
            rest.success("认证已通过，网页即将跳转！");
        }else{
            rest.error("账号错误，请联系管理员！");
        }
        renderJson(rest);
    }

    /**
     * 结果搜索，包括doc和video
     */
    @Clear(WXSSLoginInterceptor.class)
    public void search(){
        String key = getPara("key");
        //文档
        String sql1 = "select dc.id,dc.cover,dc.title from doc_info dc\n" +
                "where dc.title like '%"+key+"%'";
        data.put("doc", Db.find(sql1));
        //视频
        String sql2 = "select vi.id,vi.cover,vi.title from video_info vi\n" +
                "where vi.title like '%"+key+"%'";
        data.put("video", Db.find(sql2));
        //全文检索结果
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("total", 0);
        result.put("searchTime", 0);
        result.put("items", null);
        data.put("result", result);
        rest.success().setData(data);

        renderJson(rest);
    }

    /**
     * 提示搜索
     */
    @Clear(WXSSLoginInterceptor.class)
    public void tipSearch(){

    }

    /**
     * 获得热搜词
     */
    @Clear(WXSSLoginInterceptor.class)
    public void getHotSearch(){
        String sql = "select name,count(name) searchCount\n" +
                "from doc_search ds\n" +
                "group by ds.name\n" +
                "order by searchCount DESC";
        data.put(ResponseCode.LIST, Db.find(sql));

        rest.success().setData(data);
        renderJson(rest);
    }

    /**
     * 分类数据，包括文档与视频
     */
    @Clear(WXSSLoginInterceptor.class)
    public void getClassList(){
        UserDao userInfo = getCurrentUser(getPara("token"));

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
        data.put("doc", dc);
        //视频分类
        String sqlVideo = "select * from video_class";
        if(userInfo != null){
            //传入token,判断用户权限，设置可打开内容，后续优化
            switch (userInfo.getLevel()){
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

        data.put("video", Db.find(sqlVideo));
        rest.success().setData(data);

        renderJson(rest);
    }

    public void userFeedback(){
        boolean r = true;
        UserDao userInfo = getCurrentUser(getPara("token"));
        if(r){
            rest.success("谢谢反馈！");
        }else {
            rest.error("提交失败！");
        }
        renderJson(rest);
    }

}
