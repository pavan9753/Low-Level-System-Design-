import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        BankService bank = new BankService();
        bank.addAccount(new Account("account-1", 500), "1234");

        CashDispenser dispenser = new CashDispenser(5, 10); // five $50 notes and ten $20 notes
        ATM atm = new ATM(bank, dispenser);
        Card card = new Card("account-1");

        atm.insertCard(card);
        if (!atm.authenticate("1234")) {
            System.out.println("Could not authenticate card.");
            return;
        }

        System.out.println("Balance: $" + atm.checkBalance());
        System.out.println(atm.withdraw(120));
        System.out.println("Balance after withdrawal: $" + atm.checkBalance());
        System.out.println(atm.deposit(50));
        atm.endSession();
    }
}

enum ATMState {
    IDLE,
    CARD_INSERTED,
    AUTHENTICATED
}

class Card {
    private final String accountId;

    Card(String accountId) { this.accountId = accountId; }
    String getAccountId() { return accountId; }
}

class Account {
    private final String id;
    private int balance;

    Account(String id, int startingBalance) {
        this.id = id;
        this.balance = startingBalance;
    }

    String getId() { return id; }
    int getBalance() { return balance; }
    void deposit(int amount) { balance += amount; }
    boolean withdraw(int amount) {
        if (amount > balance) return false;
        balance -= amount;
        return true;
    }
}

class BankService {
    private final Map<String, Account> accounts = new HashMap<String, Account>();
    private final Map<String, String> pins = new HashMap<String, String>();

    void addAccount(Account account, String pin) {
        accounts.put(account.getId(), account);
        pins.put(account.getId(), pin);
    }

    boolean authenticate(Card card, String pin) {
        return accounts.containsKey(card.getAccountId())
                && pin.equals(pins.get(card.getAccountId()));
    }

    int getBalance(String accountId) { return getAccount(accountId).getBalance(); }

    boolean deposit(String accountId, int amount) {
        if (amount <= 0) return false;
        getAccount(accountId).deposit(amount);
        return true;
    }

    boolean withdraw(String accountId, int amount) {
        if (amount <= 0) return false;
        return getAccount(accountId).withdraw(amount);
    }

    private Account getAccount(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) throw new IllegalArgumentException("Unknown account: " + accountId);
        return account;
    }
}

class CashDispenser {
    private final int[] denominations = {50, 20};
    private final int[] noteCounts = new int[2];

    CashDispenser(int fiftyDollarNotes, int twentyDollarNotes) {
        noteCounts[0] = fiftyDollarNotes;
        noteCounts[1] = twentyDollarNotes;
    }

    boolean canDispense(int amount) {
        return amount > 0 && !findNotes(amount).isEmpty();
    }

    boolean dispense(int amount) {
        List<Integer> notes = findNotes(amount);
        if (notes.isEmpty()) return false;
        for (int denomination : notes) {
            for (int i = 0; i < denominations.length; i++) {
                if (denominations[i] == denomination) {
                    noteCounts[i]--;
                    break;
                }
            }
        }
        System.out.println("Dispensing notes: " + notes);
        return true;
    }

    private List<Integer> findNotes(int amount) {
        List<Integer> notes = new ArrayList<Integer>();
        return chooseNotes(0, amount, notes) ? notes : new ArrayList<Integer>();
    }

    private boolean chooseNotes(int index, int remaining, List<Integer> notes) {
        if (remaining == 0) return true;
        if (index == denominations.length || remaining < 0) return false;

        int denomination = denominations[index];
        int maxToUse = Math.min(noteCounts[index], remaining / denomination);
        for (int count = maxToUse; count >= 0; count--) {
            for (int i = 0; i < count; i++) notes.add(denomination);
            if (chooseNotes(index + 1, remaining - count * denomination, notes)) return true;
            for (int i = 0; i < count; i++) notes.remove(notes.size() - 1);
        }
        return false;
    }
}

class ATM {
    private final BankService bank;
    private final CashDispenser dispenser;
    private ATMState state = ATMState.IDLE;
    private Card currentCard;
    private int failedPinAttempts;

    ATM(BankService bank, CashDispenser dispenser) {
        this.bank = bank;
        this.dispenser = dispenser;
    }

    void insertCard(Card card) {
        if (state != ATMState.IDLE) throw new IllegalStateException("ATM is busy.");
        currentCard = card;
        failedPinAttempts = 0;
        state = ATMState.CARD_INSERTED;
    }

    boolean authenticate(String pin) {
        if (state != ATMState.CARD_INSERTED) return false;
        if (!bank.authenticate(currentCard, pin)) {
            failedPinAttempts++;
            if (failedPinAttempts >= 3) endSession();
            return false;
        }
        state = ATMState.AUTHENTICATED;
        return true;
    }

    int checkBalance() {
        requireAuthentication();
        return bank.getBalance(currentCard.getAccountId());
    }

    String deposit(int amount) {
        requireAuthentication();
        return bank.deposit(currentCard.getAccountId(), amount)
                ? "Deposit successful: $" + amount : "Invalid deposit amount.";
    }

    String withdraw(int amount) {
        requireAuthentication();
        if (!dispenser.canDispense(amount)) return "ATM cannot dispense that amount.";
        if (!bank.withdraw(currentCard.getAccountId(), amount)) return "Insufficient account balance.";
        if (!dispenser.dispense(amount)) {
            return "Cash could not be dispensed; bank reversal is required.";
        }
        return "Withdrawal successful: $" + amount;
    }

    void endSession() {
        currentCard = null;
        state = ATMState.IDLE;
        System.out.println("Card returned. Session ended.");
    }

    private void requireAuthentication() {
        if (state != ATMState.AUTHENTICATED) {
            throw new IllegalStateException("Authenticate before using the ATM.");
        }
    }
}
