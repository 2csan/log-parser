package com.aci.jd2015;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by NiceOne on 6/2/2016.
 * *******************************
 */
public class Test implements LogParser {

    private int n, k;
    private String[] a;
    private boolean flag = false;
    private String CRC;

    private List<String> allLines = new ArrayList<>();//Все строки
    private List<String> dateLines = new ArrayList<>();//С датой
    private List<String> messageLines = new ArrayList<>();//Остальные строки
    private List<Message> messages = new ArrayList<>();//Результат

    @Override
    public void process(InputStream is, OutputStream os) throws IOException {
        readStream(is);
        while (!allLines.isEmpty()) {
            fillLists(getList(allLines));
            CRC = allLines.get(getCRCIndex(allLines)).substring(4);
            n = messageLines.size();
            flag = false;
            for (int j = 0; j <= n; j++) {// Количество "остальных" строк
                k = j;
                a = new String[j];
                findMes(0, 0);//всевозможне перестановки
                if (flag) {
                    break;
                }
            }
        }
        Collections.sort(messages);
        writeMessages(os);
    }

    private void readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))
        ) {
            String line;
            while ((line = reader.readLine()) != null) { //&& (!line.equals("exit") для теста
                allLines.add(line.replaceAll("[\n]", ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeMessages(OutputStream os) throws IOException {
        try (PrintWriter pw = new PrintWriter(os)) {
            for (Message message : messages) {
                pw.print(message.toString());
            }
        }
    }

    private void findMes(int pos, int maxUsed) {// рекурсивный перебор всех возможных значений
        if (pos == k) {
            for (String data : dateLines) {
                StringBuilder asd = new StringBuilder(data);
                for (String line : a) {
                    asd.append(line);
                }
                if (getHashLine(asd.toString()).equals(CRC)) {//При совпадение хешей
                    messages.add(new Message(asd, CRC));
                    removeFromAllLines(asd);
                }
            }
        } else {
            for (int i = maxUsed + 1; i <= n; i++) {
                if (!flag) {
                    a[pos] = messageLines.get(i - 1);
                    findMes(pos + 1, i);
                }
            }
        }
    }

    private void fillLists(List<String> list) {// Заполняем списки
        dateLines.clear();
        messageLines.clear();
        for (String current : list) {
            if (isDateLine(current)) {
                dateLines.add(current);
            } else
                messageLines.add(current);
        }
    }

    private boolean isDateLine(String line) {//Если строка с датой
        return line.matches("^(0[1-9]|[12][0-9]|3[01])[.](0[1-9]|1[012])[.](19|20)\\d\\d\\s([0-1]\\d|2[0-3])(:[0-5]\\d){2}[.]\\d\\d\\d.*");
    }

    private List<String> getList(List<String> list) {//Возращаем лист до строки CRC_
        return list.subList(0, getCRCIndex(list));
    }

    private int getCRCIndex(List<String> list) {//Сам номер строки с CRC_
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).startsWith("CRC_"))
                return i;
        }
        return 0;
    }

    private String getHashLine(String line) {//Hash
        return DigestUtils.md5Hex(line);
    }

    private void removeFromAllLines(StringBuilder asd) {//
        String temp = asd.toString();
        for (int i = 0; i < getCRCIndex(allLines); i++) {
            if (temp.startsWith(allLines.get(i))) {
                temp = temp.substring(allLines.get(i).length(), temp.length());
                allLines.remove(i);
                i--;
            }
        }
        allLines.remove("CRC_" + CRC);
    }

    private class Message implements Comparable<Message> {
        private Date date;
        private List<String> stringList;

        private Message(StringBuilder line, String CRC) {
            stringList = new ArrayList<>();
            String currentLine = line.toString();
            for (String temp : dateLines) {
                if (currentLine.startsWith(temp)) {
                    try {
                        date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").parse(currentLine.substring(0, 24));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    stringList.add(currentLine.substring(24, temp.length()));
                    currentLine = currentLine.substring(temp.length());
                }
            }
            for (String temp : messageLines) {
                if (currentLine.startsWith(temp)) {
                    stringList.add(temp);
                    currentLine = currentLine.substring(temp.length());
                }
            }
            stringList.add("CRC_" + CRC);
        }

        @Override
        public String toString() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
            StringBuilder stringBuilder = new StringBuilder(dateFormat.format(getDate()) + " ");
            for (String s : stringList) {
                stringBuilder.append(s).append("\n");
            }
//            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf("\n"));
            return stringBuilder.toString();
        }

        private Date getDate() {
            return date;
        }

        @Override
        public int compareTo(Message o) {
            return date.compareTo(o.getDate());
        }
    }
//
//    public static void main(String[] args) throws IOException {
//        Test test = new Test();
//        test.process(System.in, System.out);
//    }
}
