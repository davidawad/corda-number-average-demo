package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.NumberStateContract;
import com.template.contracts.TemplateContract;
import com.template.states.AverageState;
import com.template.states.NumberState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.*;
import java.util.stream.Collectors;

@InitiatingFlow
@StartableByRPC
public class SendNumberListFlow extends FlowLogic<SignedTransaction> {

    // We will not use these ProgressTracker for this Hello-World sample
    private final ProgressTracker progressTracker = new ProgressTracker();
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    //private variables
    private Party recipient;
    private Party sender;
    private int listSize;
    private final int NUMBER_BOUND = 1000;

    //public constructor
    public SendNumberListFlow(Party recipient, int listSize) {
        this.recipient = recipient;
        this.listSize = listSize;
    }

    public SendNumberListFlow(Party recipient) {
        this(recipient, 5);
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        this.sender = getOurIdentity();

        // Step 1. Get a reference to the notary service on our network and our key pair.
        // Note: ongoing work to support multiple notary identities is still in progress.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        List<Integer> nums = generateNRandomNumbers(listSize);

        //Compose the State that carries the average to be sent
        final NumberState output = new NumberState(this.sender, this.recipient, nums);

        // Step 3. Create a new TransactionBuilder object.
        final TransactionBuilder builder = new TransactionBuilder(notary);

        // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
        builder.addOutputState(output);

        // collect recipients
        builder.addCommand(new NumberStateContract.Commands.Send(), Arrays.asList(this.sender.getOwningKey(), this.recipient.getOwningKey()));

        // Step 5. Verify and sign it with our KeyPair.
        builder.verify(getServiceHub());
        final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

        // Step 6. Collect the other party's signature using the SignTransactionFlow.
        List<Party> otherParties = output.getParticipants().stream().map(el -> (Party)el).collect(Collectors.toList());
        otherParties.remove(getOurIdentity());
        List<FlowSession> sessions = otherParties.stream().map(el -> initiateFlow(el)).collect(Collectors.toList());

        SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

        // Step 7. Assuming no exceptions, we can now finalise the transaction
        return subFlow(new FinalityFlow(stx, sessions));
    }

    // generates n random numbers
    private List<Integer> generateNRandomNumbers(int n) {
        if (n < 1) {
            //complain
        }
        Random randNum = new Random();
        List<Integer> list = new ArrayList<Integer>();

        while (list.size() < n) {
            list.add(randNum.nextInt(NUMBER_BOUND)+1); // add 1 to make sure 0 is never added.
        }

        return list;
    }

}
