Running the demos
=================

The repository contains a small number of demo programs that run two-node networks, demonstrating functionality developed
so far. We have:

1. The trader demo, which shows a delivery-vs-payment atomic swap of commercial paper for cash. You can learn more about
   how this works in :doc:`protocol-state-machines`.
2. The IRS demo, which shows two nodes establishing an interest rate swap between them and performing fixings with a
   rates oracle, all driven via the HTTP API.
3. The IRS demo web interface - a web interface to the IRS demo.
4. The attachment demo, which demonstrates uploading attachments to nodes.
5. The SIMM valuation demo, a large demo which shows two nodes agreeing on a portfolio and valuing the initial margin
   using the Standard Initial Margin Model.
6. The Bank of Corda demo, which demonstrates a node acting as an issuer of assets (the Bank of Corda) and remote client
    applications requesting issuance (via RPC, HTTP) of some cash on behalf of a node called Big Corporation

.. note:: If any demos don't work please jump on our mailing list and let us know.

The demos can be run either from the command line, or from inside IntelliJ. Running from the command line is
recommended if you are just wanting to see them run, using IntelliJ can be helpful if you want to debug or
develop the demos themselves.

Trader demo
-----------

This demo brings up three nodes: Bank A, Bank B and a notary/network map node that they both use. Bank A will
be the buyer, and self-issues some cash in order to acquire the commercial paper from Bank B, the seller.

To run from the command line:

1. Run ``./gradlew samples:trader-demo:deployNodes`` to create a set of configs and installs under ``samples/trader-demo/build/nodes``
2. Run ``./samples/trader-demo/build/nodes/runnodes`` (or ``runnodes.bat`` on Windows) to open up three new terminals with the three nodes.
3. Run ``./gradlew samples:trader-demo:runBuyer`` to set up the buyer node with some self-issued cash. This step
   is not expected to print much.
4. Run ``./gradlew samples:trader-demo:runSeller`` to trigger the transaction. You can see both sides of the
   trade print their progress and final transaction state in the bank node tabs/windows.

To run from IntelliJ:

1. Open the Corda project in IntelliJ and run the "Install" configuration
2. Open the Corda samples project in IntelliJ and run the "Trader Demo: Run Nodes" configuration
3. Run "Trader Demo: Run Buyer"
4. Run "Trader Demo: Run Seller"

In the "Trader Demo: Run Nodes" windows you should see some log lines scroll past, and within a few seconds the messages
"Purchase complete - we are a happy customer!" and "Sale completed - we have a happy customer!" should be printed.

IRS demo
--------

This demo brings up three nodes: Bank A, Bank B and a node that runs a notary, a network map and an interest rates
oracle together. The two banks agree on an interest rate swap, and then do regular fixings of the deal as the time
on a simulated clock passes.

To run from the command line:

1. Run ``./gradlew samples:irs-demo:deployNodes samples:irs-demo:installDist`` to install configs and a command line tool under ``samples/irs-demo/build``.
2. Change to the ``samples/irs-demo/build`` directory.
3. Run ``./nodes/runnodes`` (or ``runnodes.bat`` on Windows) to open up three new terminals with the three nodes.
4. Run ``./install/irs-demo/bin/irs-demo --role UploadRates`` (or use ``irs-demo.bat`` on Windows). You should see a
   message be printed to the first node (the notary/oracle/network map node) saying that it has accepted some new
   interest rates.
5. Now run ``irs-demo`` as in step 4, but this time with ``--role Trade 1``. The number is a trade ID. You should
   see lots of activity as the nodes set up the deal, notarise it, get it signed by the oracle and so on.
6. Now run ``irs-demo --role Date 2016-12-12`` to roll the simulated clock forward and see some fixings take place.

To run from IntelliJ:

1. Open the Corda project in IntelliJ and run the "Install" configuration
2. Open the Corda samples project in IntelliJ and run the "IRS Demo: Run Nodes" configuration
3. Run "IRS Demo: Run Upload Rates" to upload rates to the oracle.
4. Run "IRS Demo: Run Trade" to have nodes agree on a trade.
5. Run "IRS Demo: Run Date Change" to run the fixings.

In the "IRS Demo: Run Nodes" window you'll see a lot of activity when you run the trade and when you run the date change.
The date change rolls the clock forwards and causes the nodes to agree on the fixings over a period.

