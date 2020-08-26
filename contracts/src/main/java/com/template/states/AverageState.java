package com.template.states;

import com.template.contracts.AverageStateContract;
import com.template.contracts.TemplateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CordaSerializable
@BelongsToContract(AverageStateContract.class)
public class AverageState implements ContractState {

    //private variables
    private double avg;
    private Party recipient;
    private Party sender;

    @ConstructorForDeserialization
    public AverageState(double avg, Party recipient, Party sender) {
        this.avg = avg;
        this.recipient = recipient;
        this.sender = sender;
    }

    //getters
    public double getAvg() { return avg; }
    public Party getSender() { return sender; }
    public Party getRecipient() { return sender; }


    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @Override
    //@NotNull
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, recipient);
    }
}
