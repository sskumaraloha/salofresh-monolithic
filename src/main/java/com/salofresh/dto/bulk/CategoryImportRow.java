package com.salofresh.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Plain POJO mirroring the columns of the categories import CSV file.
 * Column order/names: name,type,description,active
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CategoryImportRow {

    private String name;
    private String type;
    private String description;
    private String active;
}
