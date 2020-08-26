package com.template.states;

import com.template.contracts.AverageStateContract;
import com.template.contracts.NumberStateContract;
import com.template.contracts.TemplateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CordaSerializable
@BelongsToContract(NumberStateContract.class)
public class NumberState implements ContractState, LinearState {

    //private variables
    private Party sender;
    private Party recipient;
    private List<Integer> numbers = new ArrayList<Integer>();
    private UniqueIdentifier linearId;


    @ConstructorForDeserialization
    public NumberState(Party sender, Party recipient, List<Integer> numbers, UniqueIdentifier linearId) {
        this.numbers = numbers;
        this.sender = sender;
        this.recipient = recipient;
        this.linearId = linearId;
    }


    public NumberState(Party sender, Party recipient, List<Integer> numbers) {
        this(sender, recipient, numbers, new UniqueIdentifier());
    }


    //getters
    public Party getSender() { return sender; }
    public List<Integer> getNumbers() { return numbers; }

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, recipient);
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}
