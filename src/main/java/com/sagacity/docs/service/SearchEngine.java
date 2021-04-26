package com.sagacity.docs.service;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import org.apdplat.word.WordSegmenter;
import org.apdplat.word.segmentation.Segmentation;
import org.apdplat.word.segmentation.SegmentationAlgorithm;
import org.apdplat.word.segmentation.SegmentationFactory;
import org.apdplat.word.segmentation.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用中文分词器将搜索内容分解为关键词
 */
public class SearchEngine {

    public final static SearchEngine dao = new SearchEngine();
    /**
     * 初始化
     */
    public void init(){
        WordSegmenter.segWithStopWords("分词器初始化……");
    }

    public String seg(String text) {
        StringBuilder result = new StringBuilder();
        for(Word word : WordSegmenter.segWithStopWords(text)){
            result.append(word.getText()).append(" ");
        }
        return result.toString();
    }

    public List<String> segList(String text){
        List<String> keys = new ArrayList<String>();
        for(Word word : WordSegmenter.segWithStopWords(text)){
            keys.add(word.getText());
        }
        return keys;
    }

    public List<Record> doSearch(String key){
        String sql = "select di.id,di.title,di.`desc`,di.cover,di.source,di.is_end\n" +
                ",dc.id doc_class_id,dc.title doc_class,u.user_id,u.Caption \n" +
                "from doc_info di\n" +
                "left join doc_class dc on dc.id=di.doc_class_id\n" +
                "left join sys_users u on u.user_id=di.user_id\n" +
                "where di.title like '%"+key+"%' or u.Caption like '%"+key+"%'";

        List<Record> rs = Db.find(sql);
        return rs;
    }

    public static void main(String[] args) {
        SearchEngine sg = new SearchEngine();
        System.out.println(sg.seg("我还是从前那个少年没有一丝丝改变!"));
    }
}
