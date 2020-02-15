package com.alibaba.smart.framework.engine.bpmn.assembly.multi.instance;

import javax.xml.namespace.QName;

import com.alibaba.smart.framework.engine.bpmn.constant.BpmnNameSpaceConstant;
import com.alibaba.smart.framework.engine.model.assembly.NoneIdBasedElement;

/**
 * @author ettear
 * Created by ettear on 16/10/2017.
 */
public class InputDataItem implements NoneIdBasedElement {
    public final static QName type = new QName(BpmnNameSpaceConstant.NAME_SPACE, "inputDataItem");

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
