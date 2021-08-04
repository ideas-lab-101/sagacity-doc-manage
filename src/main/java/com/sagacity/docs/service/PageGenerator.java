package com.sagacity.docs.service;

import com.jfinal.kit.PathKit;
import com.jfinal.render.FreeMarkerRender;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;


import java.io.*;
import java.util.Map;

public class PageGenerator {

    public boolean generate(String staticPath, Map<String, Object> model) {

        Writer writer = null;
        try {
            Configuration config = FreeMarkerRender.getConfiguration();
            config.setDirectoryForTemplateLoading(new File(PathKit.getWebRootPath()+"/template"));

            Template template = config.getTemplate("bullshit.html");
            File staticFile = new File(staticPath);
            File staticDir = staticFile.getParentFile();
            if (staticDir != null) {
                staticDir.mkdirs();
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(staticFile), "UTF-8"));
            template.process(model, writer);
            writer.flush();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (TemplateException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
