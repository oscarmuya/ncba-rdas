package com.loopdfs.rdas.client;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.loopdfs.rdas.config.RdasProperties;
import com.loopdfs.rdas.exception.SoapCountryClientException;
import com.loopdfs.rdas.model.internal.ContinentRecord;
import com.loopdfs.rdas.model.internal.CountryRecord;
import com.loopdfs.rdas.model.internal.CurrencyRecord;
import com.loopdfs.rdas.model.internal.LanguageRecord;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
class CountryInfoSoapClient implements SoapCountryClient {

  private final WebServiceTemplate webServiceTemplate;
  private final RdasProperties properties;
  private final MeterRegistry meterRegistry;
  private final Counter soapErrors;

  CountryInfoSoapClient(WebServiceTemplate webServiceTemplate, RdasProperties properties, MeterRegistry meterRegistry) {
    this.webServiceTemplate = webServiceTemplate;
    this.properties = properties;
    this.meterRegistry = meterRegistry;
    this.soapErrors = Counter.builder("rdas.soap.errors").description("SOAP CountryInfo call failures")
        .register(meterRegistry);
  }

  @Override
  public CountryReferenceData fetchReferenceData() {
    try {
      List<ContinentRecord> continents = call("ListOfContinentsByName",
          () -> parseCodeNameList(invoke("ListOfContinentsByName"), "tContinent", "sCode"));
      List<CurrencyRecord> currencies = call("ListOfCurrenciesByName",
          () -> parseCodeNameList(invoke("ListOfCurrenciesByName"), "tCurrency", "sISOCode").stream()
              .map(item -> new CurrencyRecord(item.code(), item.name()))
              .toList());
      List<LanguageRecord> languages = call("ListOfLanguagesByName",
          () -> parseCodeNameList(invoke("ListOfLanguagesByName"), "tLanguage", "sISOCode").stream()
              .map(item -> new LanguageRecord(item.code(), item.name()))
              .toList());

      Map<String, ContinentRecord> continentsByCode = indexContinents(continents);
      Map<String, CurrencyRecord> currenciesByCode = indexCurrencies(currencies);
      Map<String, LanguageRecord> languagesByCode = indexLanguages(languages);
      List<CountryRecord> countries = call("FullCountryInfoAllCountries",
          () -> parseCountries(invoke("FullCountryInfoAllCountries"), continentsByCode, currenciesByCode,
              languagesByCode));

      return new CountryReferenceData(countries, continents, currencies, languages);
    } catch (SoapCountryClientException ex) {
      throw ex;
    } catch (Exception ex) {
      soapErrors.increment();
      throw new SoapCountryClientException("Unable to load country reference data from SOAP service", ex);
    }
  }

