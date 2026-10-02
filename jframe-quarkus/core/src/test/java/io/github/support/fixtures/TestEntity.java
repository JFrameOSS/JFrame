package io.github.support.fixtures;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Simple test entity with an id and a name. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestEntity {

    private Long id;

    private String name;
}
