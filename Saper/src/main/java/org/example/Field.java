package org.example;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import java.util.List;

enum Status {
    BOMB,
    FIELD
}

enum Visibility {
    VISIBLE,
    HIDDEN
}

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
public class Field {
    Status status;
    int value;
    Visibility visibility;

    public void evalValue(List<Field> neighbours) {
        int newValue = 0;
        for(Field neighbour : neighbours)
            newValue = neighbour.status == Status.BOMB ? newValue + 1 : newValue;
        this.value = status == Status.BOMB ? -1 : newValue;
    }
    public boolean isVisible() {
        return this.visibility == Visibility.VISIBLE;
    }

    @Override
    public String toString() {
        if(visibility == Visibility.HIDDEN)
            return "*";
        if(status == Status.BOMB)
            return "B";
        return String.valueOf(value);
    }
}
