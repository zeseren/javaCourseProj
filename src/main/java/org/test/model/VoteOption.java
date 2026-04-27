package org.test.model;

/**
 * 投票选项实体类。
 *
 * voteCount 来自数据库，percent 是结果页展示时临时计算出来的比例。
 */
public class VoteOption {
    private int id;
    private int questionId;
    private String content;
    private int voteCount;
    private double percent;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }
}
