# Bank of Corda demo
Please see docs/build/html/running-the-demos.html

This program simulates the role of an Asset Issuing authority (eg. Central Bank for Cash) by accepting requests
from third parties to issue some quantity of an Asset and transfer that ownership to the requester.
The Issuing Authority accepts requests via the [IssuerProtocol] protocol flow, self-issues the Asset and transfers
ownership to the Issuer Requester. Notarisation and signing form part of the protocol flow.

The Requesting party can be a CorDapp (running locally or remotely to the Bank of Corda node), a remote RPC client or
a Web Client.

## Prerequisites

You will need to have [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
installed and available on your path.

## Getting Started

1. Launch the Bank of Corda node (and associated Notary) by running:
[BankOfCordaDriver] --role ISSUER
(to validate your Node is running you can try navigating to this sample link: http://localhost:10005/api/bank/date)

2. Run the Bank of Corda Client driver (to simulate a Protocol Flow Issue Requester) by running:
[BankOfCordaDriver] --role ISSUE_CASH_REQUEST_FLOW

3. Run the Bank of Corda Client driver (to simulate a Web Issue Requester) by running:
[BankOfCordaDriver] --role ISSUE_CASH_REQUEST_WEB

4. Run the Bank of Corda Client driver (to simulate an RPC Issue Requester) by running:
[BankOfCordaDriver] --role ISSUE_CASH_REQUEST_RPC

## Developer notes

Testing of the Bank of Corda application is demonstrated at two levels:
1. Unit testing the Protocol Flow uses the [LedgerDSL] and [MockServices]
2. Integration testing via RPC and HTTP uses the [Driver] DSL to launch standalone Node instances

Security
The RPC API requires a client to pass in a User and Permission set:

which are validated against those configured at startup on the Issuer Node:
    User("user1", "test", permissions = setOf(startProtocolPermission<CashProtocol>()))
The Web API is currently unsecure and will be permissioned in a future release

Notary
We are using a [SimpleNotaryService] in this example, but could easily switch to a [ValidatingNotaryService]

## Future

The Bank of Corda node will become an integral part of other Corda Samples that require initial issuance of some asset.

## Further Reading

Tutorials and developer docs for Cordapps and Corda are [here](https://docs.corda.r3cev.com).