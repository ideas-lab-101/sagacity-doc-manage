package com.sagacity.docs.question;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="question",pkName="id")
public class Question extends Model<Question> {

    public final static Question dao = new Question();

}
