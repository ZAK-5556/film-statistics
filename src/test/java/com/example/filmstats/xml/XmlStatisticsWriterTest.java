package com.example.filmstats.xml;

import com.example.filmstats.statistics.Attribute;
import com.example.filmstats.statistics.Statistics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlStatisticsWriterTest {

    private final XmlStatisticsWriter writer = new XmlStatisticsWriter();

    @Test
    void shouldWriteItemsSortedByCount() throws IOException {
        // given
        Statistics statistics = Statistics.of(Attribute.GENRES, Map.of("Drama", 2L, "Romance", 5L, "Crime", 1L));
        StringWriter out = new StringWriter();

        // when
        writer.write(statistics, out);

        // then
        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <statistics>
                  <item>
                    <value>Romance</value>
                    <count>5</count>
                  </item>
                  <item>
                    <value>Drama</value>
                    <count>2</count>
                  </item>
                  <item>
                    <value>Crime</value>
                    <count>1</count>
                  </item>
                </statistics>
                """;
        assertEquals(expected, out.toString());
    }

    @Test
    void shouldWriteEmptyStatistics() throws IOException {
        // given
        Statistics statistics = Statistics.of(Attribute.YEAR, Map.of());
        StringWriter out = new StringWriter();

        // when
        writer.write(statistics, out);

        // then
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<statistics>\n</statistics>\n", out.toString());
    }

    @Test
    void shouldEscapeSpecialCharacters() throws IOException {
        // given
        Statistics statistics = Statistics.of(Attribute.DIRECTOR, Map.of("Tom & Jerry <Co>", 1L));
        StringWriter out = new StringWriter();

        // when
        writer.write(statistics, out);

        // then
        String xml = out.toString();
        assertTrue(xml.contains("Tom &amp; Jerry &lt;Co"), xml);
    }

    @Test
    void shouldNameFileAfterAttribute() throws IOException {
        // given
        Path dir = Files.createTempDirectory("xml");
        Statistics statistics = Statistics.of(Attribute.YEAR, Map.of("2010", 1L));

        // when
        Path file = writer.write(statistics, dir);

        // then
        assertEquals("statistics_by_year.xml", file.getFileName().toString());
        assertTrue(Files.readString(file).contains("<value>2010</value>"));
    }
}
