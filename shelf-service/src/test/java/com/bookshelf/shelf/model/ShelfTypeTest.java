package com.bookshelf.shelf.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShelfTypeTest {

    @Test
    void getDefaults_returnsOnlyDefaultShelfTypes() {
        List<ShelfType> defaults = ShelfType.getDefaults();

        assertThat(defaults).containsExactlyInAnyOrder(
                ShelfType.READ,
                ShelfType.CURRENTLY_READING,
                ShelfType.OWNED,
                ShelfType.WISH_LIST
        );
        assertThat(defaults).doesNotContain(ShelfType.CUSTOM);
    }
}
