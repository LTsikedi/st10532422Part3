package com.mycompany.main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Login {

    static String storedUser;
    static String storedPassword;

    public static boolean checkUsername(String username) {
        boolean hasUnderScore = false;
        boolean lessThanFive = false;

        if (username.length() < 5) {
            lessThanFive = true;
        }

        if (username.contains("_")) {
            hasUnderScore = true;
        }

        return hasUnderScore && lessThanFive;
    }

    public static boolean checkPasswordComplexity(String password) {
        boolean hasDigit = false;
        boolean hasSpecial = false;
        boolean hasCapital = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasCapital = true;
            }

            if (Character.isDigit(c)) {
                hasDigit = true;
            }

            if (!Character.isLetterOrDigit(c)) {
                hasSpecial = true;
            }
        }

        return hasSpecial && hasDigit && hasCapital;
    }

    public static boolean checkCellPhoneNumber(String number) {
        boolean isValid = false;
        boolean hasCode = false;

        if (number.length() == 12) {
            isValid = true;
        }

        if (number.startsWith("+27")) {
            hasCode = true;
        }

        return hasCode && isValid;
    }

    public static void registerUser() {
        Scanner input = new Scanner(System.in);

        System.out.println("Please Create Username");
        String user = input.nextLine();

        System.out.println("Please Create a Password");
        String password = input.nextLine();

        System.out.println("Please Enter CellPhone Number");
        String number = input.nextLine();

        while (!checkUsername(user)) {
            System.out.println("Username invalid");

            System.out.println("Please Re-create Username");
            user = input.nextLine();
        }

        while (!checkPasswordComplexity(password)) {
            System.out.println("Password invalid");

            System.out.println("Please Re-create a Password");
            password = input.nextLine();
        }

        while (!checkCellPhoneNumber(number)) {
            System.out.println("Cellphone Number invalid");

            System.out.println("Please Enter a Valid CellPhone Number");
            number = input.nextLine();
        }

        storedUser = user;
        storedPassword = password;
        System.out.println("You have been successfully been registered");
    }

    // Now returns true once the user has logged in successfully
    public static boolean loginUser() {
        boolean hasLoggedIn = false;

        System.out.println("Login to your Chat App");
        System.out.println("---------------------------------------------------");
        Scanner input = new Scanner(System.in);

        System.out.println("Please Enter Your Username");
        String user = input.nextLine();

        System.out.println("Please Enter Your Password");
        String enterPass = input.nextLine();

        do {
            if (user.equals(storedUser) && enterPass.equals(storedPassword)) {
                System.out.println("Welcome to Your Chatapp");
                hasLoggedIn = true;
            } else {
                System.out.println("Login unsuccessful");
                System.out.println("Please Enter Your Username");
                user = input.nextLine();
                System.out.println("Please Enter Your Password");
                enterPass = input.nextLine();
            }
        } while (!hasLoggedIn);

        return hasLoggedIn;
    }
}

class Message {

    public static final int MAX_LENGTH = 250;
    public static final String JSON_FILE = "messages.json";

    // Path of the JSON file in use (the unit tests point this at a temporary file)
    static String jsonFilePath = JSON_FILE;

    // The arrays required by Part 3. They are filled while the program runs (nothing is hard-coded)
    private static final List<Message> sentMessages = new ArrayList<>();
    private static final List<Message> disregardedMessages = new ArrayList<>();
    private static final List<Message> storedMessages = new ArrayList<>();   // read from the JSON file
    private static final List<String> messageHashes = new ArrayList<>();
    private static final List<String> messageIDs = new ArrayList<>();

    // Parallel arrays: index i in allMessages, messageIDs and messageHashes is the same message
    private static final List<Message> allMessages = new ArrayList<>();

    private static int numMessagesSent = 0;

    // Pattern used to pull one message out of the JSON file (see loadStoredMessages)
    private static final String JSON_STRING = "\"((?:[^\"\\\\]|\\\\.)*)\"";
    private static final Pattern JSON_ENTRY = Pattern.compile(
            "\"messageID\":\\s*" + JSON_STRING + ",\\s*"
            + "\"messageNumber\":\\s*(\\d+),\\s*"
            + "\"messageHash\":\\s*" + JSON_STRING + ",\\s*"
            + "\"recipient\":\\s*" + JSON_STRING + ",\\s*"
            + "\"message\":\\s*" + JSON_STRING);

