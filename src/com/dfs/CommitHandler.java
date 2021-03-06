package com.dfs;

import java.io.*;
import java.util.Map;

import static com.dfs.Constants.*;

public class CommitHandler implements CommandHandler {

    private int transactionId;
    private int seqNumber;
    private int contentLength;
    private String filename;
    private Transaction transaction;
    private String directory;
    private Map<Integer, Transaction> transactions;

    private String[] command;

    public CommitHandler(String[] command, Transaction transaction, String directory, Map<Integer, Transaction> transactions) {
        this.command = command;
        this.transaction = transaction;
        this.directory = directory;
        this.transactions = transactions;
    }

    public String handleCommand() {
        try {
            if(transaction.transactionConplete()) {
                return ERROR + " " + transactionId + " " + seqNumber + " 201 " + ERROR_201.length() + "\r\n\r\n" +  ERROR_201 + "\n";
            }
            parseCommand();
            Integer write = transaction.checkForMissingWrites();
            if(write != -1) {
                return ACK_RESEND + " " + transactionId + " " + seqNumber + " 0 0" + "\r\n\r\n\r\n";
            }
            writeToDisk();
            removeTransaction(transactions);
            return ACK + " " + transactionId + " " + seqNumber + "\r\n\r\n\r\n";
        } catch (DfsServerException e) {
            return ERROR + " " + transactionId + " " + seqNumber + " " + e.getErrorCode() + " " + e.getMessage().length() + "\r\n\r\n" +  e.getMessage() + "\n";
        }
    }

    private void parseCommand() throws DfsServerException {
        if(command.length < MESSAGE_PARTS) {
            throw new DfsServerException(204, ERROR_204);
        }
        transactionId = Integer.parseInt(command[1]);
        seqNumber = Integer.parseInt(command[2]);
        contentLength = Integer.parseInt(command[3]);
        if(transaction.getCurrentSeqNum() != seqNumber) {
            throw new DfsServerException(204, ERROR_204);
        }
    }

    private void writeToDisk() throws DfsServerException {
        transaction.writeDatatoStream();
        String filepath = directory + transaction.getFileName();
        File file = new File(filepath);
        boolean fileExists = file.exists();
        try {
            if(!fileExists) {
                boolean fileCreated = file.createNewFile();
                if(!fileCreated) {
                    throw new DfsServerException(205, ERROR_205);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }


        ByteArrayOutputStream byteStream = transaction.getByteStream();
        try(FileOutputStream fileStream = new FileOutputStream(filepath, fileExists)) {
            byteStream.writeTo(fileStream);
            fileStream.flush();
            fileStream.getFD().sync();
            transaction.setStatus(TXN_STATE.COMPLETE);
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private void removeTransaction(Map<Integer, Transaction> transactions) {
        transactions.get(transactionId).setStatus(TXN_STATE.COMPLETE);
    }

}
