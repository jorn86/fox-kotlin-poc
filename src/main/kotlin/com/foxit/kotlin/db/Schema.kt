package com.foxit.kotlin.db

fun DatabaseService.createSchema() {
    connection {
        statement {
            execute("CREATE TABLE column (" +
                    "id IDENTITY PRIMARY KEY, " +
                    "name TEXT NOT NULL " +
                    ")")
            execute("CREATE TABLE task (" +
                    "id IDENTITY PRIMARY KEY, " +
                    "column_id INT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "description TEXT, " +
                    "created TIMESTAMP NOT NULL DEFAULT NOW(), " +
                    "FOREIGN KEY (column_id) REFERENCES column(id) " +
                    ")")
        }
    }
}
