package org.wso2.carbon.identity.workflow.engine.dto;

/**
 * Action to perform on the task dto.
 */
public class StateDTO {

    /**
     * Actions to perform on an approval task.
     */
    public enum ActionEnum {
        CLAIM, RELEASE, APPROVE, REJECT,
    };

    private ActionEnum action = null;

    /**
     * getAction to perform on the task.
     **/
    public ActionEnum getAction() {

        return action;
    }

    /**
     * Set Action to perform on the task.
     **/
    public void setAction(ActionEnum action) {

        this.action = action;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class StateDTO {\n");
        sb.append("  action: ").append(action).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
