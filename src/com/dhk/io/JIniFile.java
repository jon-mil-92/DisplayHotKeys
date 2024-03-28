package com.dhk.io;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <b><p>Description:</b><br>
 * JIniFile stores and retrieves application-specific information and settings from INI files.
 * <br><br>
 * JIniFile enables handling the storage and retrieval of application-specific information
 * and settings in a standard INI file. The INI file text format is a standard introduced
 * in Windows 3.x for storing and retrieving application settings from session to session.
 * An INI file stores information in logical groupings, called "sections." For example,
 * the WIN.INI file contains a section called "[Desktop]". Within each section, actual data
 * values are stored in named keys. Keys take the form:
 * <br><br>
 * &lt;keyname&gt;=&lt;value&gt;<br>
 * A FileName is passed to the JIniFile constructor and identifies the INI file that the object accesses.</p>
 *
 * @author Andreas Norman
 * @version 1.0.1
 */
public class JIniFile extends ArrayList<String> {
	private static final long serialVersionUID = 1L;
	private final File userFileName;

    /**
     * Constructor JIniFile
     * <p>
     * Creates a JIniFile object for an application. Create assigns the fileName parameter
     * to the fileName property, which is used to specify the name of the INI file to use.
     * <p>
     * <b>Note:</b> By default the INI files are stored in the Application directory. To work with an INI file in another
     * location, specify the full path name of the file in fileName.
     */
    public JIniFile(String fileName) throws IOException {
        clear();
        File userFileName = new File(fileName);
        this.userFileName = userFileName;
        if (userFileName.exists()) {
            try (BufferedReader inbuf = new BufferedReader(new FileReader(userFileName))) {
                while (true) {
                    String s = inbuf.readLine();
                    if (s == null) {
                        break;
                    }
                    add(s);
                }
            }
        }
    }

    /**
     * Call ReadSections to retrieve the names of all sections in the INI file into an ArrayList object.
     *
     * @return an ArrayList holding the sections
     */
    public List<String> readSections() {
        List<String> list = new ArrayList<>();
        String s;
        for (int i = 0; i < size(); i++) {
            s = get(i);
            if (s.startsWith("[") && s.endsWith("]")) {
                list.add(s.substring(1, (s.length() - 1)));
            }
        }
        return list;
    }

    /**
     * Call UpdateFile to flush buffered reads from and writes to the INI file to disk.
     *
     * @throws IOException if not successful
     */
    public void updateFile() throws IOException {
        try (BufferedWriter outbuf = new BufferedWriter(new FileWriter(userFileName, false))) {
            int i = 0;
            while (i < size()) {
                String s = get(i);
                if (s == null) {
                    break;
                }
                outbuf.write(s);
                outbuf.newLine();
                i++;
            }
        }
    }


    /**
     * Call ReadString to read a string value from an INI file. section identifies the
     * section in the file that contains the desired key. key is the name of the key
     * from which to retrieve the value. defaultValue is the string value to return if the:<br>
     * - section does not exist.<br>
     * - Key does not exist.<br>
     * - Data value for the key is not assigned.<br>
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue default value if no value is found
     * @return returns the value. If empty or nonexistent it will return the default value
     */
    public String readString(String section, String key, String defaultValue) {
        String value = defaultValue;
        if (valuePosition(section, key) > 0) {
            int strLen = key.length() + 1;
            value = get(valuePosition(section, key)).substring(strLen);
        }
        return value;
    }

    /**
     * Call ReadSectionValues to read the keys, and the values from all keys, within a specified
     * section of an INI file into an ArrayList object. section identifies the section in the file
     * from which to read key values.
     *
     * @param section the section name
     * @return an ArrayList holding the values in the specified section.
     */
    public List<String> readSectionValues(String section) {
        List<String> myList = new ArrayList<>();
        int start = sectionPosition(section) + 1;
        String s;
        if (sectionPosition(section) > -1) {
            for (int i = start; i < size(); i++) {
                s = get(i).substring(get(i).indexOf("=") + 1);
                if (s.startsWith("[") && s.endsWith("]")) {
                    break;
                } else {
                    myList.add(s);
                }
            }
        }
        return myList;
    }

    /**
     * Call ReadInteger to read a string value from an INI file. Section identifies the
     * section in the file that contains the desired key. key is the name of the key
     * from which to retrieve the value. defaultValue is the integer value to return if the:<br>
     * - section does not exist.<br>
     * - Key does not exist.<br>
     * - Data value for the key is not assigned.<br>
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue default value if no value is found
     * @return returns the value. If empty or nonexistent it will return the default value
     */
    public int readInteger(String section, String key, int defaultValue) {
        int value = defaultValue;
        if (valuePosition(section, key) > 0) {
            int strLen = key.length() + 1;
            value = Integer.parseInt(get(valuePosition(section, key)).substring(strLen));
        }
        return value;
    }

