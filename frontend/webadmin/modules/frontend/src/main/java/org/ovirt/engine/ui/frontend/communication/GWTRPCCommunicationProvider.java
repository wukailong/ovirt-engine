package org.ovirt.engine.ui.frontend.communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.action.LoginUserParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.DbUser;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.gwtservices.GenericApiGWTServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

/**
 * This class is an implementation of the {@code CommunicationProvider} using the GWT-RPC mechanism.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class GWTRPCCommunicationProvider implements CommunicationProvider {
    /**
     * GWT RPC service.
     */
    private GenericApiGWTServiceAsync service = null;

    /**
     * Get the GWT RPC service.
     * @return instance of the GWT RPC service.
     */
    private GenericApiGWTServiceAsync getService() {
        return service;
    }

    /**
     * Constructor.
     * @param asyncService GWT RPC service.
     */
    @Inject
    public GWTRPCCommunicationProvider(final GenericApiGWTServiceAsync asyncService) {
        service = asyncService;
    }

    /**
     * Transmit a single operation, with the expectation of a single result object.
     * @param operation The operation to execute.
     */
    void transmitOperation(final VdcOperation<?, ?> operation) {
        // Figure out if this is an action or a query.
        if (operation.getOperation() instanceof VdcActionType) {
            // Action
            runAction(operation);
        } else {
            if (operation.isPublic()) {
                runPublicQuery(operation);
            } else {
                runQuery(operation);
            }
        }
    }

    /**
     * Run a query that does not require the user to be logged in.
     * @param operation The operation to run.
     */
    private void runPublicQuery(final VdcOperation<?, ?> operation) {
        getService().RunPublicQuery((VdcQueryType) operation.getOperation(),
                (VdcQueryParametersBase) operation.getParameter(), new AsyncCallback<VdcQueryReturnValue>() {
            @Override
            public void onFailure(final Throwable exception) {
                operation.getCallback().onFailure(operation, exception);
            }

            @Override
            public void onSuccess(final VdcQueryReturnValue result) {
                operation.getCallback().onSuccess(operation, result);
            }
        });
    }

    /**
     * Run a query that requires the user to be logged in.
     * @param operation The operation to run.
     */
    private void runQuery(final VdcOperation<?, ?> operation) {
        getService().RunQuery((VdcQueryType) operation.getOperation(),
                (VdcQueryParametersBase) operation.getParameter(), new AsyncCallback<VdcQueryReturnValue>() {
            @Override
            public void onFailure(final Throwable exception) {
                operation.getCallback().onFailure(operation, exception);
            }

            @Override
            public void onSuccess(final VdcQueryReturnValue result) {
                operation.getCallback().onSuccess(operation, result);
            }
        });
    }

    /**
     * Run an action on the {@code GenericApiGWTServiceAsync} service.
     * @param operation The operation to run.
     */
    private void runAction(final VdcOperation<?, ?> operation) {
        getService().RunAction((VdcActionType) operation.getOperation(),
                (VdcActionParametersBase) operation.getParameter(), new AsyncCallback<VdcReturnValueBase>() {
            @Override
            public void onFailure(final Throwable exception) {
                operation.getCallback().onFailure(operation, exception);
            }

            @Override
            public void onSuccess(final VdcReturnValueBase result) {
                operation.getCallback().onSuccess(operation, result);
            }
        });
    }

    @Override
    public void transmitOperationList(final List<VdcOperation<?, ?>> operations) {
        // Operations can be either actions or queries. Both require different handling so lets
        // Split them out into two lists so we can process them independently.
        List<VdcOperation<?, ?>> queriesList = new ArrayList<VdcOperation<?, ?>>();
        Map<VdcActionType, List<VdcOperation<?, ?>>> actionsMap =
                new HashMap<VdcActionType, List<VdcOperation<?, ?>>>();
        for (VdcOperation<?, ?> operation: operations) {
            if (operation.getOperation() instanceof VdcActionType) {
                List<VdcOperation<?, ?>> actionsList = actionsMap.get(operation.getOperation());
                if (actionsList == null) {
                    actionsList = new ArrayList<VdcOperation<?, ?>>();
                    actionsMap.put((VdcActionType) operation.getOperation(), actionsList);
                }
                actionsList.add(operation);
            } else {
                queriesList.add(operation);
            }
        }
        if (!actionsMap.isEmpty()) {
            // We have some actions, call method to send actions.
            transmitMultipleActions(actionsMap);
        }
        if (!queriesList.isEmpty()) {
            // We have some queries, call method to send queries.
            transmitMultipleQueries(queriesList);
        }
    }

    /**
     * Call the back-end with either RunMultipleQueries or RunQuery based on the size of the queriesList.
     * When the query(ies) complete call the appropriate callback(s).
     * @param queriesList The list of queries.
     */
    private void transmitMultipleQueries(final List<VdcOperation<?, ?>> queriesList) {
        if (queriesList.size() > 1 || (queriesList.size() == 1
                && queriesList.get(0).getCallback() instanceof VdcOperationCallbackList)) {
            List<VdcQueryType> queryTypes = new ArrayList<VdcQueryType>();
            List<VdcQueryParametersBase> parameters = new ArrayList<VdcQueryParametersBase>();
            for (VdcOperation<?, ?> operation: new ArrayList<VdcOperation<?, ?>>(queriesList)) {
                if (operation.isPublic()) {
                    queriesList.remove(operation);
                    runPublicQuery(operation);
                } else {
                    queryTypes.add((VdcQueryType) operation.getOperation());
                    parameters.add((VdcQueryParametersBase) operation.getParameter());
                }
            }
            getService().RunMultipleQueries((ArrayList<VdcQueryType>) queryTypes,
                    (ArrayList<VdcQueryParametersBase>) parameters,
                    new AsyncCallback<ArrayList<VdcQueryReturnValue>>() {
                @Override
                public void onFailure(final Throwable exception) {
                    Map<VdcOperationCallback<?, ?>, List<VdcOperation<?, ?>>> callbackMap = getCallbackMap(queriesList);
                    for (VdcOperationCallback<?, ?> callback: callbackMap.keySet()) {
                        if (callback instanceof VdcOperationCallbackList) {
                            ((VdcOperationCallbackList) callback).onFailure(callbackMap.get(callback), exception);
                        } else {
                            ((VdcOperationCallback) callback).onFailure(callbackMap.get(callback).get(0), exception);
                        }
                    }
                }

                @Override
                public void onSuccess(final ArrayList<VdcQueryReturnValue> result) {
                    Map<VdcOperationCallback<?, ?>, List<VdcOperation<?, ?>>> callbackMap = getCallbackMap(queriesList);
                    for (VdcOperationCallback<?, ?> callback: callbackMap.keySet()) {
                        List<VdcQueryReturnValue> queryResult = (List<VdcQueryReturnValue>) getOperationResult(
                                callbackMap.get(callback), queriesList, result);
                        if (callback instanceof VdcOperationCallbackList) {
                            ((VdcOperationCallbackList) callback).onSuccess(callbackMap.get(callback), queryResult);
                        } else {
                            ((VdcOperationCallback) callback).onSuccess(callbackMap.get(callback).get(0),
                                    queryResult.get(0));
                        }
                    }
                }
            });
        } else {
            transmitOperation(queriesList.get(0));
        }
    }

    /**
     * Call the back-end using either RunAction or RunMultiple actions based on the fact that the map being
     * passed is keyed on the {@code VdcActionType} this allows us to determine if there are one or more actions
     * pending per action type.
     * When the action completes call back the appropriate callback methods with the appropriate results.
     * @param actions A {@code Map} of {@code VdcActionType}s with a list of operations associated with that
     * type
     */
    private void transmitMultipleActions(final Map<VdcActionType, List<VdcOperation<?, ?>>> actions) {
        for (final VdcActionType actionType: actions.keySet()) {
            List<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();
            final List<VdcOperation<?, ?>> allActionOperations = actions.get(actionType);
            for (VdcOperation<?, ?> operation: allActionOperations) {
                parameters.add((VdcActionParametersBase) operation.getParameter());
            }
            if (parameters.size() > 1 || (allActionOperations.size() == 1
                    && allActionOperations.get(0).getCallback() instanceof VdcOperationCallbackList)) {
                getService().RunMultipleActions(actionType, (ArrayList<VdcActionParametersBase>) parameters, false,
                        new AsyncCallback<ArrayList<VdcReturnValueBase>>() {

                    @Override
                    public void onFailure(final Throwable exception) {
                        Map<VdcOperationCallback<?, ?>, List<VdcOperation<?, ?>>> callbackMap =
                                getCallbackMap(actions.get(actionType));
                        for (VdcOperationCallback<?, ?> callback: callbackMap.keySet()) {
                            if (callback instanceof VdcOperationCallbackList) {
                                ((VdcOperationCallbackList) callback).onFailure(callbackMap.get(callback), exception);
                            } else {
                                ((VdcOperationCallback) callback).onFailure(callbackMap.get(callback).get(0),
                                        exception);
                            }
                        }
                    }

                    @Override
                    public void onSuccess(final ArrayList<VdcReturnValueBase> result) {
                        Map<VdcOperationCallback<?, ?>, List<VdcOperation<?, ?>>> callbackMap =
                                getCallbackMap(actions.get(actionType));
                        for (VdcOperationCallback<?, ?> callback: callbackMap.keySet()) {
                            List<VdcReturnValueBase> actionResult = (List<VdcReturnValueBase>)
                                    getOperationResult(callbackMap.get(callback), allActionOperations, result);
                            if (callback instanceof VdcOperationCallbackList) {
                                ((VdcOperationCallbackList) callback).onSuccess(callbackMap.get(callback),
                                        actionResult);
                            } else {
                                ((VdcOperationCallback) callback).onSuccess(callbackMap.get(callback).get(0),
                                        actionResult.get(0));
                            }
                        }
                    }

                });
            } else {
                transmitOperation(actions.get(actionType).get(0));
            }
        }
    }

    /**
     * Map operations by callback, so we can properly call a single callback for all related operations.
     * @param operationList The list of operations to determine the map for.
     * @return A Map of operations keyed by the callback.
     */
    private Map<VdcOperationCallback<?, ?>, List<VdcOperation<?, ?>>> getCallbackMap(
            final List<VdcOperation<?, ?>> operationList) {
        Map<VdcOperationCallback<?, ?>, List<VdcOperation<?, ?>>> callbackMap =
                new HashMap<VdcOperationCallback<?, ?>, List<VdcOperation<?, ?>>>();
        for (VdcOperation<?, ?> operation: operationList) {
            List<VdcOperation<?, ?>> operationsByCallback = callbackMap.get(operation.getCallback());
            if (operationsByCallback == null) {
                operationsByCallback = new ArrayList<VdcOperation<?, ?>>();
                callbackMap.put(operation.getCallback(), operationsByCallback);
            }
            operationsByCallback.add(operation);
        }
        return callbackMap;
    }

    /**
     * Build a results list that maps 1 to 1 with the operationList.
     * allOperations and allResults have the same index, so we look up the index of the operations we want in all
     * operations, then retrieve the result that matches that.
     * @param operationList The list of operations we want to compare to.
     * @param allOperations The list of all operations that we want to get indexes from.
     * @param allResults The list of results that map 1 to 1 with allOperations to get the results from.
     * @return A {@code List} of return values that maps 1 to 1 with operationList.
     */
    List<?> getOperationResult(final List<VdcOperation<?, ?>> operationList,
            final List<VdcOperation<?, ?>> allOperations, final List<?> allResults) {
        List result = new ArrayList();
        for (VdcOperation<?, ?> operation: operationList) {
            int index = allOperations.indexOf(operation);
            if (index > -1 && index < allResults.size()) {
                result.add(allResults.get(index));
            }
        }
        return result;
    }

    /**
     * Log in the user.
     * @param loginOperation The login operation.
     */
    @Override
    public void login(final VdcOperation<VdcActionType, LoginUserParameters> loginOperation) {
        getService().Login(loginOperation.getParameter().getUserName(), loginOperation.getParameter().getUserPassword(),
                loginOperation.getParameter().getDomain(), loginOperation.getOperation(),
                new AsyncCallback<VdcReturnValueBase>() {
            @Override
            public void onSuccess(final VdcReturnValueBase result) {
                loginOperation.getCallback().onSuccess(loginOperation, result);
            }

            @Override
            public void onFailure(final Throwable caught) {
                loginOperation.getCallback().onFailure(loginOperation, caught);
            }
        });
    }

    /**
     * Log the user out.
     * @param userObject The object containing the user information.
     * @param callback The callback to call when the operation is complete.
     */
    @Override
    public void logout(final Object userObject, final UserCallback callback) {
        getService().logOff((DbUser) userObject, new AsyncCallback<VdcReturnValueBase>() {
            @Override
            public void onSuccess(final VdcReturnValueBase result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(final Throwable caught) {
                callback.onFailure(caught);
            }
        });
    }
}