    private final String messageID;
    private final int messageNumber;
    private final String recipient;
    private final String text;
    private final String messageHash;

    // Used by the application: the ID is random and the message number
    // is the count of messages sent so far (starts at 0, goes up as messages are sent)
    public Message(String recipient, String text) {
        this(generateMessageID(), numMessagesSent, recipient, text);
    }

    // Lets the unit tests supply a known ID and message number
    Message(String messageID, int messageNumber, String recipient, String text) {
        this.messageID = messageID;
        this.messageNumber = messageNumber;
        this.recipient = recipient;
        this.text = text;
        this.messageHash = createMessageHash();
    }

    // Random ten-digit number (leading zeros allowed, e.g. 0012345678)
    private static String generateMessageID() {
        long id = ThreadLocalRandom.current().nextLong(10_000_000_000L);
        return String.format("%010d", id);
    }

    // Boolean: checkMessageID() - the message ID must be no more than ten characters
    public boolean checkMessageID() {
        return messageID != null && messageID.length() <= 10;
    }

    // String: checkRecipientCell() - checks this message's recipient number
    public String checkRecipientCell() {
        return checkRecipientCell(recipient);
    }

    // Same check for a number that has not been attached to a message yet.
    // Must start with an international code (+) followed by the number;
    // the part after the country code may be no more than ten digits.
    public static String checkRecipientCell(String cell) {
        if (cell != null && cell.matches("\\+\\d{1,3}\\d{1,10}")) {
            return "Cell phone number successfully captured.";
        }
        return "Cell phone number is incorrectly formatted or does not contain an international code. Please correct the number and try again.";
    }

    // Message must not exceed 250 characters
    public static String checkMessageLength(String message) {
        if (message.length() <= MAX_LENGTH) {
            return "Message ready to send.";
        }
        int over = message.length() - MAX_LENGTH;
        return "Message exceeds 250 characters by " + over + "; please reduce the size.";
    }

    // String: createMessageHash() - first two digits of ID : message number : FIRSTLAST (all caps)
    public String createMessageHash() {
        String[] words = text.trim().split("\\s+");
        String firstWord = cleanWord(words[0]);
        String lastWord = cleanWord(words[words.length - 1]);
        String firstTwo = messageID.length() >= 2 ? messageID.substring(0, 2) : messageID;
        return (firstTwo + ":" + messageNumber + ":" + firstWord + lastWord).toUpperCase();
    }

    private static String cleanWord(String word) {
        return word.replaceAll("[^A-Za-z0-9]", "");
    }

    public String messageIDGenerated() {
        return "Message ID generated: " + messageID;
    }

    // String: SentMessage() - the user chooses to send, disregard or store the message
    public String sentMessage(java.util.Scanner input) {
        System.out.println("Please choose an option:");
        System.out.println("1) Send Message");
        System.out.println("2) Disregard Message");
        System.out.println("3) Store Message to send later");

        int choice = 0;
        while (choice < 1 || choice > 3) {
            System.out.print("Enter option (1-3): ");
            try {
                choice = Integer.parseInt(input.nextLine().trim());
            } catch (NumberFormatException e) {
                choice = 0;
            }
        }
        return sentMessage(choice);
    }

    public String sentMessage(int choice) {
        switch (choice) {
            case 1:
                sentMessages.add(this);
                registerMessage(this);
                numMessagesSent++;
                return "Message successfully sent.";
            case 2:
                disregardedMessages.add(this);
                registerMessage(this);
                return "Press 0 to delete the message.";
            case 3:
                return storeMessage();
            default:
                return "Invalid option.";
        }
    }

    // Adds the message's ID and hash to their arrays (index matches allMessages)
    private static void registerMessage(Message message) {
        if (!messageIDs.contains(message.messageID)) {
            allMessages.add(message);
            messageIDs.add(message.messageID);
            messageHashes.add(message.messageHash);
        }
    }

