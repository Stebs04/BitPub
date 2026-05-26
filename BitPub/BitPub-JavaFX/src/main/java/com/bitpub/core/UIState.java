package com.bitpub.core;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Encapsulates the state of a UI operation for MVVM binding.
 */
public class UIState<T> {
    public enum Status { IDLE, LOADING, SUCCESS, ERROR }

    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.IDLE);
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final ObjectProperty<T> data = new SimpleObjectProperty<>(null);

    public Status getStatus() { return status.get(); }
    public ObjectProperty<Status> statusProperty() { return status; }
    
    public String getErrorMessage() { return errorMessage.get(); }
    public StringProperty errorMessageProperty() { return errorMessage; }

    public T getData() { return data.get(); }
    public ObjectProperty<T> dataProperty() { return data; }

    public void setLoading() {
        Platform.runLater(() -> {
            this.status.set(Status.LOADING);
            this.errorMessage.set("");
        });
    }

    public void setSuccess(T data) {
        Platform.runLater(() -> {
            this.data.set(data);
            this.status.set(Status.SUCCESS);
            this.errorMessage.set("");
        });
    }

    public void setError(String message) {
        Platform.runLater(() -> {
            this.errorMessage.set(message);
            this.status.set(Status.ERROR);
        });
    }
    
    public void setIdle() {
        Platform.runLater(() -> {
            this.status.set(Status.IDLE);
        });
    }
}
