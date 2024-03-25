/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.tags.cloud;

import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.text.Normalizer;
import java.util.Collection;

/**
 * Provides access to Wordle through exposed HTTP APIs.
 *
 * http://www.wordle.net/advanced
 * POST: wordcounts=word1:80,word2:20
 *
 * @author fanguad
 */
@Log4j2
public final class WordleAccess {
    private static final String WORDLE_URL = "http://www.wordle.net/compose";
    private static final String FORM_NAME = "wordcounts";
    private static final String NEWLINE = String.format("%n");

    public static void openWordle(Collection<TagCloudEntry> tags) {
        // create the form data
        StringBuilder sb = new StringBuilder();
        for (TagCloudEntry entry : tags) {
            // converts characters with diacritics to their closest ascii character
            String normalized = Normalizer.normalize(entry.getTag(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            // removes all other non-ASCII characters
            normalized = normalized.replaceAll("[^\\p{ASCII}]+", "");
            sb.append(normalized);
            sb.append(':');
            sb.append(entry.getCount());
            sb.append(NEWLINE);
        }

        // remove the last comma
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length()-1);
        }

        try {
            URI tempForm = createTempForm(sb.toString());
            Desktop.getDesktop().browse(tempForm);
        } catch (IOException e) {
            log.warn("Error creating NowPlaying-Wordle bridge file", e);
        }
    }

    public static URI createTempForm(String formData) throws IOException {
        File temp = File.createTempFile("nowplaying_wordle_bridge_",".html");
        PrintWriter out = new PrintWriter(temp);
        out.println("<html><head>");
//        out.println("<script type = \"text/javascript\">");
//        out.println("function onLoad() {");
//        out.println("document.getElementById('form').submit(); }");
//        out.println("</script>");
        out.println("</head>");
        out.println("<body>");
        out.print("<form name=\"form\" action=\"");
        out.print(WORDLE_URL);
        out.println("\" method=\"post\">");
//        out.print("<input name=\"");
        out.print("<textarea rows=\"40\" cols=\"80\"  name=\"");
        out.print(FORM_NAME);
        out.print("\" id=\"");
        out.print(FORM_NAME);
        out.print("\"/>");
        out.print(formData);
        out.println("</textarea>");
        out.println("<input type=\"submit\" class=\"submit\" value=\"load tag cloud at wordle.net\"/>");
        out.println("</form> </body>");
        out.close();

        return temp.toURI();
    }
}