    // Store the message in the JSON file (earlier stored messages are read in first so they are kept)
    public String storeMessage() {
        try {
            loadStoredMessages();
            if (!storedMessages.contains(this)) {
                storedMessages.add(this);
            }
            registerMessage(this);
            writeStoredMessages();
        } catch (IOException e) {
            return "Message could not be stored: " + e.getMessage();
        }
        return "Message successfully stored.";
    }

    private static void writeStoredMessages() throws IOException {
        Files.write(Paths.get(jsonFilePath), toJsonArray(storedMessages).getBytes(StandardCharsets.UTF_8));
    }

    // Reads the JSON file into the stored messages array.
    // Reference: the approach (read the whole file as text, then pull the fields out with
    // java.util.regex) is based on the Java documentation:
    //   Oracle (n.d.) Files.readAllBytes, java.nio.file. Java Platform SE API documentation.
    //   Oracle (n.d.) Pattern and Matcher, java.util.regex. Java Platform SE API documentation.
    // The pattern was written for the JSON layout produced by toJson() below.
    public static void loadStoredMessages() throws IOException {
        Path file = Paths.get(jsonFilePath);
        if (!Files.exists(file)) {
            return;   // nothing has been stored yet
        }

        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        Matcher matcher = JSON_ENTRY.matcher(json);

        while (matcher.find()) {
            String id = unescape(matcher.group(1));
            Message message = findByMessageID(id);

            if (message == null) {
                message = new Message(id, Integer.parseInt(matcher.group(2)),
                        unescape(matcher.group(4)), unescape(matcher.group(5)));
                registerMessage(message);
            }
            if (!storedMessages.contains(message)) {
                storedMessages.add(message);
            }
        }
    }

