package org.test.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VoteQuestion 实体类单元测试。
 */
class VoteQuestionTest {

    @Test
    void shouldSetAndGetId() {
        VoteQuestion question = new VoteQuestion();
        question.setId(10);
        assertEquals(10, question.getId());
    }

    @Test
    void shouldSetAndGetTitle() {
        VoteQuestion question = new VoteQuestion();
        question.setTitle("你最喜欢的编程语言是？");
        assertEquals("你最喜欢的编程语言是？", question.getTitle());
    }

    @Test
    void shouldSetAndGetUserId() {
        VoteQuestion question = new VoteQuestion();
        question.setUserId(5);
        assertEquals(5, question.getUserId());
    }

    @Test
    void shouldSetAndGetUsername() {
        VoteQuestion question = new VoteQuestion();
        question.setUsername("李四");
        assertEquals("李四", question.getUsername());
    }

    @Test
    void shouldSetAndGetCreatedAt() {
        VoteQuestion question = new VoteQuestion();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        question.setCreatedAt(now);
        assertEquals(now, question.getCreatedAt());
    }

    @Test
    void shouldReturnZeroIdForNewQuestion() {
        VoteQuestion question = new VoteQuestion();
        assertEquals(0, question.getId());
    }

    @Test
    void shouldReturnNullTitleForNewQuestion() {
        VoteQuestion question = new VoteQuestion();
        assertNull(question.getTitle());
    }
}
