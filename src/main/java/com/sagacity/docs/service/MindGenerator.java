package com.sagacity.docs.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Record;
import com.sagacity.utility.ConvertUtil;
import org.xmind.core.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MindGenerator {

    public final static MindGenerator dao = new MindGenerator();
    public int docId =0;

    public boolean generateMindMap(int docId, String bookName, String nodesJson){

        boolean result = false;
        this.docId = docId;

        // 创建思维导图的工作空间
        IWorkbookBuilder workbookBuilder = Core.getWorkbookBuilder();
        IWorkbook workbook = workbookBuilder.createWorkbook();

        // 获得默认sheet
        ISheet primarySheet = workbook.getPrimarySheet();

        // 获得根主题
        ITopic rootTopic = primarySheet.getRootTopic();
        // 设置根主题的标题
        rootTopic.setTitleText(bookName);

        // 章节 topic 的列表
        ArrayList<ITopic> chapterTopics = Lists.newArrayList();
        JSONArray nodes = JSONArray.parseArray(nodesJson);
        for (int i=0; i<nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            ITopic topic = workbook.createTopic();
            topic.setTitleText(node.getString("title"));
            topic.setHyperlink(PropKit.get("base.url")+"d?docId="+docId+"&pageId="+node.get("id"));
            chapterTopics.add(topic);
            if(null != node.get("children")){ //添加子节点
                addSubNodes(workbook, topic, node.getString("children"));
            }
        }

        // 把章节节点添加到要节点上
        chapterTopics.forEach(it -> rootTopic.add(it, ITopic.ATTACHED));
        try{
            File f = new File(PropKit.get("resource.dir")+"mind_map/");
            if (!f.exists()) {
                f.mkdirs();
            }
            // 保存文件
            workbook.save(PropKit.get("resource.dir") + "mind_map/" + bookName + ".xmind");
            result = true;
        }catch (Exception ex){
            result = false;
        }
        return result;
    }

    private void addSubNodes(IWorkbook workbook, ITopic pNode, String childNodesStr){
        // 创建小节节点
        JSONArray nodes = JSONArray.parseArray(childNodesStr);
        for (int i=0;i<nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            ITopic cTopic = workbook.createTopic();
            cTopic.setTitleText(node.getString("title"));
            cTopic.setHyperlink(PropKit.get("base.url")+"d?docId="+docId+"&pageId="+node.get("id"));

            pNode.add(cTopic, ITopic.ATTACHED);
            if(null != node.get("children")){ //添加子节点
                addSubNodes(workbook, cTopic, node.getString("children"));
            }
        }
    }
}