There is also an web app as part of this demo. To use this set up the trades and then navigate to
http://localhost:10005/web/irsdemo and http://localhost:10005/web/irsdemo to see both node's view of the ledger.

To use the demos click the "Create Deal" button, fill in the form, then click the "Submit" button. Now you will be
able to use the time controls at the top left of the home page to run the fixings. Click any individual trade in the
blotter to view it.

Attachment demo
---------------

This demo brings up three nodes, and sends a transaction containing an attachment from one to the other. To run
it from the command line (recommended for Mac/UNIX users!):

1. Run ``./gradlew samples:attachment-demo:deployNodes`` to create a set of configs and installs under ``samples/attachment-demo/build/nodes``
2. Run ``./samples/attachment-demo/build/nodes/runnodes`` to open up three new terminal tabs/windows with the three nodes.
3. Run ``./gradlew samples:attachment-demo:runRecipient``, which will block waiting for something to happen.
4. Run ``./gradlew samples:attachment-demo:runSender`` in another terminal window to trigger the transaction.
   Now look at the other windows to see the output of the demo.

Or you can run them from inside IntelliJ, but when done this way, all the node output is printed to a single console.

1. Open the Corda project in IntelliJ and run the "Install" configuration
2. Open the Corda samples project in IntelliJ and run the "Attachment Demo: Run Nodes" configuration
3. Run "Attachment Demo: Run Recipient" - this waits for a trade to start
4. Run "Attachment Demo: Run Sender" - sends the attachment

In the "Attachment Demo: Run Nodes" window you should see some log lines scroll past, and within a few seconds the
message "File received - we're happy!" should be printed.

SIMM and Portfolio Demo
-----------------------

.. note:: Read more about this demo at :doc:`initialmarginagreement`.

To run the demo run:

1. Open the Corda project in IntelliJ and run the "Install" configuration
2. Open the Corda samples project in IntelliJ and run the "Simm Valuation Demo" configuration

Now open http://localhost:10005/web/simmvaluationdemo and http://localhost:10005/web/simmvaluationdemo to view the two nodes that this
will have started respectively. You can now use the demo by creating trades and agreeing the valuations.

Bank Of Corda demo
------------------

This demo brings up three nodes: a notary, a node acting as the Bank of Corda that accepts requests for issuance of some asset
and a node acting as Big Corporation which requests issuance of an asset (cash in this example)
Upon receipt of a request the Bank of Corda node self-issues the asset and then transfers ownership to the requester
after successful notarisation and recording of the issue transaction on the ledger.

To run from the command line (recommended for Mac/UNIX users!):

1. Run ``./gradlew samples:bank-of-corda-demo:deployNodes`` to create a set of configs and installs under ``samples/bank-of-corda-demo/build/nodes``
2. Run ``./samples/bank-of-corda-demo/build/nodes/runnodes`` to open up three new terminal tabs/windows with the three nodes.

Tip!: to verify the Bank of Corda node is alive and running navigate to the following URL
http://localhost:10005/api/bank/date

3. Run ``./gradlew samples:bank-of-corda-demo:runRPCCashIssue`` in another terminal window to trigger a cash issuance request
4. Run ``./gradlew samples:bank-of-corda-demo:runWebCashIssue`` in another terminal window to trigger another cash issuance request
   Now look at the other windows to see the output of the demo.

Or you can run them from inside IntelliJ as follows:

1. Open the Corda project in IntelliJ and run the "Install" configuration
2. Open the Corda samples project in IntelliJ and run the "Bank Of Corda Demo: Run Issuer" configuration
3. Run "Bank Of Corda Demo: Run RPC Cash Issue" - requests issuance of some cash on behalf of Big Corporation via RPC
4. Run "Bank Of Corda Demo: Run Web Cash Issue" - requests issuance of some cash on behalf of Big Corporation via HTTP

In the "Bank Of Corda Demo: Run Issuer" window you should see the following information lines displayed:
>> Awaiting issuance request
>> Self issuing asset
>> Transferring asset to issuance requester
>> Confirming asset issuance to requester

In the the client issue request window you should see the following printed:
>> Successfully processed Cash Issue request

Launch the Explorer application to visualize the issuance and transfer of cash on each node:
./gradlew tools:explorer:run
(for the Bank of Corda node specify localhost, port 10004, user1, test at the login window)
(for the Big Corporation node specify localhost, port 10006, user1, test at the login window)
See https://docs.corda.net/node-explorer.html for further details on usage.
