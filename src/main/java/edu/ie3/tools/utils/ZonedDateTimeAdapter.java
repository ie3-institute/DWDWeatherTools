/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Adapter to serialize and deserialize ZoneDateTimes to XML file
 *
 * @author krause, kittl
 * @version 0.2
 * @since 01.09.2017
 */
public class ZonedDateTimeAdapter extends XmlAdapter<String, ZonedDateTime> {
  final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:00");

  @Override
  public String marshal(ZonedDateTime date) {
    return formatter.format(date);
  }

  @Override
  public ZonedDateTime unmarshal(String date) {
    return ZonedDateTime.parse(date, formatter);
  }
}
