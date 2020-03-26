package com.alibaba.smart.framework.engine.service.command.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.smart.framework.engine.common.util.CollectionUtil;
import com.alibaba.smart.framework.engine.common.util.DateUtil;
import com.alibaba.smart.framework.engine.common.util.MarkDoneUtil;
import com.alibaba.smart.framework.engine.configuration.ConfigurationOption;
import com.alibaba.smart.framework.engine.configuration.IdGenerator;
import com.alibaba.smart.framework.engine.configuration.ProcessEngineConfiguration;
import com.alibaba.smart.framework.engine.configuration.aware.ProcessEngineConfigurationAware;
import com.alibaba.smart.framework.engine.configuration.scanner.AnnotationScanner;
import com.alibaba.smart.framework.engine.constant.AssigneeTypeConstant;
import com.alibaba.smart.framework.engine.constant.RequestMapSpecialKeyConstant;
import com.alibaba.smart.framework.engine.constant.TaskInstanceConstant;
import com.alibaba.smart.framework.engine.exception.ValidationException;
import com.alibaba.smart.framework.engine.extension.annoation.ExtensionBinding;
import com.alibaba.smart.framework.engine.extension.constant.ExtensionConstant;
import com.alibaba.smart.framework.engine.hook.LifeCycleHook;
import com.alibaba.smart.framework.engine.instance.impl.DefaultTaskAssigneeInstance;
import com.alibaba.smart.framework.engine.instance.impl.DefaultTaskInstance;
import com.alibaba.smart.framework.engine.instance.storage.ActivityInstanceStorage;
import com.alibaba.smart.framework.engine.instance.storage.ExecutionInstanceStorage;
import com.alibaba.smart.framework.engine.instance.storage.ProcessInstanceStorage;
import com.alibaba.smart.framework.engine.instance.storage.TaskAssigneeStorage;
import com.alibaba.smart.framework.engine.instance.storage.TaskInstanceStorage;
import com.alibaba.smart.framework.engine.model.instance.ExecutionInstance;
import com.alibaba.smart.framework.engine.model.instance.TaskAssigneeCandidateInstance;
import com.alibaba.smart.framework.engine.model.instance.TaskAssigneeInstance;
import com.alibaba.smart.framework.engine.model.instance.TaskInstance;
import com.alibaba.smart.framework.engine.service.command.ExecutionCommandService;
import com.alibaba.smart.framework.engine.service.command.TaskCommandService;

/**
 * @author 高海军 帝奇  2016.11.11
 */
@ExtensionBinding(group = ExtensionConstant.SERVICE, bindKey = TaskCommandService.class)

