package com.mycompany.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MessageTest_1 {

    // Temporary folder so the tests never touch the real messages.json
    @TempDir
    Path tempDir;

    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Message message5;

    // Builds the five test messages from the task and gives each its flag
    @BeforeEach
    void setUp() {
        Message.reset();
        Message.jsonFilePath = tempDir.resolve("messages.json").toString();

        message1 = new Message("1000000001", 0, "+27834557896", "Did you get the cake?");
        message2 = new Message("2000000002", 1, "+27838884567", "Where are you? You are late! I have asked you to be on time.");
        message3 = new Message("3000000003", 2, "+27834484567", "Yohoooo, I am at your gate.");
        message4 = new Message("0838884567", 3, "0838884567", "It is dinner time!");
        message5 = new Message("5000000005", 4, "+27838884567", "Ok, I am leaving without you.");

        message1.sentMessage(1);   // Sent
        message2.sentMessage(3);   // Stored
        message3.sentMessage(2);   // Disregard
        message4.sentMessage(1);   // Sent
        message5.sentMessage(3);   // Stored
    }

    private static List<String> texts(List<Message> messages) {
        List<String> result = new ArrayList<>();
        for (Message m : messages) {
            result.add(m.getText());
        }
        return result;
    }

    @Test
    void sentMessagesArrayCorrectlyPopulated() {
        assertEquals(Arrays.asList("Did you get the cake?", "It is dinner time!"),
                texts(Message.getSentMessages()));
    }

    @Test
    void disregardedAndStoredArraysCorrectlyPopulated() {
        assertEquals(Arrays.asList("Yohoooo, I am at your gate."), texts(Message.getDisregardedMessages()));
        assertEquals(Arrays.asList(message2.getText(), message5.getText()), texts(Message.getStoredMessages()));
    }

    @Test
    void messageHashAndIdArraysCorrectlyPopulated() {
        assertEquals(5, Message.getMessageIDs().size());
        assertEquals(5, Message.getMessageHashes().size());
        assertTrue(Message.getMessageIDs().contains("0838884567"));
        assertTrue(Message.getMessageHashes().contains(message2.getMessageHash()));
    }

    @Test
    void displayTheLongestMessage() {
        assertEquals("Where are you? You are late! I have asked you to be on time.",
                Message.findLongestStoredMessage().getText());
    }

    @Test
    void searchForMessageID() {
        assertEquals("It is dinner time!", Message.findByMessageID("0838884567").getText());
    }

    @Test
    void searchForMessageIDThatDoesNotExist() {
        assertNull(Message.findByMessageID("9999999999"));
    }

    @Test
    void searchAllMessagesForARecipient() {
        assertEquals(Arrays.asList("Where are you? You are late! I have asked you to be on time.",
                "Ok, I am leaving without you."),
                texts(Message.findMessagesByRecipient("+27838884567")));
    }

    @Test
    void deleteAMessageUsingTheMessageHash() {
        assertEquals("Message: \"Where are you? You are late! I have asked you to be on time\" successfully deleted.",
                Message.deleteByHash(message2.getMessageHash()));
        assertNull(Message.findByMessageID("2000000002"));
        assertEquals(Arrays.asList(message5.getText()), texts(Message.getStoredMessages()));
    }

    @Test
    void deleteWithAHashThatDoesNotExist() {
        assertEquals("No message found with that hash.", Message.deleteByHash("00:0:NOPE"));
    }

    @Test
    void displayReportShowsHashRecipientAndMessage() {
        String report = Message.printMessages();

        assertTrue(report.contains("Message Hash: " + message1.getMessageHash()));
        assertTrue(report.contains("Recipient: +27834557896"));
        assertTrue(report.contains("Message: Did you get the cake?"));
        assertTrue(report.contains("Message Hash: " + message4.getMessageHash()));
        assertTrue(report.contains("Message: It is dinner time!"));
    }

    @Test
    void storedMessagesAreReadBackFromTheJsonFile() throws IOException {
        Message.reset();   // empty every array, then read the file the setup wrote

        Message.loadStoredMessages();

        assertEquals(Arrays.asList(message2.getText(), message5.getText()), texts(Message.getStoredMessages()));
        assertEquals(message2.getMessageHash(), Message.getStoredMessages().get(0).getMessageHash());
    }
}
