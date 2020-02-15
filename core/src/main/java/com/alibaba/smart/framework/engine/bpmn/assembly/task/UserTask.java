package com.alibaba.smart.framework.engine.bpmn.assembly.task;

import javax.xml.namespace.QName;

import com.alibaba.smart.framework.engine.bpmn.assembly.multi.instance.MultiInstanceLoopCharacteristics;
import com.alibaba.smart.framework.engine.bpmn.constant.BpmnNameSpaceConstant;
import com.alibaba.smart.framework.engine.model.assembly.impl.AbstractTask;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 高海军 帝奇 Apr 14, 2016 2:50:20 PM
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserTask extends AbstractTask {

    private static final long serialVersionUID = 2241766485621486315L;


    public final static QName type = new QName(BpmnNameSpaceConstant.NAME_SPACE, "userTask");

    private MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics;

    @Override
    public String toString() {
        return super.getId();
    }
}
