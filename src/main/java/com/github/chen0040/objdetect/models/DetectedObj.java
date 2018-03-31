package com.github.chen0040.objdetect.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetectedObj {
    private String label;
    private float score;
    private float[] box;

    public DetectedObj(){

    }
    public DetectedObj(String label, float score, float[] box) {
        this.label = label;
        this.score = score;
        this.box = box;
    }
    @Override
    public String toString() {
        return "{ label: " + label + ", score: " + score + ", box: " + formatBox(box) + " }";
    }

    private String formatBox(float[] box) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for(int i=0; i < box.length; ++i) {
            if(i != 0) {
                sb.append(", ");
            }
            sb.append(box[i]);
        }
        sb.append(")");
        return sb.toString();
    }
}
