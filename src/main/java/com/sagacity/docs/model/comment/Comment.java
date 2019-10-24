package com.sagacity.docs.model.comment;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="comment",pkName="id")
public class Comment extends Model<Comment> {

    public final static Comment dao = new Comment();

}
