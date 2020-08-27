package com.template;

import com.google.common.collect.ImmutableList;
import com.template.flows.Responder;
import com.template.flows.SendAverageFlow;
import com.template.flows.SendNumberListFlow;
import com.template.states.AverageState;
import com.template.states.NumberState;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;


import net.corda.core.concurrent.CordaFuture;
import net.corda.core.transactions.SignedTransaction;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SendNumberListFlowTests {

    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows"))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);

        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedMockNode node : ImmutableList.of(a, b, c)) {
            node.registerInitiatedFlow(Responder.class);
        }
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

//    @Test
//    public void dummyTest() {
//
//    }

    // test that we can get the output of our transaction on here.
    @Test
    public void stateBasicPropertyTests() throws ExecutionException, InterruptedException {

        SignedTransaction tx1;
        SignedTransaction tx2;
        SignedTransaction tx3;

        CordaFuture<SignedTransaction> first5 = a.startFlow(new SendNumberListFlow(c.getInfo().getLegalIdentities().get(0)));
        CordaFuture<SignedTransaction> next5 = b.startFlow(new SendNumberListFlow(c.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();

        tx1 = first5.get();
        tx2 = next5.get();

        NumberState firstState = tx1.getTx().outputsOfType(NumberState.class).get(0);
        NumberState secondState = tx2.getTx().outputsOfType(NumberState.class).get(0);

        assert(tx1.getTx().getInputs().isEmpty()); // numberListFlows have no inputs
        assert(tx2.getTx().getInputs().isEmpty()); // numberListFlows have no inputs

        // confirm a bunch of details about this game state.
        assert(firstState.getNumbers().size() == 5);  // confirm size of lists generated
        assert(secondState.getNumbers().size() == 5); // confirm size of lists generated

        System.out.println("Node A sent the following numbers :");
        System.out.println(firstState.getNumbers());
        System.out.println("Node B sent the following numbers :");
        System.out.println(secondState.getNumbers());

        List<Integer> combinedList = firstState.getNumbers();
        combinedList.addAll(secondState.getNumbers());

        // pass our previous states as inputs to the SendAverageFlow
        SendAverageFlow saf = new SendAverageFlow(firstState.getLinearId(), secondState.getLinearId());
        CordaFuture<SignedTransaction> avgFlowSend = c.startFlow(saf);

        // bad practice; for convenience assume the code to compute average works
        final double actualAverage = saf.calculateAverage(combinedList);

        // get our transaction linearid
        network.runNetwork();

        tx3 = avgFlowSend.get();

        AverageState thirdState = tx3.getTx().outputsOfType(AverageState.class).get(0);

        System.out.println("Node received the following list of numbers :");
        System.out.println(combinedList);
        System.out.println("Reached end of tests, final average is " + Double.toString(actualAverage));

        //assert(!tx3.getTx().getInputs().size() == 2); // computing averages requires two input states
        assert(thirdState.getAvg() == actualAverage);
        
    }

}
