package com.github.chen0040.objdetect.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Box {

    private float left;
    private float top;
    private float width;
    private float height;

    public Box() {

    }

    public Box(float[] box) {
        left = box[1];
        top = box[0];
        width = box[3] - box[1];
        height = box[2] - box[0];
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append("left: ").append(left).append(", ");
        sb.append("top: ").append(top).append(", ");
        sb.append("width: ").append(width).append(", ");
        sb.append("height: ").append(height);
        sb.append(")");
        return sb.toString();
    }
}