public class DefaultTaskCommandService implements TaskCommandService, LifeCycleHook ,
    ProcessEngineConfigurationAware {

    private ProcessInstanceStorage processInstanceStorage;
    private ActivityInstanceStorage activityInstanceStorage;
    private ExecutionInstanceStorage executionInstanceStorage;
    private ExecutionCommandService executionCommandService;
    private ProcessEngineConfiguration processEngineConfiguration;
    private TaskInstanceStorage taskInstanceStorage;
    private TaskAssigneeStorage taskAssigneeStorage;

    @Override
    public void start() {

        AnnotationScanner annotationScanner = this.processEngineConfiguration.getAnnotationScanner();
        this.executionCommandService = annotationScanner.getExtensionPoint(ExtensionConstant.SERVICE,ExecutionCommandService.class);
        this.processInstanceStorage = annotationScanner.getExtensionPoint(ExtensionConstant.COMMON,ProcessInstanceStorage.class);
        this.taskAssigneeStorage = annotationScanner.getExtensionPoint(ExtensionConstant.COMMON,TaskAssigneeStorage.class);
        this.activityInstanceStorage = annotationScanner.getExtensionPoint(ExtensionConstant.COMMON,ActivityInstanceStorage.class);
        this.executionInstanceStorage = annotationScanner.getExtensionPoint(ExtensionConstant.COMMON,ExecutionInstanceStorage.class);
        this.taskInstanceStorage = annotationScanner.getExtensionPoint(ExtensionConstant.COMMON,TaskInstanceStorage.class);

    }

    @Override
    public void stop() {

    }


    @Override
    public void complete(String taskId, Map<String, Object> request, Map<String, Object> response) {


        TaskInstance taskInstance = taskInstanceStorage.find(taskId,processEngineConfiguration );
        MarkDoneUtil.markDoneTaskInstance(taskInstance, TaskInstanceConstant.COMPLETED, TaskInstanceConstant.PENDING,
            request, taskInstanceStorage, processEngineConfiguration);

        executionCommandService.signal(taskInstance.getExecutionInstanceId(), request,response);

    }

    @Override
    public void complete(String taskId, Map<String, Object> request) {
        this.complete(taskId,request,null);
    }

    @Override
    public void complete(String taskId, String userId, Map<String, Object> request) {
        if(null == request){
            request = new HashMap<String, Object>();
        }
        request.put(RequestMapSpecialKeyConstant.TASK_INSTANCE_CLAIM_USER_ID,userId);

        //TUNE check privilege

        complete(  taskId, request);
    }

    @Override
    public void transfer(String taskId, String fromUserId, String toUserId) {

        ConfigurationOption configurationOption = processEngineConfiguration
            .getOptionContainer().get(ConfigurationOption.TRANSFER_ENABLED_OPTION.getId());

        if (!configurationOption.isEnabled()){
            throw new ValidationException("should set TRANSFER_ENABLED_OPTION ");
        }


        List<TaskAssigneeInstance> taskAssigneeInstanceList = taskAssigneeStorage.findList(taskId,processEngineConfiguration );

        if(CollectionUtil.isEmpty(taskAssigneeInstanceList)){
            throw new ValidationException("taskAssigneeInstanceList can't be empty for taskId:"+taskId);
        }

        boolean found = false;

        TaskAssigneeInstance matchedTaskAssigneeInstance = null;
        for (TaskAssigneeInstance taskAssigneeInstance : taskAssigneeInstanceList) {
            if (taskAssigneeInstance.getAssigneeId().equals(fromUserId)){
                found =true;
                matchedTaskAssigneeInstance = taskAssigneeInstance;
                break;
            }
        }

        if(!found){
            throw new ValidationException("No taskAssigneeInstance found  for fromUserId:"+fromUserId);
        }

        if( !AssigneeTypeConstant.USER.equals( matchedTaskAssigneeInstance.getAssigneeType())){
            throw new ValidationException("Illegal  AssigneeType :"+matchedTaskAssigneeInstance.getAssigneeType());
        }

        taskAssigneeStorage.update(matchedTaskAssigneeInstance.getInstanceId(),toUserId,processEngineConfiguration);

    }

    @Override
    public TaskInstance createTask(ExecutionInstance executionInstance, String taskInstanceStatus,Map<String, Object> request) {
    	IdGenerator idGenerator = processEngineConfiguration.getIdGenerator();
    	
        TaskInstance taskInstance = new DefaultTaskInstance();
        taskInstance.setActivityInstanceId(executionInstance.getActivityInstanceId());
        taskInstance.setExecutionInstanceId(executionInstance.getInstanceId());
        taskInstance.setProcessDefinitionActivityId(executionInstance.getProcessDefinitionActivityId());
        taskInstance.setProcessDefinitionIdAndVersion(executionInstance.getProcessDefinitionIdAndVersion());
        taskInstance.setProcessInstanceId(executionInstance.getProcessInstanceId());

        taskInstance.setStatus(taskInstanceStatus);
        String id = idGenerator.getId();
        taskInstance.setInstanceId(id);


        if(null != request){

            String processDefinitionType = (String)request.get(RequestMapSpecialKeyConstant.PROCESS_DEFINITION_TYPE);
            taskInstance.setProcessDefinitionType(processDefinitionType);

            Date startTime = (Date)request.get(RequestMapSpecialKeyConstant.TASK_START_TIME);
            taskInstance.setStartTime(startTime);

            Date completeTime = (Date)request.get(RequestMapSpecialKeyConstant.TASK_COMPLETE_TIME);
            taskInstance.setCompleteTime(completeTime);

            String comment = (String)request.get(RequestMapSpecialKeyConstant.TASK_INSTANCE_COMMENT);
            taskInstance.setComment(comment);

            String extension = (String)request.get(RequestMapSpecialKeyConstant.TASK_INSTANCE_EXTENSION);
            taskInstance.setExtension(extension);

            Integer priority = (Integer)request.get(RequestMapSpecialKeyConstant.TASK_INSTANCE_PRIORITY);
            taskInstance.setPriority(priority);

            String tag = (String)request.get(RequestMapSpecialKeyConstant.TASK_INSTANCE_TAG);
            taskInstance.setTag(tag);

            String title = (String)request.get(RequestMapSpecialKeyConstant.TASK_TITLE);
            taskInstance.setTitle(title);

            String claimUserId = (String)request.get(RequestMapSpecialKeyConstant.CLAIM_USER_ID);
            taskInstance.setClaimUserId(claimUserId);

            Date claimTime = (Date)request.get(RequestMapSpecialKeyConstant.CLAIM_USER_TIME);
            taskInstance.setClaimTime(claimTime);
        }


        taskInstanceStorage.insert(taskInstance,processEngineConfiguration);
        return taskInstance;
    }

    @Override
    public void addTaskAssigneeCandidate(String taskId, TaskAssigneeCandidateInstance taskAssigneeCandidateInstance) {

        TaskInstance taskInstance = taskInstanceStorage.find(taskId,processEngineConfiguration );

        TaskAssigneeInstance taskAssigneeInstance = new DefaultTaskAssigneeInstance();

        taskAssigneeInstance.setTaskInstanceId(taskInstance.getInstanceId());
        taskAssigneeInstance.setProcessInstanceId(taskInstance.getProcessInstanceId());

        taskAssigneeInstance.setAssigneeId(taskAssigneeCandidateInstance.getAssigneeId());
        taskAssigneeInstance.setAssigneeType(taskAssigneeCandidateInstance.getAssigneeType());

        taskAssigneeInstance.setInstanceId(processEngineConfiguration.getIdGenerator().getId());

        Date currentDate = DateUtil.getCurrentDate();
        taskAssigneeInstance.setStartTime(currentDate);
        taskAssigneeInstance.setCompleteTime(currentDate);

        taskAssigneeStorage.insert(taskAssigneeInstance,processEngineConfiguration);


    }

    @Override
    public void removeTaskAssigneeCandidate(String taskId, TaskAssigneeCandidateInstance taskAssigneeCandidateInstance) {


        List<TaskAssigneeInstance> taskAssigneeInstanceList = taskAssigneeStorage.findList(taskId,processEngineConfiguration );

        boolean found = false;
        TaskAssigneeInstance matchedTaskAssigneeInstance = null;
        if(CollectionUtil.isNotEmpty(taskAssigneeInstanceList)){

            for (TaskAssigneeInstance taskAssigneeInstance : taskAssigneeInstanceList) {
                if(taskAssigneeInstance.getAssigneeId().equals(taskAssigneeCandidateInstance.getAssigneeId())
                    && taskAssigneeInstance.getAssigneeType().equals(taskAssigneeCandidateInstance.getAssigneeType())){
                    found = true;
                    matchedTaskAssigneeInstance = taskAssigneeInstance;
                    break;
                }
            }

            if(!found){
                throw new ValidationException("No taskAssigneeCandidateInstance found for "+taskAssigneeCandidateInstance);
            }

            taskAssigneeStorage.remove(matchedTaskAssigneeInstance.getInstanceId(),processEngineConfiguration);


        }


    }

    @Override
    public void markDone(String taskId, Map<String, Object> request) {

        TaskInstance taskInstance = taskInstanceStorage.find(taskId,processEngineConfiguration );
        MarkDoneUtil.markDoneTaskInstance(taskInstance, TaskInstanceConstant.COMPLETED, TaskInstanceConstant.PENDING,
            request, taskInstanceStorage, processEngineConfiguration);

    }





    @Override
    public void setProcessEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }
}
