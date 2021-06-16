package com.sagacity.docs.service;

import com.sagacity.utility.FileUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.Random;

/**
 * 废话生成器
 */
public class BullshitGenerator {

    String[] famous;
    String[] bosh;
    String[] before;
    String[] after;
    String title = "";


    public void initData(String t){
        String path = BullshitGenerator.class.getClassLoader().getResource("bullshit_data.json").getPath();
        String s = FileUtil.readJsonFile(path);
        JSONObject dataFile = JSONObject.fromObject(s);
        //famous
        JSONArray j1 = dataFile.getJSONArray("famous");
        famous = new String[j1.size()];
        for (int i = 0; i < j1.size(); i++) {
            famous[i]= j1.get(i).toString();
        }
        //bosh
        JSONArray j2 = dataFile.getJSONArray("bosh");
        bosh = new String[j2.size()];
        for (int i = 0; i < j2.size(); i++) {
            bosh[i]= j2.get(i).toString();
        }
        //before
        JSONArray j3 = dataFile.getJSONArray("before");
        before = new String[j3.size()];
        for (int i = 0; i < j3.size(); i++) {
            before[i]= j3.get(i).toString();
        }
        //after
        JSONArray j4 = dataFile.getJSONArray("after");
        after = new String[j4.size()];
        for (int i = 0; i < j4.size(); i++) {
            after[i]= j4.get(i).toString();
        }

        title = t;
    }

    /**
     * 从字符数组中随机取一句
     * @param t
     * @return
     */
    public String randomGet(String[] t){
        Random random = new Random();
        int n = random.nextInt(t.length);
        return t[n];
    }

    public String getFamous(){
        String s = randomGet(famous);
        s = s.replace("a", randomGet(before));
        s = s.replace("b", randomGet(after));
        return s;
    }

    public String getBosh(){
        String s = randomGet(bosh);
        s = s.replace("x", title);
        return s;
    }

    /**
     * 产生新的段落
     * @param s
     * @return
     */
    public String genSection(String s){
        /**
         * 仅针对可以结尾的废话新开段落，否则不信开段落
         */
        if(s.substring(s.length()-1, s.length()).equals("。")){
             s = s.substring(0, s.length()-1);
            return s + "。 " + "</p><p>";
        }else{
            return s;
        }
    }

    public String genArticle(){
        StringBuffer article = new StringBuffer();
        for(int i=0; i<title.length(); i++){
            String section = "<p>";
            int sectionLength = 0;
            while(sectionLength < 4000 ){
                int r = new Random().nextInt(100);
                if(r < 5 && section.length() > 200){ //产生短文
                    section = genSection(section);
                    article.append(section);
                    section = "";
                }else if(r < 20){
                    String words = getFamous();
                    sectionLength += words.length();
                    section += words;
                }else{
                    String words = getBosh();
                    sectionLength += words.length();
                    section += words;
                }
            }
            section = genSection(section);
            article.append(section);
        }
        return article.append("<br/>").toString();
    }

}
