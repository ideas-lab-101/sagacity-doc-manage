package com.sagacity.docs.question;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="question_reply",pkName="id")
public class QuestionReply extends Model<QuestionReply> {

    public final static QuestionReply dao = new QuestionReply();

}