    /**
     * Call ReadFloat to read a string value from an INI file. section identifies the
     * section in the file that contains the desired key. key is the name of the key
     * from which to retrieve the value. defaultValue is the float value to return if the:<br>
     * - section does not exist.<br>
     * - Key does not exist.<br>
     * - Data value for the key is not assigned.<br>
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue default value if no value is found
     * @return returns the value. If empty or nonexistent it will return the default value
     */
    public Float readFloat(String section, String key, Float defaultValue) {
        Float value;
        value = defaultValue;
        if (valuePosition(section, key) > 0) {
            int strLen = key.length() + 1;
            value = Float.valueOf(get(valuePosition(section, key)).substring(strLen));
        }
        return value;
    }

    /**
     * Call ReadBool to read a string value from an INI file. Section identifies the
     * section in the file that contains the desired key. key is the name of the key
     * from which to retrieve the value. defaultValue is the boolean value to return if the:<br>
     * - section does not exist.<br>
     * - Key does not exist.<br>
     * - Data value for the key is not assigned.<br>
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue default value if no value is found
     * @return returns the value. If empty or nonexistent it will return the default value
     */
    public boolean readBool(String section, String key, boolean defaultValue) {
        boolean value = defaultValue;
        if (valuePosition(section, key) > 0) {
            int strLen = key.length() + 1;
            value = Boolean.valueOf(get(valuePosition(section, key)).substring(strLen));
        }
        return value;
    }

