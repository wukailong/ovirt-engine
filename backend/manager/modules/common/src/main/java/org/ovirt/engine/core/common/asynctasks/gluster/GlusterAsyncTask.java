package org.ovirt.engine.core.common.asynctasks.gluster;

import java.io.Serializable;

import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.Guid;

public class GlusterAsyncTask implements Serializable{

    private static final long serialVersionUID = 5165089908032934194L;

    private Guid taskId;
    private JobExecutionStatus status;
    private GlusterTaskType type;
    private String message;
    private Guid stepId;
    private GlusterTaskParameters taskParameters;

    public GlusterAsyncTask(){

    }

    public Guid getTaskId() {
        return taskId;
    }
    public void setTaskId(Guid taskId) {
        this.taskId = taskId;
    }
    public JobExecutionStatus getStatus() {
        return status;
    }
    public void setStatus(JobExecutionStatus status) {
        this.status = status;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public GlusterTaskType getType() {
        return type;
    }
    public void setType(GlusterTaskType type) {
        this.type = type;
    }

    public Guid getStepId() {
        return stepId;
    }

    public void setStepId(Guid stepId) {
        this.stepId = stepId;
    }

    public GlusterTaskParameters getTaskParameters() {
        return taskParameters;
    }

    public void setTaskParameters(GlusterTaskParameters taskParameters) {
        this.taskParameters = taskParameters;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((taskId == null) ? 0 : taskId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((stepId == null) ? 0 : stepId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (obj == null){
            return false;
        }
        if (getClass() != obj.getClass()){
            return false;
        }
        GlusterAsyncTask other = (GlusterAsyncTask) obj;
        return (ObjectUtils.objectsEqual(message, other.message)
                && ObjectUtils.objectsEqual(taskId, other.taskId)
                && type == other.type
                && status == other.status
                && ObjectUtils.objectsEqual(stepId, other.stepId));
    }

}
