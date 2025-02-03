package com.traderdeck.backend.models;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.traderdeck.backend.services.BigIntegerConverter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigInteger;

@Entity
@Table(name = "ticker")
public class Ticker {
    @Id
    @CsvBindByName(column = "Symbol")
    private String symbol;
    @CsvBindByName(column = "Name")
    private String name;
    @CsvBindByName(column = "Country")
    private String country;
    @CsvBindByName(column = "Industry")
    private String industry;
    @CsvBindByName(column = "Sector")
    private String sector;
    @CsvCustomBindByName(column = "Market Cap", converter = BigIntegerConverter.class)
    private BigInteger marketCap;

    public Ticker() {}

    public Ticker(String symbol, String name, String country, String industry, String sector, BigInteger marketCap) {
        this.symbol = symbol;
        this.name = name;
        this.country = country;
        this.industry = industry;
        this.sector = sector;
        this.marketCap = marketCap;
    }

    // Getters and Setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public BigInteger getMarketCap() { return marketCap; }
    public void setMarketCap(BigInteger marketCap) { this.marketCap = marketCap; }
}