  private <T> T call(String operation, Supplier<T> supplier) {
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      return supplier.get();
    } catch (RuntimeException ex) {
      soapErrors.increment();
      throw ex;
    } finally {
      sample.stop(Timer.builder("rdas.soap.latency").tag("operation", operation).register(meterRegistry));
    }
  }

  private String invoke(String operation) {
    StringWriter writer = new StringWriter();
    String payload = "<" + operation + " xmlns=\"" + properties.soap().namespaceUri() + "\"/>";
    try {
      webServiceTemplate.sendSourceAndReceiveToResult(properties.soap().endpoint(),
          new StreamSource(new StringReader(payload)),
          new StreamResult(writer));
      return writer.toString();
    } catch (Exception ex) {
      throw new SoapCountryClientException("SOAP CountryInfo operation failed: " + operation, ex);
    }
  }

  private List<ContinentRecord> parseCodeNameList(String xml, String itemName, String codeField) {
    List<ContinentRecord> values = new ArrayList<>();
    for (Element item : elementsByLocalName(parse(xml), itemName)) {
      String code = normalizeCode(childText(item, codeField));
      String name = childText(item, "sName");
      if (!code.isBlank() && !name.isBlank()) {
        values.add(new ContinentRecord(code, name));
      }
    }
    return distinctContinents(values);
  }

  private List<CountryRecord> parseCountries(String xml, Map<String, ContinentRecord> continentsByCode,
      Map<String, CurrencyRecord> currenciesByCode, Map<String, LanguageRecord> languagesByCode) {
    List<CountryRecord> countries = new ArrayList<>();
    for (Element item : elementsByLocalName(parse(xml), "tCountryInfo")) {
      String isoCode = normalizeCode(childText(item, "sISOCode"));
      String name = childText(item, "sName");
      if (isoCode.isBlank() || name.isBlank()) {
        continue;
      }

      String continentCode = normalizeCode(childText(item, "sContinentCode"));
      String currencyCode = normalizeCode(childText(item, "sCurrencyISOCode"));
      ContinentRecord continent = continentsByCode.getOrDefault(continentCode, new ContinentRecord(continentCode, ""));
      CurrencyRecord currency = currenciesByCode.getOrDefault(currencyCode, new CurrencyRecord(currencyCode, ""));
      List<LanguageRecord> languages = parseCountryLanguages(item, languagesByCode);

      countries.add(new CountryRecord(isoCode, name, childText(item, "sCapitalCity"), childText(item, "sCountryFlag"),
          childText(item, "sPhoneCode"), continent, currency, languages));
    }
    return countries;
  }

  private List<LanguageRecord> parseCountryLanguages(Element country, Map<String, LanguageRecord> languagesByCode) {
    List<LanguageRecord> languages = new ArrayList<>();
    for (Element language : directDescendantsByLocalName(country, "tLanguage")) {
      String code = normalizeCode(childText(language, "sISOCode"));
      String name = childText(language, "sName");
      if (!code.isBlank()) {
        languages.add(languagesByCode.getOrDefault(code, new LanguageRecord(code, name)));
      }
    }
    return distinctLanguages(languages);
  }

  private Document parse(String xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(xml)));
    } catch (Exception ex) {
      throw new SoapCountryClientException("Unable to parse SOAP CountryInfo response", ex);
    }
  }

  private List<Element> elementsByLocalName(Document document, String localName) {
    NodeList nodes = document.getElementsByTagNameNS("*", localName);
    List<Element> elements = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node instanceof Element element) {
        elements.add(element);
      }
    }
    return elements;
  }

  private List<Element> directDescendantsByLocalName(Element root, String localName) {
    NodeList nodes = root.getElementsByTagNameNS("*", localName);
    List<Element> elements = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node instanceof Element element) {
        elements.add(element);
      }
    }
    return elements;
  }

  private String childText(Element element, String localName) {
    NodeList nodes = element.getElementsByTagNameNS("*", localName);
    if (nodes.getLength() == 0) {
      return "";
    }
    return nodes.item(0).getTextContent().trim();
  }

  private List<ContinentRecord> distinctContinents(List<ContinentRecord> values) {
    return new ArrayList<>(values.stream()
        .collect(LinkedHashMap<String, ContinentRecord>::new, (map, item) -> map.putIfAbsent(item.code(), item),
            LinkedHashMap::putAll)
        .values());
  }

  private List<LanguageRecord> distinctLanguages(List<LanguageRecord> values) {
    return new ArrayList<>(values.stream()
        .collect(LinkedHashMap<String, LanguageRecord>::new, (map, item) -> map.putIfAbsent(item.code(), item),
            LinkedHashMap::putAll)
        .values());
  }

  private Map<String, ContinentRecord> indexContinents(List<ContinentRecord> values) {
    return values.stream().collect(LinkedHashMap::new, (map, item) -> map.put(item.code(), item),
        LinkedHashMap::putAll);
  }

  private Map<String, CurrencyRecord> indexCurrencies(List<CurrencyRecord> values) {
    return values.stream().collect(LinkedHashMap::new, (map, item) -> map.put(item.code(), item),
        LinkedHashMap::putAll);
  }

  private Map<String, LanguageRecord> indexLanguages(List<LanguageRecord> values) {
    return values.stream().collect(LinkedHashMap::new, (map, item) -> map.put(item.code(), item),
        LinkedHashMap::putAll);
  }

  private String normalizeCode(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }
}
