Consensus and Notaries
======================

Consensus over transaction validity is performed only by parties to the transaction in question. Therefore, data is only
shared with those parties which are required to see it. Other platforms generally reach consensus at the ledger level.
Thus, any given actor in a Corda system sees only a subset of the overall data managed by the system as a whole.
We say a piece of data is “on-ledger” if at least two actors on the system are in consensus as to its existence and
details and we allow arbitrary combinations of actors to participate in the consensus process for any given piece of data.
Data held by only one actor is “off-ledger”.

The following diagram illustrates this model:

.. image:: whitepaper/images/Consensus.png
   :scale: 50 %
   :align: center

Corda has “pluggable” uniqueness services. This is to improve privacy, scalability, legal-system compatibility and
algorithmic agility. A single service may be composed of many mutually untrusting nodes coordinating via a byzantine
fault tolerant algorithm, or could be very simple, like a single machine. In some cases, like when evolving a state
requires the signatures of all relevant parties, there may be no need for a uniqueness service at all.

It is important to note that these uniqueness services are required only to attest as to whether the states consumed by
a given transaction have previously been consumed; they are not required to attest as to the validity of the transaction
itself, which is a matter for the parties to the transaction. This means that the uniqueness services are not required to
(and, in the general case, will not) see the full contents of any transactions, significantly improving privacy and scalability
of the system compared with alternative distributed ledger and blockchain designs.

Consensus is described in detail here :doc:`consensus`

Additionally, section 7 of the `Technical white paper`_ covers this topic in significant more depth.

.. _`Technical white paper`: _static/corda-technical-whitepaper.pdf

