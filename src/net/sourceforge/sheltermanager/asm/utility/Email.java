/*
   Animal Shelter Manager
   Copyright(c)2000-2011, R. Rawson-Tetley

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation; either version 2 of
   the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTIBILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the
   Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston
   MA 02111-1307, USA.

   Contact me by electronic mail: bobintetley@users.sourceforge.net
*/
package net.sourceforge.sheltermanager.asm.utility;

import net.sourceforge.sheltermanager.asm.bo.*;
import net.sourceforge.sheltermanager.asm.globals.*;
import net.sourceforge.sheltermanager.asm.ui.internet.*;
import net.sourceforge.sheltermanager.asm.ui.owner.OwnerEdit;
import net.sourceforge.sheltermanager.asm.ui.ui.Dialog;
import net.sourceforge.sheltermanager.asm.ui.ui.UI;

import java.io.*;

import java.net.*;

import java.text.*;

import java.util.*;


public class Email {
    static final int DEFAULT_PORT = 25;
    static final String EOL = "\r\n"; // network end of line
    protected DataInputStream reply = null;
    protected PrintStream send = null;
    protected Socket sock = null;

    public Email() throws UnknownHostException, IOException {
        this(getSMTPServer());
    }

    /**
     *   Create an object pointing to the specified host
     *   @param hostid The host to connect to.
     *   @exception UnknownHostException
     *   @exception IOException
     */
    public Email(String hostid) throws UnknownHostException, IOException {
        this(hostid, DEFAULT_PORT);
    }

    public Email(String hostid, int port)
        throws UnknownHostException, IOException {
        sock = new Socket(hostid, port);
        reply = new DataInputStream(sock.getInputStream());
        send = new PrintStream(sock.getOutputStream());

        String rstr = readLine(reply);

        if (!rstr.startsWith("220")) {
            throw new ProtocolException(rstr);
        }

        while (rstr.indexOf('-') == 3) {
            rstr = readLine(reply);

            if (!rstr.startsWith("220")) {
                throw new ProtocolException(rstr);
            }
        }
    }

    public Email(InetAddress address) throws IOException {
        this(address, DEFAULT_PORT);
    }

    public Email(InetAddress address, int port) throws IOException {
        sock = new Socket(address, port);
        reply = new DataInputStream(sock.getInputStream());
        send = new PrintStream(sock.getOutputStream());

        String rstr = readLine(reply);

        if (!rstr.startsWith("220")) {
            throw new ProtocolException(rstr);
        }

        while (rstr.indexOf('-') == 3) {
            rstr = readLine(reply);

            if (!rstr.startsWith("220")) {
                throw new ProtocolException(rstr);
            }
        }
    }

    public void sendmsg(String to_address, String subject, String message)
        throws IOException, ProtocolException {
        sendmsg(to_address, subject, message, getLocalEmail(), "text/plain");
    }

    public void sendmsg(String to_address, String subject, String message,
        String from_address, String content_type)
        throws IOException, ProtocolException {
        sendmsg(to_address, "", subject, message, from_address, content_type);
    }

