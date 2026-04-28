package org.test.model;

/**
 * 投票选项实体类 —— 问卷中每个可选项的数据。
 *
 * 一个 VoteQuestion（问卷）下面挂多个 VoteOption（选项）。
 * 比如问卷"最喜欢的语言？"下面有三个选项：
 *   - VoteOption { content: "Java", voteCount: 15 }
 *   - VoteOption { content: "Python", voteCount: 20 }
 *   - VoteOption { content: "Go", voteCount: 5 }
 *
 * percent 字段的特殊性：
 * voteCount 是直接从数据库查出来的真实票数，
 * 但 percent（得票比例）不是存在数据库里的，
 * 而是在结果页展示前临时算出来的。
 * 因为比例会随着投票实时变化，每次展示时计算更准确。
 */
public class VoteOption {

    /**
     * 选项编号 —— 数据库自动生成。
     */
    private int id;

    /**
     * 所属问卷编号 —— 这个选项属于哪个问卷。
     *
     * 为什么需要这个字段？
     * 删除问卷时要把它下面的选项一起删掉，
     * 没有这个关联就不知道哪些选项属于这个问卷。
     */
    private int questionId;

    /**
     * 选项内容 —— 展示给投票者看的文字。
     *
     * 例如："Java"、"Python"、"Go"
     */
    private String content;

    /**
     * 得票数 —— 这个选项被多少人投过。
     *
     * 每次有人投票，数据库里会对这个数字 +1。
     * 这是从数据库直接读取的真实数据。
     */
    private int voteCount;

    /**
     * 得票比例 —— 展示结果时临时计算的百分比。
     *
     * 不在数据库中存储，每次展示结果时用公式计算：
     * percent = (当前选项票数 / 总票数) × 100
     *
     * 如果是 0 票 / 0 票的情况，比例设为 0。
     */
    private double percent;

    // ==================== getter / setter ====================

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