    // Reverses escape(): turns \n, \", \\ etc. in the file back into normal characters
    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                i++;
                char next = value.charAt(i);
                if (next == 'n') {
                    result.append('\n');
                } else if (next == 'r') {
                    result.append('\r');
                } else if (next == 't') {
                    result.append('\t');
                } else {
                    result.append(next);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String toJsonArray(List<Message> messages) {
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < messages.size(); i++) {
            json.append(messages.get(i).toJson());
            if (i < messages.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        return json.append("]\n").toString();
    }

    private String toJson() {
        return "  {\n"
                + "    \"messageID\": \"" + escape(messageID) + "\",\n"
                + "    \"messageNumber\": " + messageNumber + ",\n"
                + "    \"messageHash\": \"" + escape(messageHash) + "\",\n"
                + "    \"recipient\": \"" + escape(recipient) + "\",\n"
                + "    \"message\": \"" + escape(text) + "\"\n"
                + "  }";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Message ID, Message Hash, Recipient, Message - in that order
    public String getDetails() {
        return "Message ID: " + messageID + "\n"
                + "Message Hash: " + messageHash + "\n"
                + "Recipient: " + recipient + "\n"
                + "Message: " + text + "\n";
    }

    // String: printMessages() - report of every message sent while the program is running
    public static String printMessages() {
        return buildReport(sentMessages);
    }

    // Report with the full details of every stored message
    public static String printStoredMessages() {
        return buildReport(storedMessages);
    }

    private static String buildReport(List<Message> messages) {
        StringBuilder report = new StringBuilder();
        for (Message m : messages) {
            report.append(m.getDetails()).append("\n");
        }
        return report.toString();
    }

    // Search the parallel arrays for a message ID (null if there is no such message)
    public static Message findByMessageID(String id) {
        int index = messageIDs.indexOf(id);
        return index >= 0 ? allMessages.get(index) : null;
    }

    // Longest message in the stored messages array (null if nothing is stored)
    public static Message findLongestStoredMessage() {
        Message longest = null;
        for (Message m : storedMessages) {
            if (longest == null || m.text.length() > longest.text.length()) {
                longest = m;
            }
        }
        return longest;
    }

    // All messages sent or stored for one recipient
    public static List<Message> findMessagesByRecipient(String recipient) {
        List<Message> matches = new ArrayList<>();
        for (Message m : sentMessages) {
            if (m.recipient.equals(recipient)) {
                matches.add(m);
            }
        }
        for (Message m : storedMessages) {
            if (m.recipient.equals(recipient)) {
                matches.add(m);
            }
        }
        return matches;
    }

    // Deletes a message from every array using its message hash
    public static String deleteByHash(String hash) {
        int index = messageHashes.indexOf(hash);
        if (index < 0) {
            return "No message found with that hash.";
        }

        Message message = allMessages.get(index);
        boolean wasStored = storedMessages.contains(message);

        allMessages.remove(index);
        messageIDs.remove(index);
        messageHashes.remove(index);
        sentMessages.remove(message);
        disregardedMessages.remove(message);
        storedMessages.remove(message);

        if (wasStored) {
            try {
                writeStoredMessages();   // keep the JSON file in step with the array
            } catch (IOException e) {
                return "Message deleted, but the file could not be updated: " + e.getMessage();
            }
        }

        // The required output has no full stop after the message text
        String shown = message.text.endsWith(".")
                ? message.text.substring(0, message.text.length() - 1) : message.text;
        return "Message: \"" + shown + "\" successfully deleted.";
    }

    // Int: returnTotalMessages() - total number of messages sent
    public static int returnTotalMessages() {
        return sentMessages.size();
    }

    // Clears the shared state (used by the unit tests)
    static void reset() {
        sentMessages.clear();
        disregardedMessages.clear();
        storedMessages.clear();
        messageHashes.clear();
        messageIDs.clear();
        allMessages.clear();
        numMessagesSent = 0;
    }

    // Read-only views of the arrays (used by the menu and the unit tests)
    public static List<Message> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    public static List<Message> getDisregardedMessages() {
        return Collections.unmodifiableList(disregardedMessages);
    }

    public static List<Message> getStoredMessages() {
        return Collections.unmodifiableList(storedMessages);
    }

    public static List<String> getMessageHashes() {
        return Collections.unmodifiableList(messageHashes);
    }

    public static List<String> getMessageIDs() {
        return Collections.unmodifiableList(messageIDs);
    }

    public String getMessageID() {
        return messageID;
    }

    public String getMessageHash() {
        return messageHash;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getText() {
        return text;
    }

    public int getMessageNumber() {
        return messageNumber;
    }
}

public class Main {

    // How many messages the user said they want to enter, and how many they have entered so far
    static int totalToEnter;
    static int entered;

    public static void main(String[] args) {

        Login.registerUser();

        // Messages can only be sent once the user has logged in successfully
        if (Login.loginUser()) {
            runQuickChat();
        }
    }

    static void runQuickChat() {
        Scanner input = new Scanner(System.in);

        System.out.println("Welcome to QuickChat.");

        totalToEnter = readInt(input, "How many messages do you wish to enter? ", 1, Integer.MAX_VALUE);
        entered = 0;

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("Please choose an option:");
            System.out.println("1) Send Messages");
            System.out.println("2) Show recently sent messages");
            System.out.println("3) Quit");
            System.out.println("4) Stored Messages");

            int choice = readInt(input, "Enter option (1-4): ", 1, 4);

            switch (choice) {
                case 1:
                    sendMessages(input);
                    break;
                case 2:
                    System.out.println("Coming Soon.");
                    break;
                case 3:
                    System.out.println("Goodbye.");
                    running = false;
                    break;
                case 4:
                    storedMessagesMenu(input);
                    break;
            }
        }
    }

    // Fourth main menu option: works with the stored messages array (read from the JSON file)
    static void storedMessagesMenu(Scanner input) {
        try {
            Message.loadStoredMessages();
        } catch (IOException e) {
            System.out.println("The stored messages could not be read: " + e.getMessage());
            return;
        }

        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("Stored Messages - please choose an option:");
            System.out.println("a) Display the sender and recipient of all stored messages");
            System.out.println("b) Display the longest stored message");
            System.out.println("c) Search for a message ID");
            System.out.println("d) Search for all messages sent or stored for a recipient");
            System.out.println("e) Delete a message using its message hash");
            System.out.println("f) Display a report of all stored messages");
            System.out.println("g) Back to the main menu");

            System.out.print("Enter option (a-g): ");
            String choice = input.nextLine().trim().toLowerCase();

            switch (choice) {
                case "a":
                    displaySenderAndRecipient();
                    break;
                case "b":
                    displayLongestMessage();
                    break;
                case "c":
                    searchByMessageID(input);
                    break;
                case "d":
                    searchByRecipient(input);
                    break;
                case "e":
                    deleteByHash(input);
                    break;
                case "f":
                    displayReport();
                    break;
                case "g":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid input, please try again.");
            }
        }
    }

    // a) sender (the logged-in user) and recipient of every stored message
    static void displaySenderAndRecipient() {
        if (noStoredMessages()) {
            return;
        }
        for (Message m : Message.getStoredMessages()) {
            System.out.println("Sender: " + Login.storedUser + " | Recipient: " + m.getRecipient());
        }
    }

    // b) longest stored message
    static void displayLongestMessage() {
        Message longest = Message.findLongestStoredMessage();
        if (longest == null) {
            System.out.println("There are no stored messages.");
            return;
        }
        System.out.println("Longest stored message:");
        System.out.print(longest.getDetails());
    }

    // c) search by message ID and show the recipient and message
    static void searchByMessageID(Scanner input) {
        System.out.print("Enter the message ID: ");
        Message found = Message.findByMessageID(input.nextLine().trim());

        if (found == null) {
            System.out.println("No message found with that ID.");
            return;
        }
        System.out.println("Recipient: " + found.getRecipient());
        System.out.println("Message: " + found.getText());
    }

    // d) every message sent or stored for one recipient
    static void searchByRecipient(Scanner input) {
        System.out.print("Enter the recipient's cell number: ");
        String recipient = input.nextLine().trim();

        List<Message> matches = Message.findMessagesByRecipient(recipient);
        if (matches.isEmpty()) {
            System.out.println("No messages found for that recipient.");
            return;
        }
        for (Message m : matches) {
            System.out.println("Message: " + m.getText());
        }
    }

    // e) delete a message using its hash
    static void deleteByHash(Scanner input) {
        System.out.print("Enter the message hash: ");
        System.out.println(Message.deleteByHash(input.nextLine().trim().toUpperCase()));
    }

    // f) full details of every stored message
    static void displayReport() {
        if (noStoredMessages()) {
            return;
        }
        System.out.println("Stored Messages Report");
        System.out.println("---------------------------------------------------");
        System.out.print(Message.printStoredMessages());
    }

    static boolean noStoredMessages() {
        if (Message.getStoredMessages().isEmpty()) {
            System.out.println("There are no stored messages.");
            return true;
        }
        return false;
    }

    static void sendMessages(Scanner input) {
        if (entered >= totalToEnter) {
            System.out.println("You have already entered all " + totalToEnter + " of your messages.");
            return;
        }

        while (entered < totalToEnter) {
            System.out.println();
            System.out.println("Message " + (entered + 1) + " of " + totalToEnter);

            String recipient = readRecipient(input);
            String body = readMessageBody(input);

            Message message = new Message(recipient, body);
            String result = message.sentMessage(input);
            System.out.println(result);

            if (result.equals("Message successfully sent.")) {
                System.out.println();
                System.out.print(message.getDetails());
            } else if (result.equals("Press 0 to delete the message.")) {
                String answer = "";
                while (!answer.equals("0")) {
                    answer = input.nextLine().trim();
                }
                System.out.println("Message deleted.");
            }

            entered++;
        }

        System.out.println();
        System.out.println("Total number of messages sent: " + Message.returnTotalMessages());
    }

    static String readRecipient(Scanner input) {
        while (true) {
            System.out.print("Enter the recipient's cell number (with international code, e.g. +27718693002): ");
            String cell = input.nextLine().trim();
            String result = Message.checkRecipientCell(cell);
            System.out.println(result);
            if (result.equals("Cell phone number successfully captured.")) {
                return cell;
            }
        }
    }

    static String readMessageBody(Scanner input) {
        while (true) {
            System.out.print("Enter your message: ");
            String body = input.nextLine();

            if (body.trim().isEmpty()) {
                System.out.println("A message cannot be empty.");
            } else if (body.length() > Message.MAX_LENGTH) {
                System.out.println("Please enter a message of less than 250 characters.");
            } else {
                System.out.println("Message sent");
                return body;
            }
        }
    }

    static int readInt(Scanner input, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(input.nextLine().trim());
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through and ask again
            }
            System.out.println("Invalid input, please try again.");
        }
    }
}
