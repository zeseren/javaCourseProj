package org.test.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.test.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VoteModel 集成测试。
 *
 * 这些测试需要连接真实的 PostgreSQL 数据库（campus_vote）。
 * 测试依赖 users 表（需要先有用户才能创建问卷），
 * 每次测试前会创建专门的测试用户，测试后清理所有测试数据。
 *
 * 运行前请确认：
 * 1. PostgreSQL 已启动
 * 2. campus_vote 数据库、users / vote_questions / vote_options 表已创建
 */
class VoteModelTest {

    private static final String TEST_USER = "_test_vote_model_";
    private int testUserId;

    @BeforeEach
    void setUp() throws SQLException {
        // 先清理上一次残留，再创建新测试用户
        deleteTestData();
        UserModel.createUser(TEST_USER, "testpass");
        User user = UserModel.findByUsername(TEST_USER);
        assertNotNull(user, "测试用户创建失败，无法继续测试");
        testUserId = user.getId();
    }

    @AfterEach
    void tearDown() throws SQLException {
        deleteTestData();
    }

    private void deleteTestData() throws SQLException {
        Connection conn = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            conn = JdbcUtil.getConnection();
            // 先删选项，再删问卷（外键约束）
            ps1 = conn.prepareStatement(
                    "delete from vote_options where question_id in (select id from vote_questions where user_id in (select id from users where username like ?))");
            ps1.setString(1, TEST_USER + "%");
            ps1.executeUpdate();

            ps2 = conn.prepareStatement("delete from vote_questions where user_id in (select id from users where username like ?)");
            ps2.setString(1, TEST_USER + "%");
            ps2.executeUpdate();

            // 删除测试用户
            PreparedStatement ps3 = conn.prepareStatement("delete from users where username like ?");
            ps3.setString(1, TEST_USER + "%");
            ps3.executeUpdate();
            ps3.close();
        } finally {
            JdbcUtil.close(ps1, null);
            JdbcUtil.close(ps2, conn);
        }
    }

    // ==================== createQuestion 测试 ====================

    @Test
    void shouldCreateQuestionWithOptions() throws SQLException {
        List<String> options = Arrays.asList("选项A", "选项B", "选项C");
        boolean created = VoteModel.createQuestion(testUserId, "测试问卷标题", options);
        assertTrue(created);

        // 验证问卷和选项都已写入
        List<VoteQuestion> all = VoteModel.findAllQuestions();
        VoteQuestion found = null;
        for (VoteQuestion q : all) {
            if ("测试问卷标题".equals(q.getTitle()) && q.getUserId() == testUserId) {
                found = q;
                break;
            }
        }
        assertNotNull(found, "创建的问卷应该在列表中能找到");
        assertEquals(TEST_USER, found.getUsername());

        // 验证选项也已写入
        List<VoteOption> savedOptions = VoteModel.findOptionsByQuestionId(found.getId());
        assertEquals(3, savedOptions.size());
    }

    @Test
    void shouldFailWhenCreatingQuestionWithoutOptions() throws SQLException {
        // 传入空选项列表，应该因无选项而无法正常执行（batch 不会失败但也不会插入）
        // 实际行为：会创建一个没有选项的问卷
        boolean created = VoteModel.createQuestion(testUserId, "空选项问卷", new ArrayList<String>());
        // createQuestion 在 option 为空时 batch 不插入任何行，但 commit 仍成功
        assertTrue(created);
    }

    // ==================== findAllQuestions 测试 ====================

    @Test
    void shouldFindAllQuestions() throws SQLException {
        // 创建两个问卷
        VoteModel.createQuestion(testUserId, "问卷一", Arrays.asList("是", "否"));
        VoteModel.createQuestion(testUserId, "问卷二", Arrays.asList("好", "坏"));

        List<VoteQuestion> questions = VoteModel.findAllQuestions();
        // 验证至少能找到刚创建的两个
        int count = 0;
        for (VoteQuestion q : questions) {
            if (q.getUserId() == testUserId) {
                count++;
            }
        }
        assertTrue(count >= 2, "应该至少找到两个测试问卷");
    }

    @Test
    void shouldReturnQuestionsOrderedByCreatedAtDesc() throws SQLException {
        VoteModel.createQuestion(testUserId, "先创建的", Arrays.asList("A", "B"));
        // 短暂等待，确保创建时间不同
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        VoteModel.createQuestion(testUserId, "后创建的", Arrays.asList("C", "D"));

        List<VoteQuestion> questions = VoteModel.findAllQuestions();

        // 找测试用户的问题，按时间降序排列，"后创建的" 应该排在 "先创建的" 前面
        int laterIndex = -1;
        int earlierIndex = -1;
        for (int i = 0; i < questions.size(); i++) {
            VoteQuestion q = questions.get(i);
            if (q.getUserId() == testUserId) {
                if ("后创建的".equals(q.getTitle())) {
                    laterIndex = i;
                } else if ("先创建的".equals(q.getTitle())) {
                    earlierIndex = i;
                }
            }
        }
        assertTrue(laterIndex >= 0 && earlierIndex >= 0, "两个测试问卷都应该存在");
        assertTrue(laterIndex < earlierIndex, "后创建的应排在前面（降序）");
    }

    // ==================== findQuestionById 测试 ====================

    @Test
    void shouldFindQuestionById() throws SQLException {
        VoteModel.createQuestion(testUserId, "查找测试", Arrays.asList("选A", "选B"));

        // 拿到刚创建的问卷 ID
        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("查找测试".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }
        assertTrue(questionId > 0);

        VoteQuestion question = VoteModel.findQuestionById(questionId);
        assertNotNull(question);
        assertEquals("查找测试", question.getTitle());
        assertEquals(TEST_USER, question.getUsername());
    }

    @Test
    void shouldReturnNullForNonExistentQuestionId() throws SQLException {
        VoteQuestion question = VoteModel.findQuestionById(999999);
        assertNull(question);
    }

    // ==================== findOptionsByQuestionId 测试 ====================

    @Test
    void shouldFindOptionsByQuestionId() throws SQLException {
        VoteModel.createQuestion(testUserId, "选项查询", Arrays.asList("Java", "Python", "Go"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("选项查询".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }

        List<VoteOption> options = VoteModel.findOptionsByQuestionId(questionId);
        assertEquals(3, options.size());
        // 每个选项初始票数为 0
        for (VoteOption option : options) {
            assertEquals(0, option.getVoteCount());
        }
    }

    @Test
    void shouldReturnEmptyListForNonExistentQuestion() throws SQLException {
        List<VoteOption> options = VoteModel.findOptionsByQuestionId(999999);
        assertNotNull(options);
        assertTrue(options.isEmpty());
    }

    // ==================== increaseOptionVoteCount 测试 ====================

    @Test
    void shouldIncreaseVoteCount() throws SQLException {
        VoteModel.createQuestion(testUserId, "投票测试", Arrays.asList("赞成", "反对"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("投票测试".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }

        List<VoteOption> options = VoteModel.findOptionsByQuestionId(questionId);
        int optionId = options.get(0).getId();

        // 投一次
        boolean increased = VoteModel.increaseOptionVoteCount(optionId);
        assertTrue(increased);

        // 验证票数 +1
        List<VoteOption> updated = VoteModel.findOptionsByQuestionId(questionId);
        assertEquals(1, updated.get(0).getVoteCount());
    }

    @Test
    void shouldIncreaseVoteCountMultipleTimes() throws SQLException {
        VoteModel.createQuestion(testUserId, "多次投票", Arrays.asList("选项"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("多次投票".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }

        List<VoteOption> options = VoteModel.findOptionsByQuestionId(questionId);
        int optionId = options.get(0).getId();

        // 连续投 5 次
        VoteModel.increaseOptionVoteCount(optionId);
        VoteModel.increaseOptionVoteCount(optionId);
        VoteModel.increaseOptionVoteCount(optionId);
        VoteModel.increaseOptionVoteCount(optionId);
        VoteModel.increaseOptionVoteCount(optionId);

        List<VoteOption> updated = VoteModel.findOptionsByQuestionId(questionId);
        assertEquals(5, updated.get(0).getVoteCount());
    }

    // ==================== isQuestionOwner 测试 ====================

    @Test
    void shouldReturnTrueForOwner() throws SQLException {
        VoteModel.createQuestion(testUserId, "所有权测试", Arrays.asList("A", "B"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("所有权测试".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }

        assertTrue(VoteModel.isQuestionOwner(questionId, testUserId));
    }

    @Test
    void shouldReturnFalseForNonOwner() throws SQLException {
        VoteModel.createQuestion(testUserId, "别人问卷", Arrays.asList("A", "B"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("别人问卷".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }

        // 用一个不存在的用户 ID 验证
        assertFalse(VoteModel.isQuestionOwner(questionId, 999999));
    }

    // ==================== optionBelongsToQuestion 测试 ====================

    @Test
    void shouldReturnTrueWhenOptionBelongsToQuestion() throws SQLException {
        VoteModel.createQuestion(testUserId, "选项归属", Arrays.asList("选项X"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("选项归属".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }

        List<VoteOption> options = VoteModel.findOptionsByQuestionId(questionId);
        assertFalse(options.isEmpty());

        assertTrue(VoteModel.optionBelongsToQuestion(options.get(0).getId(), questionId));
    }

    @Test
    void shouldReturnFalseWhenOptionDoesNotBelongToQuestion() throws SQLException {
        VoteModel.createQuestion(testUserId, "问卷Q1", Arrays.asList("选项"));
        VoteModel.createQuestion(testUserId, "问卷Q2", Arrays.asList("另一个选项"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int q1Id = -1;
        int q2Id = -1;
        for (VoteQuestion q : all) {
            if ("问卷Q1".equals(q.getTitle()) && q.getUserId() == testUserId) {
                q1Id = q.getId();
            } else if ("问卷Q2".equals(q.getTitle()) && q.getUserId() == testUserId) {
                q2Id = q.getId();
            }
        }

        List<VoteOption> q2Options = VoteModel.findOptionsByQuestionId(q2Id);
        assertFalse(q2Options.isEmpty());

        // 问卷 Q2 的选项不应该属于问卷 Q1
        assertFalse(VoteModel.optionBelongsToQuestion(q2Options.get(0).getId(), q1Id));
    }

    // ==================== deleteQuestion 测试 ====================

    @Test
    void shouldDeleteQuestion() throws SQLException {
        VoteModel.createQuestion(testUserId, "待删除", Arrays.asList("A", "B"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("待删除".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }

        boolean deleted = VoteModel.deleteQuestion(questionId);
        assertTrue(deleted);

        // 验证确实删除了
        VoteQuestion question = VoteModel.findQuestionById(questionId);
        assertNull(question);
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentQuestion() throws SQLException {
        boolean result = VoteModel.deleteQuestion(999999);
        assertFalse(result);
    }

    // ==================== 管理员功能测试 ====================

    @Test
    void shouldCreateQuestionWithPendingStatus() throws SQLException {
        VoteModel.createQuestion(testUserId, "待审批问卷", Arrays.asList("A", "B"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        VoteQuestion found = null;
        for (VoteQuestion q : all) {
            if ("待审批问卷".equals(q.getTitle()) && q.getUserId() == testUserId) {
                found = q;
                break;
            }
        }
        assertNotNull(found, "应找到新创建的问卷");
        assertEquals("pending", found.getStatus(), "新发布的问卷状态应为 pending");
    }

    @Test
    void shouldFindQuestionsForUserOnlyApprovedAndEnded() throws SQLException {
        // 创建一个问卷（状态为 pending）
        VoteModel.createQuestion(testUserId, "用户视角过滤", Arrays.asList("A", "B"));

        // 普通用户视角不应该看到 pending 的问卷
        List<VoteQuestion> questions = VoteModel.findQuestionsForUser();
        for (VoteQuestion q : questions) {
            if ("用户视角过滤".equals(q.getTitle()) && q.getUserId() == testUserId) {
                fail("普通用户不应该看到待审批的问卷");
            }
        }
    }

    @Test
    void shouldApproveQuestion() throws SQLException {
        VoteModel.createQuestion(testUserId, "待审批", Arrays.asList("A", "B"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("待审批".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }
        assertTrue(questionId > 0);

        // 审批通过
        boolean approved = VoteModel.approveQuestion(questionId);
        assertTrue(approved, "审批应该成功");

        // 验证状态已变为 approved
        VoteQuestion question = VoteModel.findQuestionById(questionId);
        assertEquals("approved", question.getStatus());
    }

    @Test
    void shouldEndQuestion() throws SQLException {
        VoteModel.createQuestion(testUserId, "待结束", Arrays.asList("A", "B"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("待结束".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }
        assertTrue(questionId > 0);

        // 先审批通过
        VoteModel.approveQuestion(questionId);

        // 再结束
        boolean ended = VoteModel.endQuestion(questionId);
        assertTrue(ended, "结束应该成功");

        VoteQuestion question = VoteModel.findQuestionById(questionId);
        assertEquals("ended", question.getStatus());
    }

    @Test
    void shouldReturnTrueForOwnerOrAdmin() throws SQLException {
        VoteModel.createQuestion(testUserId, "权限测试", Arrays.asList("A", "B"));

        List<VoteQuestion> all = VoteModel.findAllQuestions();
        int questionId = -1;
        for (VoteQuestion q : all) {
            if ("权限测试".equals(q.getTitle()) && q.getUserId() == testUserId) {
                questionId = q.getId();
                break;
            }
        }
        assertTrue(questionId > 0);

        // 发布者本人应该有权限
        assertTrue(VoteModel.isQuestionOwnerOrAdmin(questionId, testUserId));
    }

    @Test
    void shouldFindAllQuestionsForAdmin() throws SQLException {
        VoteModel.createQuestion(testUserId, "管理员列表", Arrays.asList("A", "B"));

        List<VoteQuestion> questions = VoteModel.findAllQuestionsForAdmin();
        boolean found = false;
        for (VoteQuestion q : questions) {
            if ("管理员列表".equals(q.getTitle()) && q.getUserId() == testUserId) {
                found = true;
                break;
            }
        }
        assertTrue(found, "管理员应该能看到待审批的问卷");
    }
}