    public void sendmsg(String to_address, String cc_address, String subject,
        String message, String from_address, String content_type)
        throws IOException, ProtocolException {
        String rstr;
        String sstr;

        InetAddress local;

        try {
            local = InetAddress.getLocalHost();
        } catch (UnknownHostException ioe) {
            Global.logError("No local IP address found - is your network up?",
                "Email.sendmsg");
            throw ioe;
        }

        String host = local.getHostName();
        send.print("HELO " + host);
        send.print(EOL);
        send.flush();
        rstr = readLine(reply);

        if (!rstr.startsWith("250")) {
            throw new ProtocolException(rstr);
        }

        // Extract the email address portion (if it doesn't have markers, 
        // assume the whole thing is email)
        String from = from_address;

        if (from.indexOf("<") != -1) {
            from = from.substring(from.indexOf("<") + 1, from.indexOf(">"));
        }

        sstr = "MAIL FROM: <" + from + ">";
        send.print(sstr);
        send.print(EOL);
        send.flush();
        rstr = readLine(reply);

        if (!rstr.startsWith("250")) {
            throw new ProtocolException(rstr);
        }

        // to_address can be a comma separated list with multiple addresses,
        // make a list of them. If we have a cc_address, same applies. Issue
        // a separate RCPT TO for each to/cc address.
        String[] to = Utils.split(to_address, ",");
        String[] cc = Utils.split(cc_address, ",");

        for (int i = 0; i < to.length; i++) {
            sstr = "RCPT TO: <" + to[i].trim() + ">";
            send.print(sstr);
            send.print(EOL);
            send.flush();
            rstr = readLine(reply);

            if (!rstr.startsWith("250")) {
                throw new ProtocolException(rstr);
            }
        }

        if (cc_address.length() > 0) {
            for (int i = 0; i < cc.length; i++) {
                sstr = "RCPT TO: <" + cc[i].trim() + ">";
                send.print(sstr);
                send.print(EOL);
                send.flush();
                rstr = readLine(reply);

                if (!rstr.startsWith("250")) {
                    throw new ProtocolException(rstr);
                }
            }
        }

        send.print("DATA");
        send.print(EOL);
        send.flush();
        rstr = readLine(reply);

        if (!rstr.startsWith("354")) {
            throw new ProtocolException(rstr);
        }

        send.print("From: " + from_address);
        send.print(EOL);
        send.print("To: " + to_address);
        send.print(EOL);

        if (cc_address.length() > 0) {
            send.print("Cc: " + cc_address);
            send.print(EOL);
        }

        send.print("Content-Type: " + content_type);
        send.print(EOL);
        send.print("Subject: " + subject);
        send.print(EOL);

        // Create Date - we'll cheat by assuming that local clock is right
        Calendar today_date = Calendar.getInstance();
        send.print("Date: " + msgDateFormat(today_date));
        send.print(EOL);
        send.flush();

        send.print("X-Mailer: " + Global.productName + " " +
            Global.productVersion);
        send.print(EOL);

        // Sending a blank line ends the header part.
        send.print(EOL);

        // Now send the message proper
        send.print(message);
        send.print(EOL);
        send.print(".");
        send.print(EOL);
        send.flush();

        rstr = readLine(reply);

        if (!rstr.startsWith("250")) {
            throw new ProtocolException(rstr);
        }
    }

    public static boolean isSetup() {
        boolean ok = ((!getLocalEmail().equals("")) &&
            (!getSMTPServer().equals("")));

        if (!ok) {
            Dialog.showError(Global.i18n("uiinternet",
                    "You_need_to_setup_your_email"));
        }

        return ok;
    }

    public static String getLocalEmail() {
        return Configuration.getString("EmailAddress");
    }

    public static String getSMTPServer() {
        return Configuration.getString("SMTPServer");
    }

    /**
     * Opens an email form with the to address,
     * and a blank subject/body
     */
    public static void singleEmailForm(String to_email) {
        UI.cursorToWait();

        EmailForm emf = new EmailForm();
        emf.removeFields();
        emf.setTo(to_email);
        Global.mainForm.addChild(emf);
    }

    /**
     * Opens an email form with the to address and an owner link,
     * and a blank subject/body
     */
    public static void singleEmailForm(String to_email, OwnerEdit parent) {
        UI.cursorToWait();

        EmailForm emf = new EmailForm();
        emf.removeFields();
        emf.setTo(to_email);
        emf.setOwnerID(parent.getOwnerID());
        emf.setParentOwnerForm(parent);
        Global.mainForm.addChild(emf);
    }

    /**
     * Opens an email form with no to address,
     * and a blank subject/body
     */
    public static void multiEmailForm(EmailFormListener parent,
        Vector<String> fieldlist) {
        UI.cursorToWait();

        EmailForm emf = new EmailForm();
        emf.setBulkEmail();
        emf.setParent(parent);
        emf.addFields(fieldlist);
        Global.mainForm.addChild(emf);
        fieldlist = null;
    }

    public void close() {
        try {
            send.print("QUIT");
            send.print(EOL);
            send.flush();
            sock.close();
        } catch (IOException ioe) {
            // As though there's anything I can do about it now...
        }
    }

    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    private String msgDateFormat(Calendar senddate) {
        SimpleDateFormat sdf = new SimpleDateFormat("E dd/MM/yyyy HH:mm:ss Z");

        try {
            return sdf.format(senddate.getTime());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Replacement for deprecated DataInputStream.readLine()
     */
    private String readLine(DataInputStream reply) throws IOException {
        byte[] buff = new byte[1024];
        int bytesread = reply.read(buff);

        if (bytesread == -1) {
            return "";
        }

        return new String(buff).substring(0, bytesread);
    }
}
