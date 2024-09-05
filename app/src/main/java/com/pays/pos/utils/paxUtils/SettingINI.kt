package com.pays.pos.utils.paxUtils

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import com.pax.poslink.CommSetting
import com.pax.poslink.LogSetting
import java.io.*

object SettingINI {
    const val FILENAME = "setting.ini"
    private const val Deft = ""
    private const val SectionComm = "COMMUNICATE"
    private const val TagComm = "CommType"
    private const val TagIp = "IP"
    private const val TagPortnum = "SERIALPORT"
    private const val TagBaudrate = "BAUDRATE"
    private const val TagPort = "PORT"
    private const val TagTimeout = "TIMEOUT_M"
    private const val TagMacAddr = "MACADDR"
    private const val TagDevice = "DEVICE"
    private const val TagLastSN = "LASTSN"
    private const val TagLastTermId = "LASTTERMID"
    private const val SectionLog = "LOG"
    private const val TagMode = "MODE"
    private const val TagLevel = "LEVEL"
    private const val TagOutputPath = "OUTPUTFILE"
    private const val TagHost = "HOST"
    private const val TAG_ENABLE_PROXY = "ENABLE_PROXY"
    fun saveCommSettingToFile(context: Context, fileName: String, commsetting: CommSetting): Boolean {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            val ini: IniFile
            ini = IniFile(fileName)
            ini.section = SectionComm
            var bDone = ini.write(TagComm, commsetting.type)
            bDone = bDone and ini.write(TagTimeout, commsetting.timeOut)
            bDone = bDone and ini.write(TagPortnum, commsetting.serialPort)
            bDone = bDone and ini.write(TagBaudrate, commsetting.baudRate)
            bDone = bDone and ini.write(TagIp, commsetting.destIP)
            bDone = bDone and ini.write(TagPort, commsetting.destPort)
            bDone = bDone and ini.write(TagMacAddr, commsetting.macAddr)
            bDone = bDone and ini.write(TagDevice, commsetting.deviceName)
            /*bDone = bDone and ini.write(
                TagHost,
                Convenience.getHost(MainApplication.getInstance()?.applicationContext, commsetting)
            )*/
            bDone = bDone and ini.write(TAG_ENABLE_PROXY, commsetting.isEnableProxy.toString())
            return bDone
        }else{
            var keyValuePairs= HashMap<String, String>()
            keyValuePairs.put(TagComm,commsetting.type)
            keyValuePairs.put(TagTimeout,commsetting.timeOut)
            keyValuePairs.put(TagPortnum,commsetting.serialPort)
            keyValuePairs.put(TagBaudrate,commsetting.baudRate)
            keyValuePairs.put(TagIp,commsetting.destIP)
            keyValuePairs.put(TagPort,commsetting.destPort)
            keyValuePairs.put(TagMacAddr,commsetting.macAddr)
            keyValuePairs.put(TagDevice,commsetting.deviceName)

            val file = File(context.getExternalFilesDir(null), fileName.substring(fileName.lastIndexOf('/')+1))
            val content = keyValuePairs.entries.joinToString(separator = "\n") { "${it.key}=${it.value}" }

            try {
                file.writeText(content)
                return true
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }
        }
    }

    fun getCommSettingFromFile(context: Context,fileName: String): CommSetting {
        val commsetting = CommSetting()
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q){
            val ini: IniFile
            ini = IniFile(fileName)
            ini.section = SectionComm
            val commsetting = CommSetting()
            commsetting.timeOut = ini.read(TagTimeout, Deft)
            commsetting.type = ini.read(TagComm, Deft)
            commsetting.serialPort = ini.read(TagPortnum, Deft)
            commsetting.baudRate = ini.read(TagBaudrate, Deft)
            commsetting.destIP = ini.read(TagIp, Deft)
            commsetting.destPort = ini.read(TagPort, Deft)
            commsetting.macAddr = ini.read(TagMacAddr, Deft)
            commsetting.deviceName = ini.read(TagDevice, Deft)
            /*Convenience.setHost(
                MainApplication.getInstance()?.applicationContext, commsetting, ini.read(
                    TagHost, Deft
                )
            )*/
            val enableProxy = ini.read(TAG_ENABLE_PROXY, Deft)
            if (!TextUtils.isEmpty(enableProxy)) {
                commsetting.isEnableProxy = java.lang.Boolean.parseBoolean(enableProxy)
            }
            return commsetting
        }
        else{
            val file = File(context.getExternalFilesDir(null), fileName.substring(fileName.lastIndexOf("/")+1))
             if (file.exists()) {
                try {
                    var map=file.readLines().associate {
                        val (key, value) = it.split("=")
                        key to value
                    }
                    commsetting.timeOut = map.get(TagTimeout)
                    commsetting.type = map.get(TagComm)
                    commsetting.serialPort = map.get(TagPortnum)
                    commsetting.baudRate = map.get(TagBaudrate)
                    commsetting.destIP = map.get(TagIp)
                    commsetting.destPort = map.get(TagPort)
                    commsetting.macAddr = map.get(TagMacAddr)
                    commsetting.deviceName = map.get(TagDevice)
                    val enableProxy = map.get(TAG_ENABLE_PROXY)
                    if (!TextUtils.isEmpty(enableProxy)) {
                        commsetting.isEnableProxy = java.lang.Boolean.parseBoolean(enableProxy)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
            return commsetting
        }

    }

    fun saveLastSN(fileName: String, SN: String): Boolean {
        val ini: IniFile
        ini = IniFile(fileName)
        ini.section = SectionComm
        return ini.write(TagLastSN, SN)
    }

    fun getLastSN(fileName: String): String {
        val ini: IniFile
        ini = IniFile(fileName)
        ini.section = SectionComm
        return ini.read(TagLastSN, Deft)
    }

    fun saveLastTermId(fileName: String, termId: String): Boolean {
        val ini: IniFile
        ini = IniFile(fileName)
        ini.section = SectionComm
        return ini.write(TagLastTermId, termId)
    }

    fun getLastTermId(fileName: String): String {
        val ini: IniFile
        ini = IniFile(fileName)
        ini.section = SectionComm
        return ini.read(TagLastTermId, Deft)
    }

    fun saveLogSettingToFile(fileName: String): Boolean {
        val ini: IniFile
        ini = IniFile(fileName)
        ini.section = SectionLog
        var bDone = ini.write(TagMode, java.lang.Boolean.toString(LogSetting.isLoggable()))
        bDone = bDone and ini.write(TagLevel, LogSetting.getLevel().toString())
        bDone = bDone and ini.write(TagOutputPath, LogSetting.getOutputPath())
        return bDone
    }

    fun loadSettingFromFile(fileName: String): Boolean {
        val ini: IniFile
        ini = IniFile(fileName)
        ini.section = SectionLog
        var bDone = false
        var temp = ini.read(TagMode, Deft)
        if (temp.length > 0) {
            bDone = LogSetting.setLogMode(java.lang.Boolean.parseBoolean(temp))
        }
        if (bDone) {
            temp = ini.read(TagLevel, Deft)
            if (temp.length > 0) {
                bDone = LogSetting.setLevel(LogSetting.LOGLEVEL.valueOf(temp))
            }
        }
        if (bDone) {
            temp = ini.read(TagOutputPath, Deft)
            if (temp.length > 0) {
                bDone = LogSetting.setOutputPath(temp)
            }
        }
        return bDone
    }
}

internal class IniFile(val fileName: String) {
    var section: String? = null

    init {
        val fconfig = File(fileName)
        if (fconfig.exists()) {
            //System.out.println("file is exist!");
            try {
                val command = "chmod 666 " + fileName
                val runtime = Runtime.getRuntime()
                runtime.exec(command)
            } catch (e: IOException) {
                println("chmod 666 failed!")
            }
        } else {
            try {
                if (fconfig.createNewFile()) {
                    //System.out.println("create successful!");
                    try {
                        val command = "chmod 666 " + fileName
                        val runtime = Runtime.getRuntime()
                        runtime.exec(command)
                    } catch (e: IOException) {
                        println("chmod 666 failed!")
                    }
                }
            } catch (e: IOException) {
                //e.printStackTrace();
            }
        }
    }

    fun write(key: String, value: String): Boolean {
        return write_profile_string(section, key, value, fileName) == 1
    }

    fun write(key: String, value: Int): Boolean {
        val tmp = StringBuffer(64)
        tmp.delete(0, tmp.capacity())
        tmp.append(value)
        return write(key, tmp.toString())
    }

    fun read(key: String, default_value: String?): String {
        val buf = StringBuffer(4096)
        read_profile_string(
            section, key, buf, buf.capacity(), default_value,
            fileName
        )
        return buf.toString()
    }

    fun read(key: String, default_value: Int): Int {
        return read_profile_int(section, key, default_value, fileName)
    }

    companion object {
        const val MAX_INI_FILE_SIZE = 1024 * 16
        private fun load_ini_file(file: String, buf: StringBuffer, file_size: IntArray): Int {
            var `in`: FileReader? = null
            val fconfig = File(file)
            try{
                if (!fconfig.exists()) {
                    //System.out.println("file is not exist!");
                    return 0
                }
            }finally{
                try {
                    `in`?.close()
                } catch (e: IOException) {
                    //Do nothing
                }
            }

            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q){

                try {


                    `in` = FileReader(file)
                    file_size[0] = 0
                    val data = CharArray(MAX_INI_FILE_SIZE)
                    val num = `in`.read(data)
                    if (num > 0) {
                        val str = String(data, 0, num)
                        buf.delete(0, buf.capacity())
                        buf.append(str)
                        file_size[0] = num
                    }
                    `in`.close()
                    return 1
                } catch (e: IOException) {
                    //Do nothing
                    e.printStackTrace()
                } finally {
                    try {
                        `in`?.close()
                    } catch (e: IOException) {
                        //Do nothing
                    }
                }
            } else{
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), file)
                 if (file.exists()) {
                    var text=file.readText()
                } else {
                    null  // File not found
                }

               /* return try {
                    context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }*/

            }


            return 0
        }

        private fun newline(c: Char): Int {
            return if ('\n' == c || '\r' == c) 1 else 0
        }

        /*//java not supported endchar
    private static int end_of_string(char c)
    {
        return '\0'==c? 1 : 0;
    }
    */
        private fun left_barce(c: Char): Int {
            return if ('[' == c) 1 else 0
        }

        private fun right_brace(c: Char): Int {
            return if (']' == c) 1 else 0
        }

        private fun parse_file(
            section: String?, key: String, buf: String, sec_s: IntArray, sec_e: IntArray,
            key_s: IntArray, key_e: IntArray, value_s: IntArray, value_e: IntArray
        ): Int {
            var i = 0
            value_e[0] = -1
            value_s[0] = value_e[0]
            key_s[0] = value_s[0]
            key_e[0] = key_s[0]
            sec_e[0] = key_e[0]
            sec_s[0] = sec_e[0]
            while (i < buf.length) {
                //find the section
                if ((0 == i || newline(buf[i - 1]) == 1) && left_barce(
                        buf[i]
                    ) == 1
                ) {
                    val section_start = i + 1

                    //find the ']'
                    do {
                        i++
                    } while (right_brace(buf[i]) == 0 && i < buf.length)

                    //System.out.println("section_start  " + section_start);
                    //System.out.println("i-section_start  " + (i-section_start));
                    //System.out.println("write section is   " + section);
                    //System.out.println("file section is   " + p.substring(section_start,i-section_start));
                    //System.out.println("file content is   " + p);
                    if (section == buf.substring(section_start, i)) {
                        var newline_start = 0
                        i++
                        //Skip over space char after ']'
                        while (buf[i] == ' ') {
                            i++
                        }

                        //find the section
                        sec_s[0] = section_start
                        sec_e[0] = i

                        //System.out.println("sec_s[0] is " + sec_s[0]);
                        //System.out.println("sec_e[0] is " + sec_e[0]);
                        while (i < buf.length && (newline(buf[i - 1]) == 0 || left_barce(
                                buf[i]
                            ) == 0)
                        ) {
                            //System.out.println("j char is   " + p.charAt(j));
                            //get a new line
                            newline_start = i
                            while (newline(buf[i]) == 0 && i < buf.length) {
                                i++
                            }
                            //now i  is equal to end of the line
                            var j = newline_start
                            if (';' != buf[j]) //skip over comment
                            {
                                while (j < i && buf[j] != '=') {
                                    //System.out.println("j char is   " + p.charAt(j));
                                    j++
                                    //System.out.println("j+1 char is " + p.charAt(j));
                                    if ('=' == buf[j]) {
                                        //System.out.println("newline_start  " + newline_start);
                                        //System.out.println("j is   " + j);
                                        //System.out.println("key is   " + key);
                                        //System.out.println("file key is   " + p.substring(newline_start,j));
                                        //System.out.println("file content is   " + p);
                                        if (key == buf.substring(newline_start, j)) {
                                            //find the key ok
                                            //System.out.println("not find the key ");
                                            key_s[0] = newline_start
                                            key_e[0] = j - 1
                                            value_s[0] = j + 1
                                            value_e[0] = i
                                            //System.out.println("the key_s is  "+key_s[0]);
                                            return 1
                                        }
                                    }
                                }
                            }
                            i++
                        }
                    }
                } else {
                    i++
                }
            }
            return 0
        }

        fun read_profile_string(
            section: String?, key: String, value: StringBuffer,
            size: Int, default_value: String?, file: String
        ): Int {
            val buf = StringBuffer(MAX_INI_FILE_SIZE)
            val file_size = IntArray(1)
            val sec_s = IntArray(1)
            val sec_e = IntArray(1)
            val key_s = IntArray(1)
            val key_e = IntArray(1)
            val value_s = IntArray(1)
            val value_e = IntArray(1)
            value_e[0] = 0
            value_s[0] = value_e[0]
            key_e[0] = value_s[0]
            key_s[0] = key_e[0]
            sec_e[0] = key_s[0]
            sec_s[0] = sec_e[0]
            file_size[0] = sec_s[0]
            //check parameters
            if (load_ini_file(file, buf, file_size) == 0) {
                if (default_value != null) {
                    value.delete(0, value.length)
                    value.append(default_value)
                }
                return 0
            }
            return if (parse_file(
                    section,
                    key,
                    buf.toString(),
                    sec_s,
                    sec_e,
                    key_s,
                    key_e,
                    value_s,
                    value_e
                ) == 0
            ) {
                if (default_value != null) {
                    value.delete(0, value.length)
                    value.append(default_value)
                }
                0 //not find the key
            } else {
                var cpcount = value_e[0] - value_s[0]
                if (size - 1 < cpcount) {
                    cpcount = size - 1
                }
                value.delete(0, value.length)
                value.append(buf.toString().substring(value_s[0], value_s[0] + cpcount))
                1
            }
        }

        fun read_profile_int(
            section: String?, key: String, default_value: Int,
            file: String
        ): Int {
            val value = StringBuffer(32)
            return if (read_profile_string(
                    section,
                    key,
                    value,
                    value.capacity(),
                    null,
                    file
                ) == 0
            ) {
                default_value
            } else {
                value.toString().toInt()
            }
        }

        /**
         * write a profile string to a ini file
         * @param section [in] name of the section,can't be NULL and empty string
         * @param key [in] name of the key pairs to value, can't be NULL and empty string
         * @param value [in] profile string value
         * @param file [in] path of ini file
         * @return 1 : success\n 0 : failure
         */
        fun write_profile_string(
            section: String?, key: String,
            value: String, file: String
        ): Int {
            val buf = StringBuffer(MAX_INI_FILE_SIZE)
            val w_buf = StringBuffer(MAX_INI_FILE_SIZE)
            val file_size = IntArray(1)
            val sec_s = IntArray(1)
            val sec_e = IntArray(1)
            val key_s = IntArray(1)
            val key_e = IntArray(1)
            val value_s = IntArray(1)
            val value_e = IntArray(1)
            value_e[0] = 0
            value_s[0] = value_e[0]
            key_e[0] = value_s[0]
            key_s[0] = key_e[0]
            sec_e[0] = key_s[0]
            sec_s[0] = sec_e[0]
            file_size[0] = sec_s[0]


            //check parameters
            if (load_ini_file(file, buf, file_size) == 0) {
                sec_s[0] = -1
            } else {
                //System.out.println("file content is "+buf.toString());
                parse_file(
                    section,
                    key,
                    buf.toString(),
                    sec_s,
                    sec_e,
                    key_s,
                    key_e,
                    value_s,
                    value_e
                )
            }
            //System.out.println("sec_s[0] is "+sec_s[0]);
            //System.out.println("key_s[0] is "+key_s[0]);
            if (-1 == sec_s[0]) {
                if (0 == file_size[0]) {
                    //sprintf(w_buf+file_size,"[%s]\n%s=%s\n",section,key,value);
                    w_buf.insert(file_size[0], "[$section]\n$key=$value\n")
                } else {
                    //not find the section, then add the new section at end of the file
                    w_buf.delete(0, w_buf.capacity())
                    w_buf.append(buf.toString().substring(0, file_size[0]))
                    w_buf.insert(file_size[0], "\n[$section]\n$key=$value\n")
                }
            } else if (-1 == key_s[0]) {
                //not find the key, then add the new key=value at end of the section
                w_buf.delete(0, w_buf.capacity())
                w_buf.append(buf.toString().substring(0, sec_e[0] + 1))
                w_buf.append("$key=$value\n")
                w_buf.append(buf.toString().substring(sec_e[0] + 1))
            } else {
                //update value with new value
                w_buf.delete(0, w_buf.capacity())

                //System.out.println("djk buf is "+buf.toString());
                //System.out.println("value_s[0] is "+value_s[0]);
                //System.out.println("djk buf key is "+buf.toString().substring(0, value_s[0]));
                //System.out.println("value_len is "+value_len);
                //System.out.println("value_e[0] is "+value_e[0]);
                w_buf.append(buf.toString().substring(0, value_s[0]))

                //System.out.println("value is "+value);
                w_buf.append(value)
                //System.out.println("file_size[0] is "+file_size[0]);
                if (value_e[0] < file_size[0]) {
                    w_buf.append(buf.toString().substring(value_e[0]))
                }
            }
            var out: FileWriter? = null
            try {
                out = FileWriter(file)

                //System.out.println("write file content is "+w_buf.toString());
                out.write(w_buf.toString())
                out.flush()
                out.close()
                return 1
            } catch (e: Exception) {
                // Do nothing
            } finally {
                try {
                    out?.close()
                } catch (e: IOException) {
                    // Do nothing
                }
            }
            return 0
        }
    }
}