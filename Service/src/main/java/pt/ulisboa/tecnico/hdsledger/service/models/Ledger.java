package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.hdsledger.utilities.Authenticate;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class Ledger {

    private ArrayList<String> ledgerList = new ArrayList<>();

    private Map<String, Account> accounts = new ConcurrentHashMap<>();

    public Ledger(ProcessConfig[] clientConfigs) {
        // populate the ledger with account from the config file
        for (ProcessConfig config : clientConfigs) {
            Account account = new Account(config.getId(), Authenticate.readPublicKey("../Utilities/keys/" + config.getId() + "Pub.key"));
            accounts.put(config.getId(), account);
        }
    }

    public ArrayList<String> getLedgerList() {
        return ledgerList;
    }

    public void setLedgerList(ArrayList<String> ledgerList) {
        this.ledgerList = ledgerList;
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Map<String, Account> accounts) {
        this.accounts = accounts;
    }

}
