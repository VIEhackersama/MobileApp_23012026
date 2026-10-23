package com.example.fruitshop.contracts;

import com.example.fruitshop.dto.FruitInput;
import com.example.fruitshop.models.Fruit;
import java.util.List;

/** Owner: person 2. Synchronous calls must run on the database executor. */
public interface FruitDataSource {
    List<Fruit> list(long actorAccountId, String query);
    Fruit detail(long actorAccountId, long fruitId);
    long create(long actorAccountId, FruitInput input);
    void update(long actorAccountId, long fruitId, FruitInput input);
    void delete(long actorAccountId, long fruitId);
}
