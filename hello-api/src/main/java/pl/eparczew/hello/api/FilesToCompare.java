package pl.eparczew.hello.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.Value;

import java.io.File;

@Value
@JsonDeserialize
public final class FilesToCompare {

    public final String originalfile;
    public final String comparefile;

    @JsonCreator
    public FilesToCompare(String originalFile, String compareFile) {
        this.originalfile = Preconditions.checkNotNull(originalFile, "originalFile");
        this.comparefile = Preconditions.checkNotNull(compareFile, "compareFile");
    }
}
