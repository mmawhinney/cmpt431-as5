package com.dfs;


import static com.dfs.Constants.*;

public class NewTxnHandler implements CommandHandler {

    private int transactionId;
    private int seqNumber;
    private int contentLength;

    private byte[] data;
    private String[] command;
    private Transaction transaction;

    public NewTxnHandler(String[] command, byte[] data, Transaction transaction) {
        this.command = command;
        this.data = data;
        this.transaction = transaction;
    }


    public String handleCommand() {
        try {
            parseCommand();
            parseFileName();
            return ACK + " " + transactionId + " " + seqNumber + "\r\n\r\n\r\n";
        } catch (DfsServerException e) {
            return ERROR + " " + transactionId + " " + seqNumber + " " + e.getErrorCode() + " " + e.getMessage().length() + "\r\n\r\n" + e.getMessage() + "\n";
        }
    }

    private void parseCommand() throws DfsServerException {
        if(command.length < MESSAGE_PARTS) {
            throw new DfsServerException(204, ERROR_204);
        }
        transactionId = transaction.getId();
        seqNumber = Integer.parseInt(command[2]);
        contentLength = Integer.parseInt(command[3]);
        if(seqNumber != 0) {
            throw new DfsServerException(ERROR_202);
        }
    }

    private void parseFileName() {
        byte[] filenameData = new byte[data.length];
        int j = 0;
        for (byte b : data) {
            if (b > 32 && b < 127) {
                filenameData[j] = b;
                j++;
            }
        }
        String filename = new String(filenameData, 0, j);
        transaction.setFileName(filename);
    }

}
