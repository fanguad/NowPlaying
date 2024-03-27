/*
 * Copyright (c) 2024. Dan Clark
 */

package org.nekocode.nowplaying.tags.cloud;

import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.CircleBackground;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.palette.ColorPalette;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates tag cloud images using the Kumo library
 */
@Log4j2
public class KumoCloud {
    /**
     * Creates a tag cloud image based on the given collection of tags.
     *
     * @param tags         the collection of tags to be included in the cloud
     * @param tagCloudSize the size of the tag cloud image
     * @return the image icon representing the tag cloud
     */
    public static ImageIcon createCloud(Collection<TagCloudEntry> tags, int tagCloudSize) {
        try {
            List<WordFrequency> wordFrequencies = tags.stream()
                    .map(t -> new WordFrequency(t.getTag(), t.getCount()))
                    .collect(Collectors.toCollection(ArrayList::new)); // Kumo requires a mutable list
            Dimension dimension = new Dimension(tagCloudSize, tagCloudSize);
            WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
            wordCloud.setPadding(2);
            wordCloud.setBackground(new CircleBackground(tagCloudSize / 2));
            wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1), new Color(0x40C5F1), new Color(0x40D3F1), new Color(0xFFFFFF)));
            wordCloud.setFontScalar(new SqrtFontScalar(10, 40));
            wordCloud.build(wordFrequencies);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            wordCloud.writeToStreamAsPNG(outputStream);
            var pngBytes = outputStream.toByteArray();
            return new ImageIcon(pngBytes);
        } catch (RuntimeException e) {
            log.error("Error creating tag cloud", e);
            throw e;
        }
    }
}
