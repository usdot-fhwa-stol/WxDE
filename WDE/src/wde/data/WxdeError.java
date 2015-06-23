package wde.data;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author blackch
 */
@XmlRootElement
public class WxdeError {

    private String errorNumber;
    private String errorTitle;
    private String errorMessage;

    public String getErrorNumber() {
        return errorNumber;
    }

    public void setErrorNumber(String errorNumber) {
        this.errorNumber = errorNumber;
    }

    public String getErrorTitle() {
        return errorTitle;
    }

    public void setErrorTitle(String errorTitle) {
        this.errorTitle = errorTitle;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
