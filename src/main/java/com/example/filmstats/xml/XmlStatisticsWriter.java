package com.example.filmstats.xml;

import com.example.filmstats.statistics.Statistics;
import com.example.filmstats.statistics.StatisticsEntry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes {@link Statistics} to an XML file in the format:
 * <pre>
 * &lt;statistics&gt;
 *   &lt;item&gt;
 *     &lt;value&gt;Drama&lt;/value&gt;
 *     &lt;count&gt;2&lt;/count&gt;
 *   &lt;/item&gt;
 * &lt;/statistics&gt;
 * </pre>
 */
public final class XmlStatisticsWriter {

    private static final String INDENT = "  ";

    public static String fileNameFor(Statistics statistics) {
        return "statistics_by_" + statistics.getAttribute().getJsonName() + ".xml";
    }

    /** Writes the statistics into {@code directory} and returns the created file path. */
    public Path write(Statistics statistics, Path directory) throws IOException {
        Path target = directory.resolve(fileNameFor(statistics));
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            write(statistics, writer);
        }
        return target;
    }

    public void write(Statistics statistics, Writer writer) throws IOException {
        try {
            XMLStreamWriter xml = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeCharacters("\n");
            xml.writeStartElement("statistics");
            for (StatisticsEntry entry : statistics.getEntries()) {
                xml.writeCharacters("\n" + INDENT);
                xml.writeStartElement("item");
                xml.writeCharacters("\n" + INDENT + INDENT);
                xml.writeStartElement("value");
                xml.writeCharacters(entry.value());
                xml.writeEndElement();
                xml.writeCharacters("\n" + INDENT + INDENT);
                xml.writeStartElement("count");
                xml.writeCharacters(Long.toString(entry.count()));
                xml.writeEndElement();
                xml.writeCharacters("\n" + INDENT);
                xml.writeEndElement();
            }
            xml.writeCharacters("\n");
            xml.writeEndElement();
            xml.writeCharacters("\n");
            xml.writeEndDocument();
            xml.flush();
            xml.close();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to write XML", e);
        }
    }
}
