/*
package com.pays.pos.utils.paxUtils

import org.dom4j.Document
import org.dom4j.DocumentException
import org.dom4j.Element
import org.dom4j.io.SAXReader
import java.io.ByteArrayInputStream

object UIUtil {

    fun findXMl(data: String, node: String): String? {
        val extData = "<root>$data</root>"
        val input: ByteArrayInputStream = ByteArrayInputStream(extData.toByteArray())
        val saxReader = SAXReader()
        try {
            val document: Document = saxReader.read(input)
            val eleRoot: Element = document.getRootElement()
            val iterator: Iterator<*> = eleRoot.elementIterator()
            while (iterator.hasNext()) {
                val ele: Element = iterator.next() as Element
                if (node == ele.getName()) {
                    return ele.getText()
                }
            }
        } catch (e: DocumentException) {
            e.printStackTrace()
        }
        return ""
    }

    fun roundNumber(number: Double): String {
        return String.format("%.2f", number)
    }
}*/
