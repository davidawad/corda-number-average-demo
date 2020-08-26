package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.AverageStateContract;
import com.template.contracts.TemplateContract;
import com.template.states.AverageState;
import com.template.states.NumberState;
import com.template.states.TemplateState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class SendAverageFlow extends FlowLogic<SignedTransaction> {

    private final ProgressTracker progressTracker = new ProgressTracker();
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    //private variables
    private Party receiver;
    private Party sender;
    private List<UniqueIdentifier> stateIds;

    public SendAverageFlow(UniqueIdentifier firstLinearId, UniqueIdentifier secondLinearId){
        this.stateIds = new ArrayList<UniqueIdentifier>();
        this.stateIds.add(firstLinearId);
        this.stateIds.add(secondLinearId);
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        this.sender = getOurIdentity();

        // Step 1. Get a reference to the notary service on our network and our key pair.
        // Note: ongoing work to support multiple notary identities is still in progress.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);


        // find state objects for each party's array of integers
        List<NumberState> states = new ArrayList<NumberState>();
        // find owning key for each participant
        List<Party> recipients = new ArrayList<Party>();
        List recipientKeys = new ArrayList<>();
        // collect the full list of numbers
        List<Integer> allNums = new ArrayList<Integer>();

        for(UniqueIdentifier u: stateIds) {
            NumberState state = retrieveNumberState(u);
            states.add(state);
            recipients.add(state.getSender());
            recipientKeys.add(state.getSender().getOwningKey());
            allNums.addAll(state.getNumbers());
        }

        double avg = calculateAverage(allNums);

        // force a sender to be specified in order for partipants to be set.
        receiver = states.get(0).getSender();

        //Compose the State that carries the average to be sent
        final AverageState output = new AverageState(avg, receiver, sender);

        // Step 3. Create a new TransactionBuilder object.
        final TransactionBuilder builder = new TransactionBuilder(notary);

        // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
        builder.addOutputState(output);

        builder.addCommand(new AverageStateContract.Commands.Send(), Arrays.asList(states.get(0).getSender().getOwningKey(), states.get(1).getSender().getOwningKey()));

        // Step 5. Verify and sign it with our KeyPair.
        builder.verify(getServiceHub());
        final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

        // Step 6. Collect the other party's signature using the SignTransactionFlow.
        List<Party> otherParties = recipients.stream().map(el -> (Party)el).collect(Collectors.toList());
        otherParties.remove(getOurIdentity());
        List<FlowSession> sessions = otherParties.stream().map(el -> initiateFlow(el)).collect(Collectors.toList());

        SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

        // Step 7. Assuming no exceptions, we can now finalise the transaction
        return subFlow(new FinalityFlow(stx, sessions));
    }

    public double calculateAverage(List <Integer> marks) {
        Integer sum = 0;
        if(!marks.isEmpty()) {
            for (Integer mark : marks) {
                sum += mark;
            }
            return sum.doubleValue() / marks.size();
        }
        return sum;
    }

    private NumberState retrieveNumberState(UniqueIdentifier linearId) {
        List<UUID> listOfLinearIds = Arrays.asList(linearId.getId());
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);
        Vault.Page results = getServiceHub().getVaultService().queryBy(NumberState.class, queryCriteria);
        StateAndRef inputStateAndRefToSettle = (StateAndRef) results.getStates().get(0);
        NumberState ret = (NumberState) ((StateAndRef) results.getStates().get(0)).getState().getData();
        return ret;
    }
}
