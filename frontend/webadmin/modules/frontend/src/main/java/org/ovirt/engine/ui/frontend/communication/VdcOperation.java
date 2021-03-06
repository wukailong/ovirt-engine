package org.ovirt.engine.ui.frontend.communication;

import java.io.Serializable;

import org.ovirt.engine.core.common.action.VdcActionType;

/**
 * This class represents a single operation, which is either a query or an action.
 * @param <T> The type of operation.
 * @param <P> The parameter type for the operation.
 */
@SuppressWarnings("rawtypes")
public class VdcOperation<T, P> implements Serializable {
    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = -6569717023385458462L;

    /**
     * The actual operation, can be either {@code VdcQueryType} or {@code VdcActionType}.
     */
    private final T operationType;

    /**
     * The parameter of the operation.
     */
    private final P parameter;

    /**
     * The callback to call when the operation is finished.
     */
    private final VdcOperationCallback<?, ?> operationCallback;

    /**
     * The source object if we used a copy constructor to build this object.
     */
    private final VdcOperation<T, P> source;

    /**
     * Determines if the operation is public or not.
     */
    private final boolean isPublic;

    /**
     * Private constructor that initializes the final members.
     * @param operation The operation.
     * @param param The parameter for the operation.
     * @param callback The callback to call when the operation is finished.
     * @param sourceOperation If we cloned an operation this is the source it came from.
     * @param isPublicOperation Determines if this operation should be public or not.
     */
    private VdcOperation(final T operation, final P param, final VdcOperationCallback<?, ?> callback,
            final VdcOperation<T, P> sourceOperation, final boolean isPublicOperation) {
        this.operationType = operation;
        this.parameter = param;
        this.operationCallback = callback;
        this.source = sourceOperation;
        this.isPublic = isPublicOperation;
    }

    /**
     * Copy constructor that allows for a different callback.
     * @param sourceOperation The source {@code VdcOperation} object.
     * @param callback The new callback method.
     */
    public VdcOperation(final VdcOperation<T, P> sourceOperation, final VdcOperationCallback<?, ?> callback) {
        this(sourceOperation.getOperation(), sourceOperation.getParameter(), callback, sourceOperation,
                sourceOperation.isPublic());
    }

    /**
     * Constructor.
     * @param operation The operation to set.
     * @param operationParameter The parameter for the operation.
     * @param callback The callback to call when the operation is finished.
     */
    public VdcOperation(final T operation, final P operationParameter, final VdcOperationCallback<?, ?> callback) {
        this(operation, operationParameter, callback, null, false);
    }

    /**
     * Constructor.
     * @param operation The operation to set.
     * @param operationParameter The parameter for the operation.
     * @param isPublicOperation determines if an operation is public or not. Only applicable for queries, not actions.
     * @param callback The callback to call when the operation is finished.
     */
    public VdcOperation(final T operation, final P operationParameter, final boolean isPublicOperation,
            final VdcOperationCallback<?, ?> callback) {
        this(operation, operationParameter, callback, null, isPublicOperation);
    }

    /**
     * Getter.
     * @return The action associated with the operation
     */
    public T getOperation() {
        return operationType;
    }

    /**
     * Getter.
     * @return The action parameter of the operation
     */
    public P getParameter() {
        return parameter;
    }

    /**
     * Getter.
     * @return The callback to call when the operation completes.
     */
    public VdcOperationCallback getCallback() {
        return operationCallback;
    }

    /**
     * Check if duplicates of this are allowed. If the operation wraps an action
     * duplicates are allowed.
     * @return True if duplicates are allowed, false otherwise.
     */
    public boolean allowDuplicates() {
        return operationType instanceof VdcActionType;
    }

    /**
     * Returns the number of times this operation has been copied using the copy constructor.
     * If this is the original returns 1.
     * @return The copy count.
     */
    public int getCopyCount() {
        int result = 1;
        if (source != null) {
            result += source.getCopyCount();
        }
        return result;
    }

    /**
     * Get source object if it exists.
     * @return The source object.
     */
    public VdcOperation<T, P> getSource() {
        return source;
    }

    /**
     * Returns true if the operation does not require the user to be logged in.
     * @return {@code true} if the operation is public, {@code false} otherwise.
     */
    public boolean isPublic() {
        return isPublic;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((operationType == null) ? 0 : operationType.hashCode());
        result = prime * result + ((parameter == null) ? 0 : parameter.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        boolean result = false;
        @SuppressWarnings("unchecked")
        VdcOperation<T, P> otherOperation = (VdcOperation<T, P>) other;
        result = operationType.equals(otherOperation.getOperation())
                    && parameter.equals(otherOperation.getParameter());
        return result;
    }
}
