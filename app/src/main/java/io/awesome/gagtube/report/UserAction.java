package io.awesome.gagtube.report;

/**
 * The user actions that can cause an error.
 */
public enum UserAction {
    USER_REPORT("USER REPORT"),
    UI_ERROR("UI ERROR"),
    SUBSCRIPTION("SUBSCRIPTION"),
    LOAD_IMAGE("LOAD IMAGE"),
    SOMETHING_ELSE("SOMETHING"),
    SEARCHED("SEARCHED"),
    GET_SUGGESTIONS("GET SUGGESTIONS"),
    REQUESTED_STREAM("REQUESTED STREAM"),
    REQUESTED_CHANNEL("REQUESTED CHANNEL"),
    REQUESTED_PLAYLIST("REQUESTED PLAYLIST"),
    REQUESTED_MAIN_CONTENT("REQUESTED MAIN CONTENT"),
    DELETE_FROM_HISTORY("DELETE FROM HISTORY"),
    DOWNLOAD_POSTPROCESSING("download post-processing"),
    DOWNLOAD_FAILED("download failed");

    private final String message;

    UserAction(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
