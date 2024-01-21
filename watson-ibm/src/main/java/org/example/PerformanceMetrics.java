package org.example;

import java.util.List;

public class PerformanceMetrics {

    private double PrecisionAt1 = 0.0;
    private double NDCG = 0.0;
    private double MeanReciprocalRank = 0.0;

    public PerformanceMetrics(){

    }

    public double getPrecisionAt1() {
        return PrecisionAt1;
    }

    public void setPrecisionAt1(double precisionAt1) {
        PrecisionAt1 = precisionAt1;
    }

    public double getNDCG() {
        return NDCG;
    }

    public void setNDCG(double NDCG) {
        this.NDCG = NDCG;
    }

    public double getMeanReciprocalRank() {
        return MeanReciprocalRank;
    }

    public void setMeanReciprocalRank(double meanReciprocalRank) {
        MeanReciprocalRank = meanReciprocalRank;
    }

    public double computeNDCG(List<ResultClass> result, double DCG, double IDCG) {
        for(int i = 0; i < result.size(); i++){
            DCG += (Math.pow(2, result.get(i).docScore) - 1)/(Math.log(i + 2)/Math.log(2));
        }
        result.sort((result1, result2) -> Double.compare(result2.docScore, result1.docScore));
        for(int i = 0; i < result.size(); i++){
            IDCG += (Math.pow(2, result.get(i).docScore) - 1)/(Math.log(i + 2)/Math.log(2));
        }
        return DCG/IDCG;
    }
}
