package com.truckerload.domain.parser

/** US states, DC and Canadian provinces: the codes accepted after a city name. */
internal object StateCodes {
    val all: Set<String> = (
        "AL AK AZ AR CA CO CT DE DC FL GA HI ID IL IN IA KS KY LA ME MD MA MI MN MS MO MT " +
            "NE NV NH NJ NM NY NC ND OH OK OR PA RI SC SD TN TX UT VT VA WA WV WI WY ON QC BC AB MB NS NB"
        ).split(' ').toSet()
}
