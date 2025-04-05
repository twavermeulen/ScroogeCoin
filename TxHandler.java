import java.util.ArrayList;

public class TxHandler {

	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */

	public UTXOPool UTXOPool;

	public TxHandler(UTXOPool utxoPool) {
		UTXOPool = new UTXOPool(utxoPool);
	}

	/* Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool, 
	 * (2) the signatures on each input of tx are valid, 
	 * (3) no UTXO is claimed multiple times by tx, 
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of   
	        its output values;
	   and false otherwise.
	 */

	public boolean isValidTx(Transaction tx) {
		double totalInput = 0;
		double totalOutput = 0;

		ArrayList<UTXO> claimedUTXO = new ArrayList<>();

		for (Transaction.Input input : tx.getInputs()) {
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

			// (1)
			if (!UTXOPool.contains(utxo)) {
				return false;
			}
			// (3)
			if (claimedUTXO.contains(utxo)) {
				return false;
			}

			totalInput += UTXOPool.getTxOutput(utxo).value;
			claimedUTXO.add(utxo);

			// (2)
			Transaction.Output output = UTXOPool.getTxOutput(utxo);
			if (!output.address.verifySignature(tx.getRawDataToSign(tx.getInputs().indexOf(input)), input.signature)) {
				return false;
			}
		}

		for (Transaction.Output output : tx.getOutputs()) {

			// (4)
			if (output.value < 0) {
				return false;
			}

			totalOutput += output.value;
		}

		// (5)
        return !(totalInput < totalOutput);
    }

	/* Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness,
	 * returning a mutually valid array of accepted transactions,
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		ArrayList<Transaction> validTxs = new ArrayList<>();

		for (Transaction tx : possibleTxs) {
			if (isValidTx(tx)) {
				validTxs.add(tx);

				for (Transaction.Input input : tx.getInputs()) {
					UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
					UTXOPool.removeUTXO(utxo);
				}
				for (int i = 0; i < tx.getOutputs().size(); i++) {
					Transaction.Output output = tx.getOutput(i);
					UTXO utxo = new UTXO(tx.getHash(), i);
					UTXOPool.addUTXO(utxo, output);
				}
			}
		}

		return validTxs.toArray(Transaction[]::new);
	}
} 
