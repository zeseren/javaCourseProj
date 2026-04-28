package org.test.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VoteOption 实体类单元测试。
 */
class VoteOptionTest {

    @Test
    void shouldSetAndGetId() {
        VoteOption option = new VoteOption();
        option.setId(1);
        assertEquals(1, option.getId());
    }

    @Test
    void shouldSetAndGetQuestionId() {
        VoteOption option = new VoteOption();
        option.setQuestionId(10);
        assertEquals(10, option.getQuestionId());
    }

    @Test
    void shouldSetAndGetContent() {
        VoteOption option = new VoteOption();
        option.setContent("Java");
        assertEquals("Java", option.getContent());
    }

    @Test
    void shouldSetAndGetVoteCount() {
        VoteOption option = new VoteOption();
        option.setVoteCount(42);
        assertEquals(42, option.getVoteCount());
    }

    @Test
    void shouldSetAndGetPercent() {
        VoteOption option = new VoteOption();
        option.setPercent(75.5);
        assertEquals(75.5, option.getPercent(), 0.001);
    }

    @Test
    void shouldReturnZeroVoteCountForNewOption() {
        VoteOption option = new VoteOption();
        assertEquals(0, option.getVoteCount());
    }

    @Test
    void shouldReturnZeroPercentForNewOption() {
        VoteOption option = new VoteOption();
        assertEquals(0.0, option.getPercent(), 0.001);
    }
}
