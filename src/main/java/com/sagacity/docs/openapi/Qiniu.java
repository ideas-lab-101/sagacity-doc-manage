package com.sagacity.docs.openapi;

import com.google.gson.Gson;
import com.jfinal.kit.PropKit;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.sagacity.utility.DateUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class Qiniu {

    public final static Qiniu dao = new Qiniu();

    public String getUploadToken(){
        //获取7牛的参数
        String accessKey = PropKit.get("qiniu.accessKey");
        String secretKey = PropKit.get("qiniu.secretKey");
        String bucket = PropKit.get("qiniu.bucket");

        Auth auth = Auth.create(accessKey, secretKey);
        //自定义返回参数
        //StringMap putPolicy = new StringMap();
        //putPolicy.put("returnBody", "{\"key\":\"$(key)\",\"hash\":\"$(etag)\",\"bucket\":\"$(bucket)\",\"fsize\":$(fsize)}");

        return auth.uploadToken(bucket);
    }

    public String uploadFile(File nFile, String upToken, String prefix){
        String fileName = nFile.getName();
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        String key = prefix+ DateUtils.getLongDateMilliSecond()+"."+suffix;
        Configuration cfg = new Configuration(Zone.zone2()); //华南
        //...其他参数参考类注释
        UploadManager uploadManager = new UploadManager(cfg);
        try {
            InputStream input = new FileInputStream(nFile);
            byte[] byt = new byte[input.available()];
            input.read(byt);
            try {
                Response response = uploadManager.put(byt, key, upToken);
                //解析上传成功的结果
                DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
                return putRet.key;
//                System.out.println(putRet.key);
//                System.out.println(putRet.hash);
            } catch (QiniuException ex) {
                Response r = ex.response;
                System.err.println(r.toString());
                try {
                    System.err.println(r.bodyString());
                } catch (QiniuException ex2) {
                    //ignore
                }
                return "";
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return "";
    }
}
