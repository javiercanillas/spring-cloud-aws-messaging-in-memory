package io.github.javiercanillas.amazonws.services.sqs;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
@EqualsAndHashCode
@Getter
public class Pair<S,T> {
    private final S first;
    private final T second;
}
