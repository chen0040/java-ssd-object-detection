package com.github.chen0040.objdetect.utils;

import com.google.protobuf.TextFormat;
import object_detection.protos.StringIntLabelMapOuterClass;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LabelUtils {
    public static String[] loadLabels() throws Exception {
        String text = new String(FileUtils.getBytes("labels/mscoco_label_map.pbtxt"), StandardCharsets.UTF_8);
        StringIntLabelMapOuterClass.StringIntLabelMap.Builder builder = StringIntLabelMapOuterClass.StringIntLabelMap.newBuilder();
        TextFormat.merge(text, builder);
        StringIntLabelMapOuterClass.StringIntLabelMap proto = builder.build();
        int maxId = 0;
        for (StringIntLabelMapOuterClass.StringIntLabelMapItem item : proto.getItemList()) {
            if (item.getId() > maxId) {
                maxId = item.getId();
            }
        }
        String[] ret = new String[maxId + 1];
        for (StringIntLabelMapOuterClass.StringIntLabelMapItem item : proto.getItemList()) {
            ret[item.getId()] = item.getDisplayName();
        }
        return ret;
    }
}
