package com.alibaba.smart.framework.engine.service.command;

import java.util.Map;

import com.alibaba.smart.framework.engine.model.instance.TaskAssigneeCandidateInstance;

/**
 * 主要负责人工任务处理服务。
 *
 * @author 高海军 帝奇  2016.11.11
 * @author ettear 2016.04.13
 */
public interface TaskCommandService {

    void complete(String taskId, Map<String, Object> request);

    void complete(String taskId, String userId, Map<String, Object> request);

    void complete(String taskId,  Map<String, Object> request, Map<String, Object> response);

    void transfer(String taskId, String fromUserId, String toUserId);

    void removeTaskAssigneeCandidate(String taskId, TaskAssigneeCandidateInstance taskAssigneeCandidateInstance);

    void addTaskAssigneeCandidate(String taskId, TaskAssigneeCandidateInstance taskAssigneeCandidateInstance);

    void markDone(String taskId, Map<String, Object> request);

}
