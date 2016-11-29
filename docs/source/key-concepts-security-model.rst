Security Model
==============

Corda has been designed from the ground up to implement a global, decentralised database where all nodes are assumed to be
untrustworthy. These weak trust assumptions require that each node actively cross-check's each other's work to reach consensus
amongst a group of interacting participants.

Security is prevalent at the following levels:

* Notarisation: pluggable notaries and algorithms offering different levels of trust.
  Notaries may be validating or non-validating. A validating notary will resolve and fully check transactions they are asked to deconflict.
  Without the use of any other privacy features, they gain full visibility into every transaction.
  On the other hand. non-validating notaries assume transaction validity and do not request transaction data or their dependencies
  beyond the list of states consumed (and thus, their level of trust is much lower and exposed to malicious use of transaction inputs)
  From an algorithm perspective Corda currently supports non-BFT algorithms (uniqueness and timestamping, RAFT)

.. note:: future notary algorithms may include BFT and security hardware assisted non-BFT algorithms (where non-BFT algorithms
    are converted into a more trusted form using remote attestation and hardware protection).

* Composite keys

* Authentication

.. warning:: API level authentication (RPC, Web) is currently simple username/password and subject to design review.

* Authorisation

.. warning:: API level authorisation (RPC, Web) is currently based on permission groups applied to flow execution.
    This is subject to design review with views to selecting a proven, mature entitlements solution.

* Encryption

* Integrity

* Non-repudiation

* Privacy techniques include the following:
    ** Partial data visibility: transactions are not globally broadcast as in many other systems.
    ** Transaction tear-offs: Transactions are structured as Merkle trees, and may have individual subcomponents be revealed
       to parties who already know the Merkle root hash. Additionally, they may sign the transaction without being able to see all of it.

.. note:: future privacy techniques will include key randomisation, graph pruning, deterministic JVM sandboxing and support fo secure signing devices.
    See the Technical White Paper for detailed descriptions of these techniques and features.


