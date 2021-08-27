package se.nackademin.java20.lab1.domain;

import org.junit.jupiter.api.Test;
import se.nackademin.java20.lab1.domain.Account;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class AccountTest {

    @Test
    void shouldCreateAccountWithZeroBalance() {
        Account account = new Account("Name", 0);
        assertEquals(0, account.getBalance());
    }
}