    /**
     * SectionExists is used internally to determine the position of a section within the INI file
     * specified in FileName.
     * <p>
     * Section is the INI file section SectionExists determines the position of.
     * <p>
     * SectionExists returns an Integer value that indicates the position of the section in question.
     *
     * @param section the section name
     * @return will return the position of the section. Returns -1 not found.
     */
    private int sectionPosition(String section) {
        String s;
        int pos = -1;
        for (int i = 0; i < size(); i++) {
            s = get(i);
            if (s.equals("[" + section + "]")) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    /**
     * Use SectionExists to determine whether a section exists within the INI file specified in FileName.
     * <p>
     * Section is the INI file section SectionExists determines the existence of.
     * <p>
     * SectionExists returns a Boolean value that indicates whether the section in question exists.
     *
     * @param section the section name
     * @return returns <code>true</code> if the section exists.
     */
    public boolean sectionExist(String section) {
        boolean val = false;
        for (String value : this) {
            if (value.equals("[" + section + "]")) {
                val = true;
                break;
            }
        }
        return val;
    }

    /**
     * Call EraseSection to remove a section, all its keys, and their data values from
     * an INI file. Section identifies the INI file section to remove.
     *
     * @param section the section name
     */
    public void eraseSection(String section) {
        String s;
        int start = sectionPosition(section) + 1;
        if (sectionPosition(section) > -1) {
            for (int i = start; i < size(); i++) {
                s = get(i);
                if (s.startsWith("[") && s.endsWith("]")) {
                    break;
                } else {
                    remove(i);
                    i--;
                }
            }
            remove(sectionPosition(section));
        }
    }

    /**
     * Call ReadSection to retrieve the names of all keys within a specified section of an
     * INI file into an ArrayList object.
     *
     * @param section the section name
     * @return a List holding all key names.
     */
    public List<String> readSection(String section) {
        String s;
        List<String> myList = new ArrayList<>();
        int start = sectionPosition(section) + 1;
        if (sectionPosition(section) > -1) {
            for (int i = start; i < size(); i++) {
                s = get(i);
                if (s.startsWith("[") && s.endsWith("]")) {
                    break;
                } else {
                    myList.add(s.substring(0, s.indexOf("=")));
                }
            }
        }
        return myList;
    }

    /**
     * Use ValueExist to determine whether a key exists in the INI file specified in FileName.<br>
     * section is the section in the INI file in which to search for the key.<br>
     * key is the name of the key to search for.<br>
     * ValueExist returns a Boolean value that indicates whether the key exists in the specified section.<br>
     *
     * @param section the section name
     * @param key     the key name
     * @return returns <code>true</code> if value exists
     */
    public boolean valueExist(String section, String key) {
        int start = sectionPosition(section);
        String s;
        boolean val = false;
        for (int i = start + 1; i < size(); i++) {
            s = get(i);
            if (s.startsWith(key + "=")) {
                val = true;
                break;
            } else if (s.startsWith("[") && s.endsWith("]")) {
                break;
            }
        }
        return val;
    }

    /**
     * ValuePosition is used internally to determine the position of a in the INI file specified in FileName.
     * <p>
     * section is the section in the INI file in which to search for the key.
     * <p>
     * key is the name of the key to search for.
     * <p>
     * ValuePosition returns an Integer value that indicates the position of the key in the INI file.
     *
     * @param section the section name
     * @param key     the key name
     * @return returns the position of the Value. If not found it will return -1
     */
    private int valuePosition(String section, String key) {
        int start = sectionPosition(section);
        String s;
        int pos = -1;
        for (int i = start + 1; i < size(); i++) {
            s = get(i);
            if (s.startsWith(key + "=")) {
                pos = i;
                break;
            } else if (s.startsWith("[") && s.endsWith("]")) {
                break;
            }
        }
        return pos;
    }

    /**
     * Call DeleteKey to erase an INI file entry. section is string containing the name
     * of an INI file section, and key is a string containing the name of the key from
     * for which to set a nil value.
     * <p>
     * <b>Note:</b> Attempting to delete a key in a nonexistent section or attempting to delete a
     * nonexistent key are not errors. In these cases, DeleteKey does nothing.
     *
     * @param section the section name
     * @param key     the key name
     */
    public void deleteKey(String section, String key) {
        if (valuePosition(section, key) > 0) {
            remove(valuePosition(section, key));
        }
    }

    /**
     * Call WriteString to write a string value to an INI file. section identifies the section in the
     * file that contain the key to which to write. key is the name of the key for which to set a value.
     * value is the string value to write.
     * <p>
     * <b>Note:</b> Attempting to write a data value to a nonexistent section or attempting to write data to a nonexistent
     * key are not errors. In these cases, WriteString creates the section and key and sets its initial value
     * to value.
     *
     * @param section the section name
     * @param key     the key name
     * @param value   the value
     */
    public void writeString(String section, String key, String value) {
        String s = key + "=" + value;
        addToList(section, key, s);
    }

    /**
     * Call WriteBool to write a boolean value to an INI file. section identifies the section in the
     * file that contain the key to which to write. key is the name of the key for which to set a value.
     * value is the boolean value to write.
     * <p>
     * <b>Note:</b> Attempting to write a data value to a nonexistent section or attempting to write data to a nonexistent
     * key are not errors. In these cases, WriteBool creates the section and key and sets its initial value
     * to value.
     *
     * @param section the section name
     * @param key     the key name
     * @param value   the value
     */
    public void writeBool(String section, String key, boolean value) {
        String s = key + "=" + value;
        addToList(section, key, s);
    }

    /**
     * Call WriteFloat to write a float value to an INI file. section identifies the section in the
     * file that contain the key to which to write. key is the name of the key for which to set a value.
     * value is the float value to write.
     * <p>
     * <b>Note:</b> Attempting to write a data value to a nonexistent section or attempting to write data to a nonexistent
     * key are not errors. In these cases, WriteFloat creates the section and key and sets its initial value
     * to value.
     *
     * @param section the section name
     * @param key     the key name
     * @param value   the value
     */
    public void writeFloat(String section, String key, float value) {
        String s = key + "=" + value;
        addToList(section, key, s);
    }

    /**
     * Call WriteInteger to write an integer value to an INI file. section identifies the section in the
     * file that contain the key to which to write. key is the name of the key for which to set a value.
     * value is the integer value to write.
     * <p>
     * <b>Note:</b> Attempting to write a data value to a nonexistent section or attempting to write data to a nonexistent
     * key are not errors. In these cases, WriteInteger creates the section and key and sets its initial value
     * to value.
     *
     * @param section the section name
     * @param key     the key name
     * @param value   the value
     */
    public void writeInteger(String section, String key, int value) {
        String s = key + "=" + value;
        addToList(section, key, s);
    }


    /**
     * addToList is used internally to add values to the INI file specified in FileName.
     * <p>
     * section is the section in the INI file in which to search for the key.
     * <p>
     * key is the name of the key to search for.
     * <p>
     * value is the name of the value to write
     * <p>
     * <b>Note:</b> Attempting to write a data value to a nonexistent section or attempting to write data to a nonexistent
     * key are not errors. In these cases, addToList creates the section and key and sets its initial value
     * to value.
     *
     * @param section the section name
     * @param key     the key name
     * @param value   the value
     */
    private void addToList(String section, String key, String value) {
        if (sectionExist((section))) {
            if (valueExist(section, key)) {
                int pos = valuePosition(section, key);
                remove(pos);
                add(pos, value);
            } else {
                add((sectionPosition(section) + 1), value);
            }
        } else {
            add("[" + section + "]");
            add(value);
        }
    }
}
